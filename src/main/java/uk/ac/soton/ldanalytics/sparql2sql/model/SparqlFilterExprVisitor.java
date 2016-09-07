package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.expr.ExprFunction0;
import org.apache.jena.sparql.expr.ExprFunction1;
import org.apache.jena.sparql.expr.ExprFunction2;
import org.apache.jena.sparql.expr.ExprFunction3;
import org.apache.jena.sparql.expr.ExprFunctionN;
import org.apache.jena.sparql.expr.ExprFunctionOp;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.ExprVisitor;
import org.apache.jena.sparql.expr.NodeValue;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

public class SparqlFilterExprVisitor implements ExprVisitor {
	String expression = "";
	String havingExpression = "";
	String currentPart = "";
	Set<String> exprParts = new LinkedHashSet<String>();
	Set<String> havingParts = new LinkedHashSet<String>();
	String[] fList = {"<",">","=","!=","<=",">="};
	String[] aList = {"+","-","*","/"};
	List<String> functionList = Arrays.asList(fList);
	List<String> arithmeticList = Arrays.asList(aList);
	private List<Map<String, String>> varMappings;
	String combinePart = "";
	private Map<String, String> varMapping;

	public void finishVisit() {
		int counter = 0;
		for(String exprPart:exprParts) {
			expression += exprPart;
			if(++counter<exprParts.size()) {
				 expression += combinePart;
			}
		} 
		counter = 0;
		for(String havingPart:havingParts) {
			havingExpression += havingPart;
			if(++counter<havingParts.size()) {
				havingExpression += combinePart;
			}
		}
	}
	
	public String getExpression() {
		return expression;
	}
	
	public String getHavingExpression() {
		return havingExpression;
	}

	public void startVisit() {
		// TODO Auto-generated method stub
		
	}

	public void visit(ExprFunction0 arg0) {
	}

	public void visit(ExprFunction1 arg0) {
	}

	public void visit(ExprFunction2 args) {
		if(functionList.contains(args.getOpName())) {
//			System.out.println(args);
			Expr leftSide = args.getArg1();
			Expr rightSide = args.getArg2();
			currentPart = FormatUtil.handleExpr(leftSide,varMapping) + args.getOpName() + FormatUtil.handleExpr(rightSide, varMapping);
			if(FormatUtil.isAggVar(leftSide)) {
				havingParts.add(currentPart);
			} else {
				exprParts.add(currentPart);
			}
			currentPart = "";
		} else if(arithmeticList.contains(args.getOpName())) {
			Expr leftSide = args.getArg1();
			Expr rightSide = args.getArg2();
			currentPart = FormatUtil.handleExpr(leftSide,varMappings) + args.getOpName() + FormatUtil.handleExpr(rightSide, varMappings);
			varMapping.put(args.toString(), currentPart);
//			System.out.println(varMapping);
//			System.out.println(currentPart);
		}
		else if(args.getOpName().equals("&&")) 
			combinePart = " AND ";
		else if(args.getOpName().equals("||")) 
			combinePart = " OR ";
	}

	public void visit(ExprFunction3 arg0) {

	}

	public void visit(ExprFunctionN arg0) {
		
	}

	public void visit(ExprFunctionOp arg0) {
		
	}

	public void visit(NodeValue arg0) {
		//currentPart += arg0.asQuotedString();
		
	}

	public void visit(ExprVar exprVar) {
//		currentPart += exprVar.getVarName();
	}

	public void visit(ExprAggregator arg0) {
		
	}

	public void setMapping(Map<String,String> varMapping) {
		this.varMapping = varMapping;		
	}
	
	public void setMappings(List<Map<String,String>> varMappings) {
		this.varMappings = varMappings;
	}

}
