package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.NotImplementedException;
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
	Map<String,Integer> joinMap = new HashMap<String,Integer>();
	Map<String,String> uriToSyntax = new HashMap<String,String>();	
	Map<String,String> tableToSyntax = new HashMap<String,String>();
	List<Set<String>> groupLists = new ArrayList<Set<String>>();
	List<Set<String>> havingLists = new ArrayList<Set<String>>();
	Map<String,RdfTableMapping> mappingCatalog = new HashMap<String,RdfTableMapping>();
	Map<String,String> namedGraphVars = new HashMap<String,String>();
	Map<String,Set<String>> namedGraphSelect = new HashMap<String,Set<String>>();
	Map<String,String> externalAliases = new HashMap<String,String>();
	List<String> namedGraphHistory = new ArrayList<String>();
	String currentQueryStr = null;
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
	 * Reads a catalogue of mapping closures using the specific SWIBRE engine and loads them into a map in memory
	 * @param a map of named graphs and streams and paths of mapping closures
	 */
	public void setMappingCatalog(Map<String,String> catalog,String engineType) {
		for(Entry<String,String> catalogEntry:catalog.entrySet()) {
			RdfTableMapping mapping = null;
			if(engineType.toLowerCase().equals("jena")) {
				mapping = new RdfTableMappingJena();
			} else if(engineType.toLowerCase().equals("sesame")) {
				mapping = new RdfTableMappingSesame();
			}
			String[] parts = catalogEntry.getValue().split(";");
			for(int i=0;i<parts.length;i++) {
				mapping.loadMapping(parts[i]);
			}
			mappingCatalog.put(catalogEntry.getKey(),mapping);
		}
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
		currentQueryStr = queryStr;
		
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
	
	private Boolean ProcessGraphResults(ResultSet results, String graphUri) { //process results for graph static datasets
		String newGraphUri = graphUri;
		if(namedGraphHistory.contains(graphUri)) {
			int count=0;
			for(String historyUri:namedGraphHistory) {
				if(historyUri.equals(graphUri))
					count++;
			}
			newGraphUri = graphUri + count;
			uriToSyntax.put(newGraphUri, uriToSyntax.get(graphUri) + count);
//			System.out.println(uriToSyntax);
		}
		for(Result result:results.getResults()) {
			for(String var:result.getVarMapping().keySet()) {
				namedGraphVars.put(var,newGraphUri);
			}
		}
		namedGraphHistory.add(graphUri);
		return ProcessResults(results);		
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
		throw new NotImplementedException("QuadPattern not implemented");
	}

	public void visit(OpQuadBlock arg0) {
		throw new NotImplementedException("QuadBlock not implemented");
	}

	public void visit(OpTriple arg0) {
		throw new NotImplementedException("Triple not implemented");
	}

	public void visit(OpQuad arg0) {
		throw new NotImplementedException("Quad not implemented");
	}

	public void visit(OpPath arg0) {
		throw new NotImplementedException("Path not implemented");
	}

	public void visit(OpTable arg0) {
		throw new NotImplementedException("Table not implemented");
	}

	public void visit(OpNull arg0) {
		throw new NotImplementedException("Null not implemented");
	}

	public void visit(OpProcedure arg0) {
		throw new NotImplementedException("Procedure not implemented");
	}

	public void visit(OpPropFunc arg0) {
		throw new NotImplementedException("PropFunc not implemented");
	}

	public void visit(OpFilter filters) {
//		System.out.println(allSelectedNodes.get(allSelectedNodes.size()-1).size());
//		System.out.println("Filter");	
		
		List<String> filterStrs = new ArrayList<String>();
		Map<String,String> filterVarMapping = new HashMap<String,String>();
		for(Map<String,String> varMapping:varMappings) {
			Set<String> havingList = new HashSet<String>();
			String filterStr = "";
			for(Expr filter:filters.getExprs().getList()) {
				SparqlFilterExprVisitor v = new SparqlFilterExprVisitor();
				v.setDialect(dialect);
				v.setFilterVarMapping(filterVarMapping);
				v.setMapping(varMapping);
				v.setMappings(varMappings);
				ExprWalker.walk(v,filter);
				v.finishVisit();
				filterVarMapping.putAll(v.getFilterVarMapping());
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
//			System.out.println(filterStr);
		}
		filterList.add(filterStrs);
		
		
//		System.out.println(whereClause);
	}

	public void visit(OpGraph graphop) {
		String graphUri = graphop.getNode().getURI();
		if(graphUri.contains(";WINDOW")) {
			String[] parts = graphUri.split(";");
			if(parts.length>1) {
				RdfTableMapping windowMapping = mappingCatalog.get(parts[0]);
				hasResults.add(ProcessResults(windowMapping.executeQuery(currentQueryStr,dialect))); //check if there are results for the BGP
			}
		}
		else {
			RdfTableMapping windowMapping = mappingCatalog.get(graphUri);
			hasResults.add(ProcessGraphResults(windowMapping.executeQuery(currentQueryStr,dialect),graphUri));
		}
	}

	public void visit(OpService arg0) {
		throw new NotImplementedException("Service clause not implemented");
	}

	public void visit(OpDatasetNames arg0) {
		throw new NotImplementedException("Datasetnames not implemented");
	}

	public void visit(OpLabel arg0) {
		throw new NotImplementedException("Label not implemented");
	}

	public void visit(OpAssign arg0) {
		throw new NotImplementedException("Assign not implemented");
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
					v.setMappings(varMappings);
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
		// TODO possibly to join the varMappings in join instead of passing all up to project
		
		//add join conditions when same var across mappings
		Map<String,String> allVars = new HashMap<String,String>();
		int count = 0;
		for(Map<String,String> varMapping:varMappings) {
			for(Entry<String,String> varPair:varMapping.entrySet()) {
				String term = "";
				String thisValue = varPair.getValue();
				String allVarVal = allVars.get(varPair.getKey());
				if(allVarVal!=null) {
					term =  ProcessIriMap(allVarVal,thisValue) + "=";
					thisValue = ProcessIriMap(thisValue,allVarVal);
					joinMap.put(term+thisValue,count);
				}
				term += thisValue;	
				allVars.put(varPair.getKey(), term);
			}
			count++;
		}
		filterList.clear();
	}

	private String ProcessIriMap(String mainVal, String refVal) {
		Pattern p = Pattern.compile("\\(\\'(.+)\\'\\|\\|(.+)\\)"); //match ('IRI'||BINDING) format
		if(mainVal.startsWith("(")) {
			Matcher m = p.matcher(mainVal);
			if(m.find()) {
				return m.group(2);
			}			
		}
		if(refVal.startsWith("(")) {
			Matcher m = p.matcher(refVal);
			if(m.find()) {
				return mainVal.replaceAll(m.group(1), "");
			}
		}
		return mainVal;
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
		throw new NotImplementedException("Diff not implemented");
	}

	public void visit(OpMinus arg0) {
		throw new NotImplementedException("Minus not implemented");
	}

	public void visit(OpConditional arg0) {
		throw new NotImplementedException("Conditional not implemented");
	}

	public void visit(OpSequence arg0) {
		throw new NotImplementedException("Sequence not implemented");
	}

	public void visit(OpDisjunction arg0) {
		throw new NotImplementedException("Disjunction not implemented");
	}

	public void visit(OpExt arg0) {
		throw new NotImplementedException("Ext not implemented");
		
	}

	public void visit(OpList arg0) {
		throw new NotImplementedException("List not implemented");
	}

	public void visit(OpOrder arg0) {
		throw new NotImplementedException("Order not implemented");
	}

	public void visit(OpProject arg0) {
		int varMappingCount = 0;
		Map<String,String> preventDuplicates = new HashMap<String,String>(); //to prevent duplicate var names in the case of non unions at this level
		int index = 0;
		for(Map<String,String> varMapping:varMappings) {
			//build filters
			for(List<String> filterStrs:filterList) {
//				String filterStr = filterStrs.get(varMappingCount);
				//hack to take last filter str because it will possibly be the most complete from filterVarMappings
				String filterStr = filterStrs.get(filterStrs.size()-1);
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
				String namedGraph = null;
				String strippedVar = var.toString().replace("?", "");
				if(namedGraphVars.containsKey(strippedVar) && dialect.equals("ESPER")) { //check if var in project is part of a named graph
					namedGraph = namedGraphVars.get(strippedVar);
				}
//				System.out.println(var + ":" + selectClause + ":" + varMapping);
				if(count++>0) {
					if(!selectClause.trim().equals("SELECT"))
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
					if(namedGraph==null) {
						selectClause += selectAddition;
					} else {
						if(dialect.equals("ESPER") && !selectAddition.equals(var.getName()))
							AddNamedGraphSelect(namedGraph,selectAddition);
						selectClause += parseAsParts(selectAddition);
//						count--;
					}
					
				} else {
//					System.out.println(var.getName() + ":" + selectAddition + ":" + preventDuplicates.get(var.getName()));
					String former = preventDuplicates.get(var.getName());
					String latter = selectAddition;
					if(former.length()<latter.length()) { //TODO: this is a temp fix when two graphs are joined and we need the already processed selected elements to be propogated
						selectClause = selectClause.replace(former, latter);
						if(namedGraph!=null) {
							AddNamedGraphSelect(namedGraph,latter);
							if(!selectClause.trim().endsWith(","))
								selectClause += ",";
							selectClause += parseAsParts(latter);
						}
					}
				}
				preventDuplicates.put(var.getName(),selectAddition);
				
				selectClause = trimComma(selectClause);
			}
//			System.out.println(selectClause);
			
			if(dialect.equals("ESPER")) {
				//for streams, build static sql froms
//				Map<String,String> aliasReplace = new HashMap<String,String>();
				for(Entry<String,Set<String>> selects:namedGraphSelect.entrySet()) {
					String namedGraph = selects.getKey();
					String localSelectClause = "SELECT ";
					String localFromClause = "FROM "+namedGraph;
					String sep = "";
					for(String vars:selects.getValue()) {
//						if(vars.contains("AS")) {
//							String[] aliasParts = vars.split("AS");
//							aliasReplace.put(aliasParts[0].trim(), aliasParts[1].trim());
//						}
						localSelectClause += sep + vars ;
						sep = " , ";
					}
	//				localSelectClause = trimComma(localSelectClause);
					//modify where clause for esper
					String whereAdd = "";
					String havingAdd = "";
					if(!whereClause.trim().equals("WHERE")) {
						whereAdd = esperifyWhere(namedGraph,whereClause);
						whereClause = "WHERE ";
					}
					if(!havingClause.trim().equals("HAVING")) {
						havingAdd = esperifyWhere(namedGraph,havingClause);
						havingClause = "WHERE ";
					}
					
					//modify where clause for esper from joinmap
					for(Entry<String,Integer> joinPair:joinMap.entrySet()) {
						if(joinPair.getValue()==varMappingCount) {
							String wherePart = joinPair.getKey();
							if(wherePart.contains(namedGraph + ".")) {// checking clause is part of namedGraph
								if(whereAdd.equals("")) {
									whereAdd = "WHERE ";
								} else {
									whereAdd += " AND ";
								}
								whereAdd += wherePart; 
							}
						}
					}
					
					String tableStr = "[ '"+localSelectClause+" " +localFromClause+" "+FormatUtil.encodeStr(whereAdd,"ESPER_SQL")+" "+havingAdd+"' ] AS "+namedGraph;
//					System.out.println(varMappingCount + tableStr);
					String former = tableToSyntax.get(namedGraph);
					if(former!=null) {
						if(!former.equals(tableStr)) {
							Pattern p = Pattern.compile("\\'(.+)\\'"); //match all tablename.columnname variables in clause
							Matcher mFormer = p.matcher(former);
							Matcher mTableStr = p.matcher(tableStr);
							if(mFormer.find()) {
								if(mTableStr.find()) {
									String[] formerParts = mFormer.group(1).split("WHERE");//strip where
									if(formerParts.length>1) { //combine them
										String newtableStr = mTableStr.group(1);
										if(newtableStr.contains("WHERE")) {
											newtableStr = newtableStr.trim() + " AND ";
										} else {
											newtableStr = newtableStr.trim() + " WHERE ";
										}
										tableStr = tableStr.replace(mTableStr.group(1), newtableStr + formerParts[1].trim());
									}
								}
							}
						}
					}
					tableToSyntax.put(namedGraph, tableStr);
					
					
				}
				
//				//fix aliases in where and having		
//				for(Entry<String,String> aliasPair:aliasReplace.entrySet()) {
//					whereClause = whereClause.replaceAll(aliasPair.getKey(), aliasPair.getValue());
//					havingClause = havingClause.replaceAll(aliasPair.getKey(), aliasPair.getValue());
//				}
			}
			
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
			varMappings.set(index, varMapping);
			index++;
		}
		
		int count = 0; //add from tables
		for(String table:tableList) {
			if(count++>0) {
				fromClause += " , ";
			}
			if(tableToSyntax.containsKey(table)) {
				String additional = tableToSyntax.get(table);
				String prefix = "";
				if(dialect.equals("ESPER") && additional.trim().startsWith("[")) {//if its an sql statement 
					prefix = "sql:";
					additional = " " + fixExternalTables(additional);
				} else if (dialect.equals("ESPER") && additional.startsWith("http")) {
					table = "";
					additional = "";
					count--;
				}
				fromClause += prefix + table + additional;
			} else {
				fromClause += table;
			}
		}
		tableList.clear();
		
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

	private String fixExternalTables(String additional) {
		String[] addSplit = additional.split("] AS"); //check which table this additional belongs to
		if(addSplit.length>1) {
			String currentTable = addSplit[1].trim();
			for(Entry<String,String> alias:externalAliases.entrySet()) {
				String aliasTable = alias.getKey().split("\\.")[0];
				if(!currentTable.equals(aliasTable)) {
					additional = additional.replaceAll(alias.getKey(), "\\$\\{"+alias.getValue()+"\\}");
				}
			}
		}
		return additional;
	}

	private String parseAsParts(String selectAddition) {
		String addPart = "";
		if(selectAddition.contains("AS")) {
			String[] asParts = selectAddition.split("AS");
			String[] origParts = asParts[0].split("\\.");
			String tableName = origParts[0].trim(); //get the 'tablename' the front part before the dot
			String newAlias = asParts[1].trim();
			addPart = tableName + "." + newAlias.toUpperCase() + " AS " + newAlias; //weird but the renamed alias from SQL throws an error if its not capitalised
			externalAliases.put(tableName + "." + origParts[1].trim(), tableName + "." + newAlias.toUpperCase().trim());
		}
		return addPart;
	}

	private String esperifyWhere(String namedGraph, String whereClause) {
		Pattern p = Pattern.compile("([a-zA-Z_$][a-zA-Z_$0-9]*)\\.([a-zA-Z_$][a-zA-Z_$0-9]*)"); //match all tablename.columnname variables in clause
		Matcher m = p.matcher(whereClause);
		while(m.find()) {
			if(!m.group(1).equals(namedGraph)) {
				if(tableList.contains(m.group(1))) {
					String val = externalAliases.get(m.group(0));
					if(val==null) 
						externalAliases.put(m.group(0), m.group(0));
				}
			}
		}
		return whereClause;
	}

	private void AddNamedGraphSelect(String namedGraph, String selectAddition) {
		String graphName = uriToSyntax.get(namedGraph);
		if(graphName==null)
			graphName = namedGraph;
		Set<String> selects = namedGraphSelect.get(graphName);
		if(selects==null) {
			selects = new HashSet<String>();
		}
		selects.add(selectAddition);
		namedGraphSelect.put(graphName, selects);
	}

	private String trimComma(String selectClause) {
		if(selectClause.trim().endsWith(",")) { //remove extra commas at the end
			selectClause = selectClause.trim().substring(0, selectClause.trim().length()-1);
		}
		return selectClause;
	}

	public void visit(OpReduced arg0) {
		throw new NotImplementedException("Reduced not implemented");
	}

	public void visit(OpDistinct arg0) {
		if(previousSelects.size()>0) { 
			String previousSelect = previousSelects.remove(previousSelects.size()-1);
			previousSelect = previousSelect.replaceFirst("SELECT", "SELECT DISTINCT");
			previousSelects.add(previousSelect);
		}
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
		if(fromClause.trim().equals("FROM")) { //if from clause empty
			if(dialect.equals("ESPER") && uriToSyntax.size()>0) { //if esper, build the from clause
				String separator = " ";
				int index = 0;
				for(String val:uriToSyntax.values()) {
//					System.out.println(tableToSyntax.get(val));
					if(index>0) separator = ",";
//					for(Entry<String,String> pair:tableToSyntax.entrySet()) {
//						if(pair.getValue().equals(val)) {
							val = val + tableToSyntax.get(val); //add the window, type and time slide at the end of the stream name
//						}
//					}
					fromClause += separator + val;
					index++;
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
			if(parts.length>1) { //is a stream
				String uri = parts[0];				
				String additional = "";
				if(parts.length>4) {
					additional = ".win";
					if(parts[4].equals("TUMBLING")) {
						additional += ":time_batch";
					} else if(parts[4].equals("STEP")) {
						additional += ":time";
					}
					additional += "(" + parts[1] + " " + FormatUtil.timePeriod(parts[2]) + ")";
					
					if(mappingCatalog.containsKey(uri)) { //rename the stream name in the mapping catalog according to the window name
						RdfTableMapping mapping = mappingCatalog.remove(uri);
						mappingCatalog.put(parts[3], mapping);
					}
				} else {
					additional = ".std";
					if(parts[1].equals("LAST")) {
						additional += ":lastevent()";
					}
				}
				uriToSyntax.put(uri, additional);
			} else { //is a named graph
				uriToSyntax.put(graphUri, graphUri);
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
				uriToSyntax.put(stream.getValue(), stream.getKey()); //put uri, table pairs in
			} else {
				tableToSyntax.put(stream.getKey(), stream.getKey());
			}
		}
		
	}

}