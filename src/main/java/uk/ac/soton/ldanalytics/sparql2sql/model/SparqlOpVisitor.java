package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.algebra.OpVisitor;
import com.hp.hpl.jena.sparql.algebra.op.OpAssign;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpConditional;
import com.hp.hpl.jena.sparql.algebra.op.OpDatasetNames;
import com.hp.hpl.jena.sparql.algebra.op.OpDiff;
import com.hp.hpl.jena.sparql.algebra.op.OpDisjunction;
import com.hp.hpl.jena.sparql.algebra.op.OpDistinct;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpExtend;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpGraph;
import com.hp.hpl.jena.sparql.algebra.op.OpGroup;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpLabel;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpList;
import com.hp.hpl.jena.sparql.algebra.op.OpMinus;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.algebra.op.OpOrder;
import com.hp.hpl.jena.sparql.algebra.op.OpPath;
import com.hp.hpl.jena.sparql.algebra.op.OpProcedure;
import com.hp.hpl.jena.sparql.algebra.op.OpProject;
import com.hp.hpl.jena.sparql.algebra.op.OpPropFunc;
import com.hp.hpl.jena.sparql.algebra.op.OpQuad;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadBlock;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.algebra.op.OpReduced;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.algebra.op.OpService;
import com.hp.hpl.jena.sparql.algebra.op.OpSlice;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.algebra.op.OpTopN;
import com.hp.hpl.jena.sparql.algebra.op.OpTriple;
import com.hp.hpl.jena.sparql.algebra.op.OpUnion;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.core.VarExprList;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprAggregator;
import com.hp.hpl.jena.sparql.expr.ExprWalker;

public class SparqlOpVisitor implements OpVisitor {
	
	RdfTableMapping mapping = null;
	List<Node> eliminated = new ArrayList<Node>();
	List<Resource> uniTraversed = new ArrayList<Resource>();
	Set<Resource> blacklist = new HashSet<Resource>();
	List<SelectedNode> selectedNodes = new ArrayList<SelectedNode>();
	Map<String,String> varMapping = new HashMap<String,String>();
	Map<String,String> aliases = new HashMap<String,String>();
	Set<String> tableList = new HashSet<String>();
	List<String> previousSelects = new ArrayList<String>();
	
//	String previousSelect = "";
	String selectClause = "SELECT ";
//	String projections = "";
	String fromClause = "FROM ";
	String whereClause = "WHERE ";
	String groupClause = "GROUP BY ";	
	String havingClause = "HAVING ";
	
	Boolean bgpStarted = false;
	
	public SparqlOpVisitor() {

	}
	
	public void useMapping(RdfTableMapping mapping) {
		this.mapping = mapping;
	}
	
	public Boolean traverseGraph(Triple t, List<Triple> triples, Model model, QueryPatterns queryPatterns) {
		System.out.println("pattern:"+t);
		List<Node> traversed = new ArrayList<Node>();
		for(Statement stmt:getStatements(t.getSubject(),t.getPredicate(),t.getObject(),model)) {
			if(checkSubject(t,stmt)) {
				System.out.println("start:"+stmt);
				Boolean result = traverseGraphR(t.getSubject(),stmt.getSubject(),triples,model,queryPatterns,traversed,"");
				System.out.println("final:"+result);
			}
		}
		return false;
	}

	private Boolean traverseGraphR(Node startVar, Resource startSubject,
			List<Triple> triples, Model model, QueryPatterns queryPatterns, List<Node> traversed, String format) {
		System.out.println(format+"start_sub:"+startSubject);
		uniTraversed.add(startSubject);
		List<String> trueBranches = new ArrayList<String>();
		List<String> falseBranches = new ArrayList<String>();
			//get links
			for(Triple t:triples) {
				if(t.getSubject().matches(startVar)) {
					for(Statement stmt:getStatements(startSubject.asNode(),t.getPredicate(),t.getObject(),model)) {
						System.out.println(format+stmt);
						Resource r = null;
						if(stmt.getObject().isResource()) {
							r = stmt.getObject().asResource();
						}
						if(!uniTraversed.contains(r)) {
//							if(stmt.getObject().asResource().getURI().equals("http://knoesis.wright.edu/ssw/System_4UT01")) {
//								System.out.println("!!!sys uri");
//								System.out.println(stmt.getObject());
//								for(Node node:uniTraversed) {
//									System.out.println("compare:"+node+":"+stmt.getObject()+":"+node.equals(stmt.getObject()));
//								}
//								
//							}
							if(checkSubject(t,stmt)) {
								if(stmt.getObject().isResource()) {
									System.out.println(format+"o:"+stmt);
//									uniTraversed.add(stmt.getObject().asResource());
									Boolean result = traverseGraphR(t.getObject(),stmt.getObject().asResource(),triples,model,queryPatterns,traversed,format+"\t");
									if(result) 
										trueBranches.add(stmt.getPredicate().toString());
									else 
										falseBranches.add(stmt.getPredicate().toString());
								} else {
									System.out.println(format+"hitleaf:o:"+stmt.getObject());
								}
									
							} else {
								System.out.println("o:hitfalse");
								return false;
							}
						}
					}
				} else if(t.getObject().matches(startVar)) {
					for(Statement stmt:getStatements(t.getObject(),t.getPredicate(),startSubject.asNode(),model)) {
						if(!uniTraversed.contains(stmt.getSubject())) {
							if(checkObject(t,stmt)) {
								if(stmt.getObject().isResource()) {
									System.out.println(format+"s:"+stmt);
//									uniTraversed.add(stmt.getSubject());
									Boolean result = traverseGraphR(t.getSubject(),stmt.getSubject(),triples,model,queryPatterns,traversed,format+"\t");
									if(result) 
										trueBranches.add(stmt.getPredicate().toString());
									else 
										falseBranches.add(stmt.getPredicate().toString());
								}
							} else {
								System.out.println("s:hitfalse");
								return false;
							}
						}
					}
				}
				
//				System.out.println("reach bottom");
//				if(!traversed.contains(t.getSubject())) {
//					traversed.add(t.getSubject());
//					if(t.getSubject().matches(startVar)) {
//						for(Statement stmt:getStatements(startSubject.asNode(),t.getPredicate(),t.getObject(),model)) {
//							if(checkSubject(t,stmt)) {
//								traversed.add(t.getObject());
//								format += "\t";
//								System.out.println(format+stmt);
//								if(stmt.getObject().isResource())
//									traverseGraphR(t.getObject(),stmt.getObject().asResource(),triples,model,queryPatterns,traversed,format);
//							} else {
//								System.out.println("hitfalse:"+stmt);
//								traversed.clear();
//								return;
//							}
//						}
//					} 
//					else if(t.getObject().matches(startVar)) {
//						traversed.clear();
//						System.out.println("\t\tobj"+t);
//					}
//				}
			}
			if(falseBranches.size()==0) 
				return true;
			else {
				for(String falseBranch:falseBranches) {
					if(!trueBranches.contains(falseBranch))
						return false;
				}
				return true;
			}
	}

	public void visit(OpBGP bgp) {
		QueryPatterns selectedQueryPatterns = new QueryPatterns();
		bgpStarted = true;
		//result is a list of table and its columns to select and the variables they are tied to
		for(Model model:mapping.getMapping()) {
//			System.out.println("--------------------START MAPPING----------------------");
			List<Triple> patterns = bgp.getPattern().getList();
			QueryPatterns queryPatterns = new QueryPatterns();
			Resource s = null;
			RDFNode o = null;
			Triple t = patterns.get(0);
			StmtIterator stmts = model.listStatements(s, model.createProperty(t.getPredicate().getURI()), o);

			while(stmts.hasNext()) {
				Statement stmt = stmts.next();
				if(checkSubject(t,stmt)) {
					Boolean result = traverseGraphR(t.getSubject(),stmt.getSubject(),patterns,model,queryPatterns,new ArrayList<Node>(),"");
					System.out.println("final:"+result);
				}
			}
//			for(Triple t:patterns) {
//				if(!selectedQueryPatterns.contains(t)) {
//					QueryPatterns queryPatterns = new QueryPatterns();
//					if(traverseGraph(t,patterns,model,queryPatterns)) {
//						selectedQueryPatterns.addAll(queryPatterns);
//					}
//				}
//			}
		}
	}

	private List<Statement> getStatements(Node subject, Node predicate, Node object, Model model) {
		Resource s = subject.isBlank() ? model.asRDFNode(subject).asResource():null;
		Property p = predicate.isVariable() ? null : model.createProperty(predicate.getURI());
		RDFNode o = object.isBlank() ? model.asRDFNode(object) : null;

		StmtIterator stmts = model.listStatements(s, p, o);
		List<Statement> stmtList = new ArrayList<Statement>();
		while(stmts.hasNext()) {
			Boolean addStatement = true;
			Statement stmt = stmts.next();
//			System.out.println(subject);
			if(!subject.isVariable()) {
				if(!stmt.getSubject().isAnon()) {
					String uri = stmt.getSubject().getURI();
					if(uri.contains("{")) {
						addStatement = FormatUtil.compareUriPattern(subject.getURI(),uri);
					} else {
						if(!uri.equals(subject.getURI()))
							addStatement = false;
					}
				}
			}
			if(!object.isVariable()) {
				RDFNode stmtObj = stmt.getObject();
				if(object.isURI()) {
					if(!stmtObj.isResource()) {
						addStatement = false;
					}
					else if(!stmtObj.isAnon()) {
						String uri = stmtObj.asResource().getURI();
						if(uri.contains("{")) {
							addStatement = FormatUtil.compareUriPattern(object.getURI(),uri);
						} else {
							if(!uri.equals(object.getURI()))
								addStatement = false;
						}
					}
				}
			}
			if(addStatement)
				stmtList.add(stmt);
		}
		return stmtList;
	}

	private Boolean checkSubject(Triple triple, Statement stmt) {
		Node subjectVar = triple.getSubject();
		Node objectVar = triple.getObject();
		Resource subjectNode = stmt.getSubject();
		RDFNode objectNode = stmt.getObject();
		String key = subjectVar.toString() + subjectNode.toString();
		if(blacklist.contains(key)) {
			return false;
		}
		if(!objectVar.isVariable()) {
			if(objectVar.isLiteral()) {
				if(objectNode.isLiteral()) 
					return true;
			} else if (objectVar.isURI()) {
				String uri = objectNode.asResource().getURI();
//				System.out.println("uri:"+uri);
				if(uri.contains("{")) {
					return FormatUtil.compareUriPattern(objectVar.getURI(),uri);
				} else {
					if(!uri.equals(objectVar.getURI()))
						return false;
					else
						return true;
				}
			} else if (objectVar.isBlank()) {
				if(!objectNode.isLiteral())
					return true;
			}
			return false;
		}
		
		return true;
		
	}
	
	private Boolean checkObject(Triple triple, Statement stmt) {
		Node subjectVar = triple.getSubject();
//		Node objectVar = triple.getObject();
		Resource subjectNode = stmt.getSubject();
//		RDFNode objectNode = stmt.getObject();
		
		if(!subjectVar.isVariable()) {
			if(subjectVar.isLiteral()) {
				if(subjectNode.isLiteral()) 
					return true;
			} else if (subjectVar.isURI()) {
				String uri = subjectNode.asResource().getURI();
				if(uri.contains("{")) {
					return FormatUtil.compareUriPattern(subjectVar.getURI(),uri);
				} else {
					if(!uri.equals(subjectVar.getURI()))
						return false;
					else
						return true;
				}
			} else if (subjectVar.isBlank()) {
				if(!subjectNode.isLiteral())
					return true;
			}
			return false;
		}
		
		return true;
		
	}

//	private void eliminate(Triple t, Statement stmt, Model model, List<Triple> patterns) {
//		//check if its a branch and where the branch is
//		Node subject = t.getSubject();
//		Node predicate = t.getPredicate();
//		Node object = t.getObject();
//		List<Resource> validSubjects = new ArrayList<Resource>();
//		for(Statement correctStmt:getStatements(subject,predicate,object,model)) {
//			validSubjects.add(correctStmt.getSubject());
//		}
////		Boolean hasBranch = validSubjects.size() > 0;
////		if(hasBranch) { //calculate the common node where it branches
////			calculateBranchNode(stmt.getSubject(),validSubjects,model);
////		}
//			
//		//backward elimination recursion and mark forward elimination/blacklisting
//		eliminateR(stmt.getSubject(),validSubjects,model, 0, stmt.getSubject(), patterns, t.getSubject(), new ArrayList<Resource>());
//		
//		//forward elimination
//		//add subject to list and also all connected objects if not branch
//	}
//	
//	private int eliminateR(Resource subject, List<Resource> validSubjects,
//			Model model, int count, Resource parent, List<Triple> patterns, Node var, List<Resource> path) {
////		System.out.println("\t"+parent+"->"+subject);
//		traversed.add(subject);
//		
//		if(validSubjects.contains(subject)) {
//			return count;
//		}
//		
//		for(Triple t:patterns) {
//			StmtIterator stmts = null;
//			Node nextVar = null;
//			if(t.subjectMatches(var)) {
//				stmts = model.listStatements(subject,null,(RDFNode)null);
//				nextVar = t.getObject();
//			}
//			else if(t.objectMatches(var)) {
//				stmts = model.listStatements(null,null,subject);
//				nextVar = t.getMatchSubject();
//			}
//			
//			if(stmts!=null) {
//				while(stmts.hasNext()) {
////					if(count==1) { //create new path for each branch out
//						List<Resource> childPath = new ArrayList<Resource>();
////					}
//					
//					Statement stmt = stmts.next();
//					Resource nextNode = null;
//					if(t.subjectMatches(var)) {
//						if(!stmt.getObject().isLiteral()) {
//							nextNode = stmt.getObject().asResource();
//						} else {
//							continue;
//						}
//					} else if(t.objectMatches(var)) {
//						nextNode = stmt.getSubject();
//					}
//		//			System.out.println(o.isLiteral() + ":" + o + ":" + subject + ":" + stmt.getPredicate());
//					if(!traversed.contains(nextNode)) {
//						int nextCount = count + 1;
//		//				System.out.println(o);
//						int tempResult = eliminateR(nextNode.asResource(),validSubjects,model,nextCount,subject,patterns,nextVar,childPath);
//						
////						//on the wind down add the branch
//						path.add(nextNode);
////						if(path!=null) {
////							path.add(nextNode);
////						} else {
//////							System.out.println("path null");
////							path = new ArrayList<Resource>();
////							path.add(nextNode);
////							path.add(subject);
////							eliminateNodes(path);
////							path.clear();
////							path = null;
////						}
//						if(tempResult>0) {
//							if(count==tempResult/2) {
////								for(Resource r:path) {
////									if(blacklist.contains(r)) {
////										blacklist.remove(r);
////									}
////								}
//								//clear the path down the line
//								path.clear();
//								
////								childPath.clear();
////								System.out.println("\n\n"+nextNode);
//							} else {
//								return tempResult;
//							}
//						}
////						if(count==1) {
//						path.addAll(childPath);
////							path.add(parent);
//							eliminateNodes(path);
////						}
//					}
//				}
//			}
//		}
//		
//		return 0;
//	}

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
//		System.out.println("Filter");	
		
		for(Expr filter:filters.getExprs().getList()) {
			SparqlFilterExprVisitor v = new SparqlFilterExprVisitor();
			v.setMapping(varMapping);
			ExprWalker.walk(v,filter);
			v.finishVisit();
			String modifier = "";
			if(!v.getExpression().equals("")) {
				if(!whereClause.equals("WHERE ")) {
					modifier = " AND ";
				}
				whereClause += modifier + v.getExpression();
			} else if(!v.getHavingExpression().equals("")) {
				if(!havingClause.equals("HAVING ")) {
					modifier = " AND ";
				}
				havingClause += modifier + v.getHavingExpression();
			}
		}
		
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
			String originalKey = vars.getExpr(var).getVarName();
			if(aliases.containsKey(originalKey)) {
				String val = aliases.remove(originalKey);
				aliases.put(var.getName(), val);
			} 
			if(varMapping.containsKey(originalKey)) {
				String val = varMapping.remove(originalKey);
				varMapping.put(var.getName(), val);
			}
		}
	}

	public void visit(OpJoin arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpLeftJoin arg0) {
		// TODO Auto-generated method stub
		
	}

	public void visit(OpUnion arg0) {
		// TODO Auto-generated method stub
		
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
		
		
		if(tableList.size()>1) {
//			System.out.println("joins required");
			Map<String, Set<String>> joinMap = new HashMap<String,Set<String>>(); 
			for(SelectedNode node:selectedNodes) {
				if(node.isLeafMap) {
					String var = node.getVar();
					Set<String> cols = joinMap.get(var);
					if(cols==null) {
						cols = new HashSet<String>();
					}
					cols.add(node.getTable()+"."+node.getColumn());
					joinMap.put(var, cols);
				}
				if(node.isSubjectLeafMap) {
					String var = node.getSubjectVar();
					Set<String> cols = joinMap.get(var);
					if(cols==null) {
						cols = new HashSet<String>();
					}
					cols.add(node.getSubjectTable()+"."+node.getSubjectColumn());
					joinMap.put(var, cols);
				}
			}
			
			String joinExpression = "";
			for(Entry<String,Set<String>> joinItem:joinMap.entrySet()) {
				if(joinItem.getValue().size()>1) {
					int count = 0;
					for(String column:joinItem.getValue()) {
						if(count++>0) {
							joinExpression += "=";
						}
						joinExpression += column;
					}
				}
			}
			if(!whereClause.trim().equals("WHERE"))
				whereClause += " AND ";
			whereClause += " " + joinExpression + " ";
		}
		selectedNodes.clear(); //clear the selected node list from any bgps below this projection
		
		int count = 0; //add from tables
		for(String table:tableList) {
			if(count++>0) {
				fromClause += " , ";
			}
			fromClause += table;
		}
		tableList.clear();
		
//		System.out.println("project");
//		if(!previousSelect.equals("")) {//previous projection
////			if(!whereClause.trim().equals("WHERE")) {
////				whereClause += " AND ";
////			}
////			whereClause += projections + " IN (" + previousSelect + ") ";
//			if(!fromClause.trim().equals("FROM")) {
//				fromClause += " , ";
//			}
//			fromClause += " (" + previousSelect + ") ";
//		}
		
		count=0;
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
				varMapping.put(var.getName(), var.getName());
//				projections += var.getName();
			} else {
				selectClause += var.getName();
//				projections += var.getName();
//				count--;
			}
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

}