package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
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
	List<Resource> traversed = new ArrayList<Resource>();
	Set<SNode> blacklist = new HashSet<SNode>();
	List<SelectedNode> tempSelectedNodes = new ArrayList<SelectedNode>();
	List<SelectedNode> selectedNodes = new ArrayList<SelectedNode>();
	Map<String,String> varMapping = new HashMap<String,String>();
	Map<String,String> aliases = new HashMap<String,String>();
	Set<String> tableList = new HashSet<String>();
	List<String> previousSelects = new ArrayList<String>();
	Set<String> traversedNodes = new HashSet<String>();
	Set<Triple> traversedTriples = new HashSet<Triple>();
//	Set<Node> selNode = new HashSet<Node>();
//	Set<Node> finalSelNode = new HashSet<Node>();
//	Map<Node,Node> selNodes = new HashMap<Node,Node>();
//	Map<Node,Node> finalSelNodes = new HashMap<Node,Node>();
//	Set<NodeObj> selNodes = new HashSet<NodeObj>();
//	Set<NodeObj> finalSelNodes = new HashSet<NodeObj>();
	List<List<SelectedNode>> allSelectedNodes = new ArrayList<List<SelectedNode>>();
	List<String> filterList = new ArrayList<String>();
	List<String> unionList = new ArrayList<String>();
	
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

	public void visit(OpBGP bgp) {
		bgpStarted = true;
		List<Triple> patterns = bgp.getPattern().getList();
		for(Model model:mapping.getMapping()) {
			
//			for(Triple t:patterns) {
//				System.out.println("triple:"+t);
//				graphTraverse(t,model);
//			}
			for(Triple pattern:patterns) {
//				System.out.println("parentt:"+pattern);
				traversedNodes.clear();
				if(!traversedTriples.contains(pattern))
					graphTraverse(patterns,pattern,model);
			}
//			for(Entry<Node,Node> n:finalSelNodes.entrySet()) {
//				System.out.println("final:"+n.getKey()+","+n.getValue());
//			}
			

		}
		//eliminate phase
		Set<Triple> missingTriples = new HashSet<Triple>();
		for(Triple pattern:patterns) {
			Boolean isNotRepresented = true;
			for(SelectedNode n:selectedNodes) {
				if(n.getBinding().equals(pattern)) {
					isNotRepresented = false;
					break;
				}
			}
			if(isNotRepresented)
				missingTriples.add(pattern);
		}
		Queue<Triple> search = new LinkedList<Triple>();
		Set<Triple> removeList = new HashSet<Triple>();		
		
		for(Triple t:missingTriples) {
//			System.out.println("missing:"+t);
			search.add(t);
			removeList.add(t);
		}
			
			
		while(!search.isEmpty()) {
			Triple t = search.poll();
			for(Triple pattern:patterns) {
				if(pattern.getSubject().equals(t.getObject()) || pattern.getObject().equals(t.getSubject()) || pattern.getSubject().equals(t.getSubject())) {
//					System.out.println("remove:"+pattern);
					if(!removeList.contains(pattern)) {
//						System.out.println(pattern);
						search.add(pattern);
						removeList.add(pattern);
					}
				}
			}
		}
		
		for(Triple t:removeList) {
			for (Iterator<SelectedNode> iterator = selectedNodes.iterator(); iterator.hasNext();) {
				SelectedNode n = iterator.next();
			    if (n.getBinding().equals(t)) {
			        iterator.remove();
			    }
			}
		}
		
		for(SelectedNode n:selectedNodes) {
//			System.out.println(n);
			if(n.isLeafValue()) {
				if(!n.isFixedValue()) {
					String modifier = "";
					if(!whereClause.trim().equals("WHERE")) {
						modifier = " AND ";
					}
					whereClause += modifier + n.getWherePart();
				}
			} else if(n.isLeafMap()) {
//				System.out.println(n.getVar() + ":" + n.getTable() + "." + n.getColumn());
				if(n.isFixedValue()) {
					varMapping.put(n.getVar(), n.getColumn());
				} else {
					if(!n.getVar().equals("")) {
						varMapping.put(n.getVar(), n.getTable() + "." + n.getColumn());
						tableList.add(n.getTable());
					}
				}
			} else if(n.isObjectVar) {
				varMapping.put(n.getVar(), "'" + n.getObjectUri() + "'");
			}
			if(n.isSubjectLeafMap()) {
//				System.out.println(n.getSubjectVar() + ":" + n.getSubjectTable() + "." + n.getSubjectColumn());
				if(!n.getSubjectVar().equals("")) {
					varMapping.put(n.getSubjectVar(), n.getSubjectTable() + "." + n.getSubjectColumn());
					tableList.add(n.getSubjectTable());
				}
			} else if(n.isSubjectVar) {
				varMapping.put(n.getSubjectVar(), "'" + n.getSubjectUri() + "'");
			}
		}

		List<SelectedNode> nodesCopy = new ArrayList<SelectedNode>();
		nodesCopy.addAll(selectedNodes);
		selectedNodes.clear();
		allSelectedNodes.add(nodesCopy);
	}
	
	public void graphTraverse(List<Triple> patterns, Triple t, Model model) {
		Node predicate = t.getPredicate();
		Node object = t.getObject();
		Node subject = t.getSubject(); 
		Set<String> results = new HashSet<String>();
//		List<SelectedNode> tempSel = new ArrayList<SelectedNode>();
		for(Statement stmt:getStatements(subject,predicate,object,model)) {
//			System.out.println("stmt:"+stmt+",t:"+t);

			if(t.getSubject().isVariable() && t.getObject().isURI()) {
				if(FormatUtil.compareUriPattern(t.getObject().getURI(), stmt.getObject().asResource().getURI())) {
					SelectedNode node = new SelectedNode();
					node.setStatement(stmt);
					node.setBinding(t);
					selectedNodes.add(node);
				}
			} 
			Boolean subResult = graphTraverseR(patterns,t,model,stmt,"");
			results.add(t.getPredicate()+"|"+t.getObject()+";"+subResult);
			if(subResult) {
				SelectedNode node = new SelectedNode();
				node.setStatement(stmt);
				node.setBinding(t);
				selectedNodes.add(node);
			}
//			System.out.println(t.getPredicate()+"|"+t.getObject()+";"+subResult);
		}
		Boolean add = true;
		for(String resultStr:results) {
//			System.out.println("samplemain:"+resultStr);
			String[] parts = resultStr.split(";");
			if(parts.length>1) {
				if(parts[1].equals("false")) {
					if(!results.contains(parts[0]+";true")) {
//						System.out.println("samplemain:ex");
						add = false;
					}
				}
			}
		}
		
		if(add) {
//			System.out.println("add:"+tempSelectedNodes.size());
//			for(SelectedNode sel:tempSelectedNodes) {
//				System.out.println("add:"+sel);
//			}
//			finalSelNodes.addAll(selNodes);
//			finalSelNodes.putAll(selNodes);
			selectedNodes.addAll(tempSelectedNodes);
			tempSelectedNodes.clear();
//			finalSelNode.addAll(selNode);
		} else {
			tempSelectedNodes.clear();
		}
	}
	
	public Boolean graphTraverseR(List<Triple> patterns, Triple t, Model model, Statement stmt, String fmt) {
		Set<String> results = new HashSet<String>();
//		SelectedNode pnode = new SelectedNode();
//		pnode.setStatement(stmt);
//		pnode.setBinding(t);
//		tempSelectedNodes.add(pnode);
		for(Triple currentT:patterns) {
			Node currentS = currentT.getSubject();
			Node currentP = currentT.getPredicate();
			Node currentO = currentT.getObject();
			Node s = t.getSubject();
			Node o = t.getObject();
			Resource sO = stmt.getObject().isResource() ? stmt.getObject().asResource() : null;
			if(o.equals(currentS)) {
				if(sO!=null) {
					RDFNode nodeO = null;
					StmtIterator stmts = model.listStatements(sO, model.createProperty(currentP.getURI()), nodeO);
					while(stmts.hasNext()) {
						Statement sStmt = stmts.next();
						if(currentO.isURI()) {
							if(sStmt.getObject().isResource()) {
								if(!sStmt.getObject().asResource().getURI().equals(currentO.getURI())) {
//									System.out.println(fmt+"hitfalse:"+sStmt+" t:"+currentT);
//									traversedTriples.add(currentT);
									return false;
								} else {
									SelectedNode node = new SelectedNode();
									node.setStatement(sStmt);
									node.setBinding(currentT);
									tempSelectedNodes.add(node);
								}
							}
						} else if(currentO.isVariable()) {
							String nodeStr = sStmt.getSubject().toString()+":"+currentP.toString()+":"+sStmt.getObject().toString();
							if(!traversedNodes.contains(nodeStr)) {
								traversedNodes.add(nodeStr);
//								selNodes.add(new NodeObj(sStmt.getObject().asNode(),currentT.getObject()));
//								selNodes.put(currentT.getObject(), sStmt.getObject().asNode());
								SelectedNode node = new SelectedNode();
								node.setStatement(sStmt);
								node.setBinding(currentT);
								tempSelectedNodes.add(node);
								if(sStmt.getObject().isResource() || sStmt.getObject().isAnon()) {
//									System.out.println(fmt+"s:"+sStmt+" t:"+currentT);
									Boolean subResult = graphTraverseR(patterns,currentT,model,sStmt,fmt+"\t");
									results.add(currentT.getPredicate()+"|"+currentT.getObject()+";"+subResult);
//									if(subResult==false)
//										System.out.println(fmt+"sfalse:"+currentT);
								}
								else if(sStmt.getObject().isLiteral()) {
//									selNode.add(sStmt.getObject().asNode());
//									System.out.println(fmt+"literal:"+sStmt.getObject()+" t:"+currentT.getObject());
								}
							}
						} 
						else if(currentO.isLiteral()) {
							if(sStmt.getObject().isLiteral()) {
								String lit1 = sStmt.getObject().asLiteral().toString();
								String lit2 = currentO.getLiteralValue().toString();
								if(!lit1.equals(lit2)) {
//									System.out.println(fmt+"hitfalse_literal:"+sStmt.getObject().asLiteral()+","+currentO.getLiteralValue()+","+sStmt.getObject().asLiteral().equals(currentO.getLiteralValue()));
//									traversedTriples.add(currentT);
									return false;
								} else {
									SelectedNode node = new SelectedNode();
									node.setStatement(sStmt);
									node.setBinding(currentT);
									tempSelectedNodes.add(node);
								}
							}
						}
					}
//					traversedTriples.add(currentT);
				}
				
			}
			if(s.equals(currentS) && !o.equals(currentO)) {
//			if(s.equals(currentS)) {
				Resource sS = stmt.getSubject();
				if(sS!=null) {
					Resource nodeO = null;
//					System.out.println("o:"+sO+":"+currentS);
					StmtIterator stmts = model.listStatements(sS, model.createProperty(currentP.getURI()), nodeO);
					while(stmts.hasNext()) {
						Statement sStmt = stmts.next();
//						System.out.println(sStmt);
						if(currentO.isURI()) {
							if(sStmt.getObject().isResource()) {
								if(!sStmt.getObject().asResource().getURI().equals(currentO.getURI())) {
//									System.out.println("hitfalse:"+sStmt+" t:"+currentT);
//									traversedTriples.add(currentT);
									return false;
								}
								else {
									SelectedNode node = new SelectedNode();
									node.setStatement(sStmt);
									node.setBinding(currentT);
									tempSelectedNodes.add(node);
								}
							}
						} else if(currentO.isVariable()) {
							String nodeStr = sStmt.getSubject().toString()+":"+currentP.toString()+":"+sStmt.getObject().toString();
							if(!traversedNodes.contains(nodeStr)) {
								traversedNodes.add(nodeStr);
//								selNodes.add(new NodeObj(sStmt.getObject().asNode(),currentT.getObject()));
//								selNodes.put(currentT.getObject(), sStmt.getObject().asNode());
								SelectedNode node = new SelectedNode();
								node.setStatement(sStmt);
								node.setBinding(currentT);;
								tempSelectedNodes.add(node);
								if(sStmt.getObject().isResource() || sStmt.getObject().isAnon()) {
//									System.out.println(fmt+"p:"+sStmt+" t:"+currentT);
									Boolean subResult = graphTraverseR(patterns,currentT,model,sStmt,fmt+"\t");
									results.add(currentT.getPredicate()+"|"+currentT.getObject()+";"+subResult);
//									if(subResult==false)
//										System.out.println(fmt+"pfalse:"+currentT);
								} 
								else if(sStmt.getObject().isLiteral()) {
//									selNode.add(sStmt.getObject().asNode());
//									System.out.println(fmt+"literala:"+sStmt.getObject()+" t:"+currentT);
								}
							}
						}
						else if(currentO.isLiteral()) {
							if(sStmt.getObject().isLiteral()) {
								String lit1 = sStmt.getObject().asLiteral().toString();
								String lit2 = currentO.getLiteralValue().toString();
								if(!lit1.equals(lit2)) {
//									System.out.println(fmt+"hitfalse_literal:"+sStmt.getObject().asLiteral()+","+currentO.getLiteralValue()+","+sStmt.getObject().asLiteral().equals(currentO.getLiteralValue()));
//									traversedTriples.add(currentT);
									return false;
								} else {
									SelectedNode node = new SelectedNode();
									node.setStatement(sStmt);
									node.setBinding(currentT);
									tempSelectedNodes.add(node);
								}
							}
						}
					}
//					traversedTriples.add(currentT);
				}
			}
			if(s.equals(currentO)) {
				Resource sS = stmt.getSubject();
				if(sS!=null) {
					Resource nodeO = null;
//					System.out.println("o:"+sO+":"+currentS);
					StmtIterator stmts = model.listStatements(nodeO, model.createProperty(currentP.getURI()), sS);
					while(stmts.hasNext()) {
						Statement sStmt = stmts.next();
						if(!stmt.getObject().equals(sStmt.getObject())) {
							if(sStmt.getSubject().isResource() || sStmt.getSubject().isAnon()) {
								String nodeStr = sStmt.getSubject().toString()+":"+currentP.toString()+":"+sStmt.getObject().toString();
								if(!traversedNodes.contains(nodeStr)) {
									SelectedNode node = new SelectedNode();
									node.setStatement(sStmt);
									node.setBinding(currentT);
									tempSelectedNodes.add(node);
									currentT = Triple.create(currentT.getObject(), currentT.getPredicate(), currentT.getSubject());
									sStmt = model.createStatement(sStmt.getObject().asResource(), sStmt.getPredicate(), model.asRDFNode(sStmt.getSubject().asNode()));									
									traversedNodes.add(nodeStr);
//									System.out.println(fmt+"o:"+sStmt+" t:"+currentT);
									Boolean subResult = graphTraverseR(patterns,currentT,model,sStmt,fmt+"\t");
									results.add(currentT.getPredicate()+"|"+currentT.getObject()+";"+subResult);
//									if(subResult==false)
//										System.out.println(fmt+"ofalse:"+currentT);
								}
							}
						}
					}
//					traversedTriples.add(currentT);
				}
			}
		}
		for(String resultStr:results) {
//			System.out.println(fmt+"sample:"+resultStr+" parent:"+t);
			String[] parts = resultStr.split(";");
			if(parts.length>1) {
				if(parts[1].equals("false")) {
					if(!results.contains(parts[0]+";true")) {
//						System.out.println("hitsthis");
//						selNodes.clear();
//						for(SelectedNode n:tempSelectedNodes) {
//							if(n.getSubjectVar().equals("sensor"))
//								System.out.println("cleared:"+n);
//						}
//						System.out.println("cleared_count:"+tempSelectedNodes.size());
						tempSelectedNodes.clear();
//						selNode.clear();
						return false;
					}
				}
			}
		}
//		System.out.println(fmt+"--");
		return true;
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
				String val = varMapping.remove(originalKey);
				varMapping.put(var.getName(), val);
			}
		}
	}

	public void visit(OpJoin arg0) {
		// TODO fix join
		filterList.clear();
	}

	public void visit(OpLeftJoin arg0) {
		if(allSelectedNodes.size()>1) {
			for(SelectedNode right:allSelectedNodes.get(1)) {
				for(SelectedNode left:allSelectedNodes.get(0)) {
					if(left.getSubject().equals(right.getSubject())) {
						if(right.isLeafValue()) {
							String modifier = "";
							if(!whereClause.trim().equals("WHERE")) {
								modifier = " AND ";
							}
							whereClause += modifier + right.getWherePart();
						} else if(right.isLeafMap()) {
			//				System.out.println(n.getVar() + ":" + n.getTable() + "." + n.getColumn());
							varMapping.put(right.getVar(), right.getTable() + "." + right.getColumn());
							tableList.add(right.getTable());
						} else if(right.isObjectVar) {
							varMapping.put(right.getVar(), "'" + right.getObjectUri() + "'");
						}
						break;
					}
				}
			}
		}
	}

	public void visit(OpUnion arg0) {
		int index = allSelectedNodes.size()-1;
		int whereIndex = filterList.size()-1;
		if(unionList.isEmpty()) {
			List<SelectedNode> sel = allSelectedNodes.get(index-1);
			if(!sel.isEmpty()) {
				String unionStr = "SELECT * FROM ";
				for(String tables:tableList)
					unionStr += tables + " ";
				unionStr += " WHERE "+filterList.get(whereIndex-1);
				unionList.add(unionStr);
			}
		}
		List<SelectedNode> sel = allSelectedNodes.get(index);
		if(!sel.isEmpty()) {
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
		
		if(tableList.size()>1) {
//			System.out.println("joins required");
			Map<String, Set<String>> joinMap = new HashMap<String,Set<String>>(); 
			for(SelectedNode node:selectedNodes) {
				if(node.isLeafMap()) {
					String var = node.getVar();
					Set<String> cols = joinMap.get(var);
					if(cols==null) {
						cols = new HashSet<String>();
					}
					cols.add(node.getTable()+"."+node.getColumn());
					joinMap.put(var, cols);
				}
				if(node.isSubjectLeafMap()) {
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
			if(!whereClause.trim().equals("WHERE") && !joinExpression.trim().equals(""))
				whereClause += " AND ";
			whereClause += " " + joinExpression + " ";
		}
		selectedNodes.clear(); //clear the selected node list from any bgps below this projection
		
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
			fromClause += table;
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

}