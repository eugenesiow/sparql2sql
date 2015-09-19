package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.Map;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprAggregator;
import com.hp.hpl.jena.sparql.expr.ExprFunction0;
import com.hp.hpl.jena.sparql.expr.ExprFunction1;
import com.hp.hpl.jena.sparql.expr.ExprFunction2;
import com.hp.hpl.jena.sparql.expr.ExprFunction3;
import com.hp.hpl.jena.sparql.expr.ExprFunctionN;
import com.hp.hpl.jena.sparql.expr.ExprFunctionOp;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.ExprVisitor;
import com.hp.hpl.jena.sparql.expr.NodeValue;

public class SparqlExtendExprVisitor implements ExprVisitor {
	
	String expression = "";
	private Map<String, String> varMapping;
	Boolean isIf = false;

	public void finishVisit() {
		if(isIf) {
			expression = "CASE \n" + expression + "\nEND";
		}
	}

	public void startVisit() {

	}

	public void visit(ExprFunction0 arg0) {

	}

	public void visit(ExprFunction1 func) {

	}

	public void visit(ExprFunction2 arg0) {
//		System.out.println(arg0.getOpName());
	}

	public void visit(ExprFunction3 arg0) {
		if(arg0.getFunctionSymbol().getSymbol().equals("if")) {
			expression = "WHEN " + FormatUtil.processExprType(arg0.getArg1(),varMapping) + " THEN " + arg0.getArg2();
			String elsePart = FormatUtil.processExprType(arg0.getArg3(),varMapping);
			if(!elsePart.startsWith("WHEN")) {
				expression += " ELSE ";
			} else {
				expression += "\n";
			}
			expression += elsePart;
			varMapping.put(arg0.toString(), expression);
			isIf=true;
		}
	}

	public void visit(ExprFunctionN func) {

	}

	public void visit(ExprFunctionOp arg0) {
		
	}

	public void visit(NodeValue arg0) {

	}

	public void visit(ExprVar arg0) {

	}

	public void visit(ExprAggregator arg0) {

	}

	public String getExpression() {
		return expression;
	}

	public void setMapping(Map<String, String> varMapping) {
		this.varMapping = varMapping;
	}

}
