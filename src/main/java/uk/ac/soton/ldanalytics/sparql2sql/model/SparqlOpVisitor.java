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
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.main.StageGenerator;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprWalker;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

public class SparqlOpVisitor implements OpVisitor {
	
	RdfTableMapping mapping = null;
	List<List<Binding>> bgpBindings = new ArrayList<List<Binding>>();
	Map<String,String> varMapping = new HashMap<String,String>();
	Map<String,String> aliases = new HashMap<String,String>();
	Set<String> tableList = new HashSet<String>();
	List<String> previousSelects = new ArrayList<String>();
	List<String> filterList = new ArrayList<String>();
	List<String> unionList = new ArrayList<String>();
	List<Boolean> hasResults = new ArrayList<Boolean>();
	Map<String,String> uriToSyntax = new HashMap<String,String>();	
	Map<String,String> tableToSyntax = new HashMap<String,String>();
	
	String selectClause = "SELECT ";
	String fromClause = "FROM ";
	String whereClause = "WHERE ";
	String groupClause = "GROUP BY ";	
	String havingClause = "HAVING ";
	
	String dialect = "H2";
	Boolean bgpStarted = false;
	
	public SparqlOpVisitor() {
		StageGenerator origStageGen = (StageGenerator)ARQ.getContext().get(ARQ.stageGenerator) ;
        StageGenerator stageGenAlt = new StageGeneratorAlt(origStageGen) ;
        ARQ.getContext().set(ARQ.stageGenerator, stageGenAlt) ;
	}
	
	public void useMapping(RdfTableMapping mapping) {
		this.mapping = mapping;
	}

	public void visit(OpBGP bgp) {
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
	
	private Boolean ProcessResults(ResultSet results) {
		Boolean hasResults = false;
		List<Result> bindingSet = new ArrayList<Result>();
		for(Result result:results.getResults()) {
			hasResults = true;
			bindingSet.add(result);
			AddVarMappings(result);
		}
//		bgpBindings.add(bindingSet);
		return hasResults;
	}

	private void AddVarMappings(Result rs) {
		varMapping.putAll(rs.getVarMapping());
//		Iterator<Var> v = b.vars();
//		while(v.hasNext()) {
//			Var currentV = v.next();
//			Node val = b.get(currentV);
//			if(currentV.toString().contains("_info_")) {
//				String[] parts = val.getLiteralValue().toString().split("=");
//				if(parts.length>1) {
//					for(int i=0;i<parts.length;i++) {
//						String[] subParts = parts[i].split("\\.");
//						if(subParts.length>1) {
//							if(!Character.isDigit(subParts[1].charAt(0)))
//								tableList.add(subParts[0]);
//						}
//					}
//				}
//				if(!whereClause.trim().equals("WHERE"))
//					whereClause += " AND ";
//				whereClause += val.getLiteralValue().toString();
//			} else {
//				if(val.isLiteral()) {
//					String value = val.getLiteralValue().toString();
//					String[] parts = value.split("\\.");
//					if(parts.length>1) {
//						if(!Character.isDigit(parts[1].charAt(0)))
//							tableList.add(parts[0]);
//					}
//				}
//				varMapping.put(currentV.toString().replace("?", ""), FormatUtil.processNode(val,dialect));
//			}
//		}
	}

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
		String filterStr = "";
//		System.out.println(allSelectedNodes.get(allSelectedNodes.size()-1).size());
//		System.out.println("Filter");	
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
				if(!havingClause.equals("HAVING ")) {
					modifier = " AND ";
				}
				havingClause += modifier + v.getHavingExpression();
			}
		}
		filterList.add(filterStr);
		
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
				SparqlExtendExprVisitor v = new SparqlExtendExprVisitor();
				v.setMapping(varMapping);
				ExprWalker.walk(v,expr);
				v.finishVisit();
				aliases.put(var.getName(), v.getExpression());
			}
			if(aliases.containsKey(originalKey)) {
				String val = aliases.remove(originalKey);
				aliases.put(var.getName(), val);
			} 
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

	public void visit(OpJoin arg0) {
		// TODO fix join
		filterList.clear();
	}

	public void visit(OpLeftJoin arg0) {
		/*
		if(hasResults.size()>1) {
			for(Binding right:bgpBindings.get(1)) {
				for(Binding left:bgpBindings.get(0)) {
//					System.out.println(right +" "+ left);
					Iterator<Var> rightV = right.vars();
					Boolean discardRow = false;
					while(rightV.hasNext()) {
						Var v = rightV.next();
						if(left.contains(v)) {
							if(!left.get(v).equals(right.get(v))) {
								discardRow = true;
								break;
							}
						}
					}
					if(!discardRow)
						AddVarMappings(right);
				}
			}
		}*/
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
			String unionStr = "SELECT * FROM ";
			for(String tables:tableList)
				unionStr += tables + " ";
			unionStr += " WHERE "+filterList.get(whereIndex);
			unionList.add(unionStr);
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
		
		//build filters
		for(String filterStr:filterList) {
			if(!filterStr.trim().equals("")) {
				String modifier = "";
				if(!whereClause.equals("WHERE ")) {
					modifier = " AND ";
				}
				whereClause += modifier + filterStr;
			}
		}
		filterList.clear();
		
//		if(tableList.size()>1) { 
//			Map<String, Set<String>> joinMap = new HashMap<String,Set<String>>();
//			for(List<SelectedNode> selN:allSelectedNodes) {
//				for(SelectedNode node:selN) {
//					if(node.isLeafMap()) {
//						String var = node.getVar();
//						Set<String> cols = joinMap.get(var);
//						if(cols==null) {
//							cols = new HashSet<String>();
//						}
//						cols.add(node.getTable()+"."+node.getColumn());
//						joinMap.put(var, cols);
//					}
//					if(node.isSubjectLeafMap()) {
//						String var = node.getSubjectVar();
//						Set<String> cols = joinMap.get(var);
//						if(cols==null) {
//							cols = new HashSet<String>();
//						}
//						cols.add(node.getSubjectTable()+"."+node.getSubjectColumn());
//						joinMap.put(var, cols);
//					}
//				}
//			}
//			
//			String joinExpression = "";
//			for(Entry<String,Set<String>> joinItem:joinMap.entrySet()) {
//				if(joinItem.getValue().size()>1) {
//					int count = 0;
//					for(String column:joinItem.getValue()) {
//						if(count++>0) {
//							joinExpression += "=";
//						}
//						joinExpression += column;
//					}
//				}
//			}
//			if(!whereClause.trim().equals("WHERE") && !joinExpression.trim().equals(""))
//				whereClause += " AND ";
//			whereClause += " " + joinExpression + " ";
//		}
//		allSelectedNodes.clear(); //clear the selected node list from any bgps below this projection
			
		int count=0;
		for(Var var:arg0.getVars()) {
			if(count++>0) {
				selectClause += " , ";
//				projections += " , ";
			}
			if(aliases.containsKey(var.getName())) {
				String colName = aliases.remove(var.getName());
				selectClause += colName + " AS " + var.getName();
//				projections += var.getName();
			} else if(varMapping.containsKey(var.getName())){
				String rdmsName = varMapping.remove(var.getName());
				selectClause += rdmsName;
				if(!rdmsName.equals(var.getName())) {
					selectClause += " AS " + var.getName();
				}
//				FormatUtil.processTableName(rdmsName);
				//TODO:take care of NULL
				varMapping.put(var.getName(), var.getName());
//				projections += var.getName();
			} else {
				selectClause += var.getName();
//				projections += var.getName();
//				count--;
			}
		}
		
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
//			System.out.println(unionStr);
 			fromClause = "FROM (" + unionStr + ") ";
		}
		
//		System.out.println(selectClause);
//		previousSelect = formatSQL();
		if(bgpStarted==true) {
			previousSelects.add(formatSQL());
		} else {
			for(String sel:previousSelects) {
				if(!fromClause.trim().equals("FROM")) {
					fromClause += " , ";
				}
				fromClause += " (" + sel + ") ";
			}
			previousSelects.clear();
			previousSelects.add(formatSQL());
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
//		System.out.println("group");
		VarExprList vars = group.getGroupVars();
		Map<Var,Expr> exprMap = vars.getExprs();
		int count = 0;
		for(Var var:vars.getVars()) {
			Expr expr = exprMap.get(var);
			SparqlGroupExprVisitor v = new SparqlGroupExprVisitor();
			v.setMapping(varMapping);
			ExprWalker.walk(v, expr);
			if(count++>0) {
				groupClause += " , ";
			}
			if(!v.getExpression().equals("")) {
				groupClause += v.getExpression();
				varMapping.put(var.getName(), v.getExpression());
//				aliases.put(var.getName(), v.getExpression());
			} else {
				groupClause += FormatUtil.mapVar(var.getName(),varMapping);
			} 
		}
//		System.out.println("group:"+groupClause);
		for(ExprAggregator agg:group.getAggregators()) {
			SparqlGroupExprVisitor v = new SparqlGroupExprVisitor();
			v.setMapping(varMapping);
			ExprWalker.walk(v, agg);
			varMapping.put(v.getAggKey(),v.getAggVal());
//			aliases.put(v.getAggKey(),v.getAggVal());
		}
	}

	public void visit(OpTopN arg0) {
		
		
	}
	
	private String formatSQL() {		
		if(selectClause.trim().equals("SELECT")) {
			return "";
		} 
		if(fromClause.trim().equals("FROM")) {
			return "";
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
	
	public String getSQL() {
		if(previousSelects.size()>0) {
			return previousSelects.get(0);
		}
		return null;
	}

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