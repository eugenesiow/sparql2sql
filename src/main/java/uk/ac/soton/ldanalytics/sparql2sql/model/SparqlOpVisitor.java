package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.OpAssign;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpDatasetNames;
import org.apache.jena.sparql.algebra.op.OpDiff;
import org.apache.jena.sparql.algebra.op.OpDisjunction;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpExt;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLabel;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpList;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpNull;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpProcedure;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpPropFunc;
import org.apache.jena.sparql.algebra.op.OpQuad;
import org.apache.jena.sparql.algebra.op.OpQuadBlock;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.algebra.op.OpReduced;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.op.OpTopN;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.main.StageGenerator;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprWalker;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

public class SparqlOpVisitor implements OpVisitor {
	
	RdfTableMapping mapping = null;
	List<List<Result>> bgpBindings = new ArrayList<List<Result>>();
	List<Map<String,String>> varMappings = new ArrayList<Map<String,String>>();
//	Map<String,String> varMapping = new HashMap<String,String>();
	Map<String,String> aliases = new HashMap<String,String>();
	Set<String> tableList = new HashSet<String>();
	List<String> previousSelects = new ArrayList<String>();
	Set<String> unionSelects = new HashSet<String>();
	List<List<String>> filterList = new ArrayList<List<String>>();
	List<String> unionList = new ArrayList<String>();
	List<Boolean> hasResults = new ArrayList<Boolean>();
	Map<String,String> uriToSyntax = new HashMap<String,String>();	
	Map<String,String> tableToSyntax = new HashMap<String,String>();
	List<Set<String>> groupLists = new ArrayList<Set<String>>();
	List<Set<String>> havingLists = new ArrayList<Set<String>>();
	int globalVarMappingCount = -1;
	
	String selectClause = "SELECT ";
	String fromClause = "FROM ";
	String whereClause = "WHERE ";
	String groupClause = "GROUP BY ";	
	String havingClause = "HAVING ";
	
	String dialect = "H2";
	Boolean bgpStarted = false;
	Boolean unionStarted = false;
	
	/**
	 * trans() function that walks/visits nodes in the algebra tree 
	 */
	public SparqlOpVisitor() {
		StageGenerator origStageGen = (StageGenerator)ARQ.getContext().get(ARQ.stageGenerator) ;
        StageGenerator stageGenAlt = new StageGeneratorAlt(origStageGen) ;
        ARQ.getContext().set(ARQ.stageGenerator, stageGenAlt) ;
	}
	
	/**
	 * Assigns a mapping closure for the algebra to be executed against
	 * @param mapping a mapping closure 
	 */
	public void useMapping(RdfTableMapping mapping) {
		this.mapping = mapping;
	}

	
	/**
	 * BGP Resolution visitor
	 */
	public void visit(OpBGP bgp) {
		globalVarMappingCount++;
		bgpStarted = true;
		
		List<Triple> patterns = bgp.getPattern().getList();	
		
		String queryStr = "SELECT * WHERE {\n";
		for(Triple pattern:patterns) {
			queryStr += "\t"+nodeToString(pattern.getSubject())+" "+nodeToString(pattern.getPredicate())+" "+nodeToString(pattern.getObject())+".\n"; 
		}
		queryStr += "}";
		
//			System.out.println(queryStr);
		
				
//		StageGenerator origStageGen = (StageGenerator)qe.getContext().get(ARQ.stageGenerator) ;
//        StageGenerator stageGenAlt = new StageGeneratorAlt(origStageGen) ;
//        qe.getContext().set(ARQ.stageGenerator, stageGenAlt) ;
		
		hasResults.add(ProcessResults(mapping.executeQuery(queryStr,dialect))); //check if there are results for the BGP

//		ResultSetFormatter.out(System.out, results, query);
//		while(results.hasNext()) {
//			Binding b = results.nextBinding();
//			System.out.println(b);
//		}
	}
	
	/**
	 * Processes the ResultSet from the BGP resolution engine to produce a VarMapping (vk,vv) mapping
	 * @param results ResultSet from the BGP resolution engine
	 * @return Boolean on whether there are any results
	 */
	private Boolean ProcessResults(ResultSet results) {
		Boolean hasResults = false;
		List<Result> bindingSet = new ArrayList<Result>();
		int size = 0;
		for(Result result:results.getResults()) {
//			System.out.println(result);
			hasResults = true;
			bindingSet.add(result);
			AddVarMappings(result);
			size++;
		}
		if(size>1) unionStarted = true;
		bgpBindings.add(bindingSet);
//		for(Result r:bindingSet) {
//			System.out.println(r.getVarMapping());
//		}
		return hasResults;
	}
	
	/**
	 * Adds a Result object to the varMapping list
	 * @param rs
	 */
	private void AddVarMappings(Result rs) {
		Map<String,String> varMapping = new HashMap<String,String> ();
		varMapping.putAll(rs.getVarMapping());
		varMappings.add(varMapping);
		//TODO: might need separate tablelists for different resultsets
		tableList.addAll(rs.getTableList());
		if(!whereClause.trim().equals("WHERE"))
			whereClause += " AND ";
		whereClause += rs.getWhere();
	}

	/**
	 * Helper function to convert a Jena Node object to a string
	 * @param node Jena node object
	 * @return string of the nodes value
	 */
	private String nodeToString(Node node) {
		if(node.isURI()) {
			return "<"+node.toString()+">";
		} else if(node.isLiteral()) {
			String literal = "\""+node.getLiteralValue()+"\"";
			if(node.getLiteralDatatypeURI()!=null) {
				literal += "^^<"+node.getLiteralDatatypeURI()+">";
			}
			return literal;
		} else {
			String nodeStr = node.toString();
			if(nodeStr.startsWith("??")) {
				nodeStr = nodeStr.replace("??", "?bn");
			}
			return nodeStr;
		}
	}
	

	public void visit(OpQuadPattern arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpQuadBlock arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpTriple arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpQuad arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpPath arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpTable arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpNull arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpProcedure arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpPropFunc arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpFilter filters) {
//		System.out.println(allSelectedNodes.get(allSelectedNodes.size()-1).size());
//		System.out.println("Filter");	
		
		List<String> filterStrs = new ArrayList<String>();
		for(Map<String,String> varMapping:varMappings) {
			Set<String> havingList = new HashSet<String>();
			String filterStr = "";
			for(Expr filter:filters.getExprs().getList()) {
				SparqlFilterExprVisitor v = new SparqlFilterExprVisitor();
				v.setMapping(varMapping);
				ExprWalker.walk(v,filter);
				v.finishVisit();
				String modifier = "";
				if(!v.getExpression().equals("")) {
	//				if(!whereClause.equals("WHERE ")) {
	//					modifier = " AND ";
	//				}
					if(!filterStr.equals("")) {
						modifier = " AND ";
					}
					filterStr += modifier + v.getExpression();
	//				whereClause += modifier + v.getExpression();
				} else if(!v.getHavingExpression().equals("")) {
//					if(!havingClause.equals("HAVING ")) {
//						modifier = " AND ";
//					}
//					havingClause += modifier + v.getHavingExpression();
					havingList.add(v.getHavingExpression());
				}
			}
//			System.out.println(havingList.size() + varMapping.toString());
			if(havingList.size()>0)
				havingLists.add(havingList);
			filterStrs.add(filterStr);
		}
		filterList.add(filterStrs);
		
		
//		System.out.println(whereClause);
	}

	public void visit(OpGraph arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpService arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpDatasetNames arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpLabel arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpAssign arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpExtend arg0) {
//		System.out.println("extend");
		VarExprList vars = arg0.getVarExprList();
		for(Var var:vars.getVars()) {
			Expr expr = vars.getExpr(var);
			String originalKey = expr.getVarName();
			if(expr.isFunction()) {
				for(int i=globalVarMappingCount;i<varMappings.size();i++) {
					Map<String,String> varMapping = varMappings.get(i);
					SparqlExtendExprVisitor v = new SparqlExtendExprVisitor();
					v.setMapping(varMapping);
					ExprWalker.walk(v,expr);
					v.finishVisit();
					aliases.put(var.getName(), v.getExpression());
				}
			}
			if(aliases.containsKey(originalKey)) {
				String val = aliases.remove(originalKey);
				aliases.put(var.getName(), val);
			} 
			for(int i=globalVarMappingCount;i<varMappings.size();i++) {
				Map<String,String> varMapping = varMappings.get(i);
				if(varMapping.containsKey(originalKey)) {
					// do not remove but just get for the case where there is extend and filter: e.g.
					//				(project (?roomName ?totalMotion)
					//						  (filter (> ?.0 0)
					//						    (extend ((?totalMotion ?.0))
					//						      (extend ((?roomName ?sensor))
					//						        (group (?sensor) ((?.0 (sum ?motionOrNoMotion)))
					String val = varMapping.get(originalKey); 
					varMapping.put(var.getName(), val);
				}
			}
		}
	}

	public void visit(OpJoin arg0) {
		// TODO fix join
		filterList.clear();
	}

	public void visit(OpLeftJoin arg0) {
		if(hasResults.size()>1) {
			for(Result right:bgpBindings.get(1)) {
				for(Result left:bgpBindings.get(0)) {
					Boolean discardRow = false;
//					System.out.println(right.getVarMapping() +" "+ left.getVarMapping());
					for(String v:right.getVarMapping().keySet()) {
						if(left.getVarMapping().containsKey(v)) {
							if(!left.getVarMapping().get(v).equals(right.getVarMapping().get(v))) {
//								System.out.println(left.getVarMapping().get(v) + " " +right.getVarMapping().get(v));
								discardRow = true;
								break;
							}
						}
					}
					
					if(!discardRow) {
						for(Map<String,String> varMapping:varMappings) {
							varMapping.putAll(right.getVarMapping());
						}
					}
					
					RemoveVarMappings(right);
				}
			}
		}
	}

	/**
	 * Removes the indicated Result object from the list of varMappings
	 * @param mapping
	 */
	private void RemoveVarMappings(Result mapping) {
		for (Iterator<Map<String, String>> iterator = varMappings.iterator(); iterator.hasNext();) {
			Map<String, String> element = iterator.next();
		    if (element.equals(mapping.getVarMapping())) {
		        iterator.remove();
		    }
		}
		
	}

	public void visit(OpUnion arg0) {
		int index = hasResults.size()-1;
		int whereIndex = filterList.size()-1;
		if(unionList.isEmpty()) {
			if(hasResults.get(index-1)) {
				String unionStr = "SELECT * FROM ";
				for(String tables:tableList)
					unionStr += tables + " ";
				unionStr += " WHERE "+filterList.get(whereIndex-1);
				unionList.add(unionStr);
			}
		}
		if(hasResults.get(index)) {
			List<String> filterStrs = filterList.get(whereIndex);
			int count = 0;
			String unionStr = "";
			for(String filterStr:filterStrs) {
				if(count++>0) unionStr += " UNION ";
				unionStr += "SELECT * FROM ";
				for(String tables:tableList)
					unionStr += tables + " ";
				unionStr += " WHERE "+filterStr;
			}
			unionList.add(unionStr);
			unionStarted = false;
//			System.out.println(unionStr);
		}
		filterList.clear();
	}

	public void visit(OpDiff arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpMinus arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpConditional arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpSequence arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpDisjunction arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpExt arg0) {
		
		
	}

	public void visit(OpList arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpOrder arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpProject arg0) {
		int varMappingCount = 0;
		Map<String,String> preventDuplicates = new HashMap<String,String>(); //to prevent duplicate var names in the case of non unions at this level
		for(Map<String,String> varMapping:varMappings) {
			//build filters
			for(List<String> filterStrs:filterList) {
				String filterStr = filterStrs.get(varMappingCount);
				if(!filterStr.trim().equals("")) {
					String modifier = "";
					if(!whereClause.equals("WHERE ")) {
						modifier = " AND ";
					}
					whereClause += modifier + filterStr;
				}
			}
			filterList.clear();
			
			int count=0;

			for(Var var:arg0.getVars()) {
//				System.out.println(var + ":" + selectClause + ":" + varMapping);
				if(count++>0) {
					selectClause += " , ";
	//				projections += " , ";
				}
				String selectAddition = "";
				if(aliases.containsKey(var.getName())) {
					String colName = aliases.remove(var.getName());
					selectAddition += colName + " AS " + var.getName();
	//				projections += var.getName();
				} else if(varMapping.containsKey(var.getName())){
					String rdmsName = varMapping.remove(var.getName());
					selectAddition += rdmsName;
					if(!rdmsName.equals(var.getName())) {
						selectAddition += " AS " + var.getName();
					}
	//				FormatUtil.processTableName(rdmsName);
					//TODO:take care of NULL
					varMapping.put(var.getName(), var.getName());
	//				projections += var.getName();
				} else {
					selectAddition += var.getName();
	//				projections += var.getName();
	//				count--;
				}
				
				if(!preventDuplicates.containsKey(var.getName())) {
					selectClause += selectAddition;
				} else {
//					System.out.println(var.getName() + ":" + selectAddition + ":" + preventDuplicates.get(var.getName()));
					selectClause = selectClause.replace(preventDuplicates.get(var.getName()), selectAddition);
				}
				preventDuplicates.put(var.getName(),selectAddition);
				
				if(selectClause.trim().endsWith(",")) { //remove extra commas at the end
					selectClause = selectClause.trim().substring(0, selectClause.trim().length()-1);
				}
			}
	//		System.out.println(selectClause);
			
			count = 0; //add from tables
			for(String table:tableList) {
				if(count++>0) {
					fromClause += " , ";
				}
				if(tableToSyntax.containsKey(table)) {
					fromClause += table + tableToSyntax.get(table);
				} else {
					fromClause += table;
				}
			}
			tableList.clear();
			
			//has union
			if(!unionList.isEmpty()) {
				String unionStr = "";
				for(String union:unionList) {
					String modifier = "";
					if(!unionStr.equals(""))
						modifier = " UNION ";
					unionStr += modifier + union;
				}
//				System.out.println(unionStr);
	 			fromClause = "FROM (" + unionStr + ") ";
			}
			
			if(unionStarted==true) {
//				System.out.println(formatSQL());
				unionSelects.add(formatSQL(varMappingCount));
				//clear clauses
				selectClause = "SELECT ";
				preventDuplicates.clear();
			}
			
			varMappingCount++;
		}
		
		String unionStr = "";
		for(String unionPart:unionSelects) {
			if(!unionStr.equals("")) 
				unionStr += " UNION ";
			unionStr += unionPart;
		}
		if(!unionStr.equals("")) {
			previousSelects.add(unionStr);
		}
		unionStarted=false;
		
//		System.out.println(selectClause);
		//		previousSelect = formatSQL();
		if(bgpStarted==true) {
			String sql = formatSQL(0);
			if(!sql.trim().equals(""))
				previousSelects.add(sql);
		} else {
			for(String sel:previousSelects) {
				if(!fromClause.trim().equals("FROM")) {
					fromClause += " , ";
				}
				fromClause += " (" + sel + ") ";
			}
			previousSelects.clear();
			String sql = formatSQL(0);
			if(!sql.trim().equals(""))
				previousSelects.add(sql);
		}
		
		
		//clear clauses
		selectClause = "SELECT ";
		fromClause = "FROM ";
		whereClause = "WHERE ";
		groupClause = "GROUP BY ";
		havingClause = "HAVING ";
		
		bgpStarted = false;
	}

	public void visit(OpReduced arg0) {
		// TODO Auto-generated method stub
		 
	}

	public void visit(OpDistinct arg0) {
		String previousSelect = previousSelects.remove(previousSelects.size()-1);
		previousSelect = previousSelect.replaceFirst("SELECT", "SELECT DISTINCT");
		previousSelects.add(previousSelect);
	}

	public void visit(OpSlice arg0) {
		String previousSelect = previousSelects.remove(previousSelects.size()-1);
		previousSelect += "LIMIT " + arg0.getLength();
		previousSelects.add(previousSelect);
	}

	public void visit(OpGroup group) {
		VarExprList vars = group.getGroupVars();
		Map<Var,Expr> exprMap = vars.getExprs();
		for(int i=globalVarMappingCount;i<varMappings.size();i++) {
			Map<String,String> varMapping = varMappings.get(i);
			Set<String> groupList = new HashSet<String>();
			for(Var var:vars.getVars()) {			
				Expr expr = exprMap.get(var);
				SparqlGroupExprVisitor v = new SparqlGroupExprVisitor();
				v.setMapping(varMapping);
				ExprWalker.walk(v, expr);
				if(!v.getExpression().equals("")) {
					groupList.add(v.getExpression());
					varMapping.put(var.getName(), v.getExpression());
				} else {
					groupList.add(FormatUtil.mapVar(var.getName(),varMapping));
				} 
			}
			groupLists.add(groupList);
		}
		for(int i=globalVarMappingCount;i<varMappings.size();i++) {
			Map<String,String> varMapping = varMappings.get(i);
			for(ExprAggregator agg:group.getAggregators()) {
				SparqlGroupExprVisitor v = new SparqlGroupExprVisitor();
				v.setMapping(varMapping);
				ExprWalker.walk(v, agg);
				varMapping.put(v.getAggKey(),v.getAggVal());
	//			aliases.put(v.getAggKey(),v.getAggVal());
			}
		}
	}

	public void visit(OpTopN arg0) {
		
		
	}
	
	/**
	 * Produces an SQL query from current clauses by producing the group and having clauses and calling the base function.
	 * If cardinality of the sigma function (BGP resolution function) is more than 1, a UNION needs to be done and each formatSQL(index) produces a query to be UNION-ed together
	 * The index identifies which of the results from the sigma function this query should build upon
	 * @param index Integer indicating the index within each of the list of sets
	 * @return String containing the SQL query produced, this might be a partial query (e.g. a UNION or nested part of query)
	 */
	private String formatSQL(int index) {
		if(groupLists.size()>index) {
			Set<String> groupList = groupLists.get(index);
			int count=0;
			for(String expr:groupList) {
				if(count++>0)
					groupClause += " , ";
				//SQO: dont add constants to group by
				if(!FormatUtil.isConstant(expr))
					groupClause += expr;
			}
			groupLists.clear();
		}
		if(havingLists.size()>index) {
			Set<String> havingList = havingLists.get(index);
			int count=0;
			for(String expr:havingList) {
				if(count++>0)
					havingClause += " AND ";
				havingClause += expr;
			}
			havingLists.clear();
		}
		return formatSQL();
	}
	
	/**
	 * Base function that produces an SQL query from the current clauses (e.g. select, where, group by, etc. clauses)
	 * @return String containing the SQL query produced, this might be a partial query (e.g. a UNION or nested part of query)
	 */
	private String formatSQL() {		
		if(selectClause.trim().equals("SELECT")) {
			return "";
		} 
		if(fromClause.trim().equals("FROM")) {
			if(dialect.equals("ESPER") && uriToSyntax.size()>0) {
				for(String val:uriToSyntax.values()) {
					for(Entry<String,String> pair:tableToSyntax.entrySet()) {
						if(pair.getValue().equals(val)) {
							val = pair.getKey() + val;
							break;
						}
					}
					fromClause += " " + val;
				}
			} else {
				return "";
			}
		}
		if(whereClause.trim().equals("WHERE")) {
			whereClause = "";
		}
		if(groupClause.trim().equals("GROUP BY")) {
			groupClause = "";
		}
		if(havingClause.trim().equals("HAVING")) {
			havingClause = "";
		}
		
		return selectClause + " " +
				fromClause + " " +
				whereClause + " " +
				groupClause + " " +
				havingClause + " ";
	}
	
	/**
	 * Returns the final translated SQL statement
	 * @return the translated SQL statement as a string
	 */
	public String getSQL() {
		if(previousSelects.size()>0) {
			return previousSelects.get(0);
		}
		return "";
	}

	/**
	 * Set any named graphs and streaming named graphs with window properties
	 * @param namedGraphURIs list of URIs of named graphs and for streaming named graphs, metadata information appended at the end
	 */
	public void setNamedGraphs(List<String> namedGraphURIs) {
		for(String graphUri:namedGraphURIs) {
			dialect = "ESPER";
			String[] parts = graphUri.split(";");
			if(parts.length>1) {
				String additional = "";
				if(parts.length>3) {
					additional = ".win";
					if(parts[3].equals("TUMBLING")) {
						additional += ":time_batch";
					} else if(parts[3].equals("STEP")) {
						additional += ":time";
					}
					additional += "(" + parts[1] + " " + FormatUtil.timePeriod(parts[2]) + ")";
				} else {
					additional = ".std";
					if(parts[1].equals("LAST")) {
						additional += ":lastevent()";
					}
				}

				uriToSyntax.put(parts[0], additional);
			}
		}
	}

	/**
	 * Sets a catalogue of streams to map uris to actual stream names/identifiers
	 * @param streamCatalog Map containing (streamName,uri)
	 */
	public void setStreamCatalog(Map<String, String> streamCatalog) {
		for(Entry<String,String> stream:streamCatalog.entrySet()) {
			if(uriToSyntax.containsKey(stream.getValue())) {
				tableToSyntax.put(stream.getKey(), uriToSyntax.get(stream.getValue()));
			} else {
				tableToSyntax.put(stream.getKey(), stream.getKey());
			}
		}
		
	}

}