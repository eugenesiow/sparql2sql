package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.List;
import java.util.Map;

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

public class SparqlExtendExprVisitor implements ExprVisitor {
	
	String fullExpression = "";
	String expression = "";
	String func2Expression = "";
	private Map<String, String> varMapping;
	private List<Map<String, String>> varMappings;
	Boolean isIf = false;

	public void finishVisit() {
		if(isIf) {
			fullExpression = "CASE \n" + fullExpression + "\nEND";
		}
		if(!func2Expression.equals("")) {
			fullExpression = func2Expression + fullExpression;
		}
	}

	public void startVisit() {

	}

	public void visit(ExprFunction0 arg0) {

	}

	public void visit(ExprFunction1 func) {

	}

	public void visit(ExprFunction2 arg0) {
		func2Expression = "( " + FormatUtil.processExprType(arg0.getArg1(),varMappings) + " " + arg0.getOpName() + " " + FormatUtil.processExprType(arg0.getArg2(),varMappings) + " )";
		if(varMappings.size()>0)
			varMappings.get(0).put(arg0.toString(), func2Expression); //add the expression back to the varmappings for the operators outwards
	}

	public void visit(ExprFunction3 arg0) {
		if(arg0.getFunctionSymbol().getSymbol().equals("if")) {
			expression = "WHEN " + FormatUtil.processExprType(arg0.getArg1(),varMapping) + " THEN " + arg0.getArg2();
			String elsePart = FormatUtil.processExprType(arg0.getArg3(),varMapping);
			if(!elsePart.startsWith("if")) {
				expression += " ELSE " + elsePart + "\n";
			} else {
				expression += "\n";
			}
			fullExpression = expression + fullExpression;
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
		return fullExpression;
	}

	public void setMapping(Map<String, String> varMapping) {
		this.varMapping = varMapping;
	}
	
	public void setMappings(List<Map<String,String>> varMappings) {
		this.varMappings = varMappings;
	}

}
