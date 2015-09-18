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
	private String aggKey="";
	private String aggVal="";

	public void finishVisit() {
		// TODO Auto-generated method stub
		
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
			System.out.println(FormatUtil.processExprType(arg0.getArg1(),varMapping));
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
		aggKey = arg0.getAggVar().getVarName();
		aggVal = FormatUtil.symbolMap(arg0.getAggregator().getName()) + "("; 
		for(Expr expr:arg0.getAggregator().getExprList()) {
			aggVal += FormatUtil.processExprType(expr, varMapping); 
		}
		aggVal += ")";
	}

	public String getExpression() {
		return expression;
	}

	public void setMapping(Map<String, String> varMapping) {
		this.varMapping = varMapping;
	}

}
