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

public class SparqlGroupExprVisitor implements ExprVisitor {
	
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
//		System.out.println(arg0);
	}

	public void visit(ExprFunction1 func) {
		if(func.isFunction()) {
			expression = FormatUtil.processExprType(func,varMapping) ;
		}
	}

	public void visit(ExprFunction2 arg0) {

	}

	public void visit(ExprFunction3 arg0) {

	}

	public void visit(ExprFunctionN func) {
//		System.out.println(arg0);
		if(func.isFunction()) {
			expression = FormatUtil.processExprType(func,varMapping) ;
		}
	}

	public void visit(ExprFunctionOp arg0) {
		
	}

	public void visit(NodeValue arg0) {
		
		
	}

	public void visit(ExprVar arg0) {
		expression = arg0.getVarName();
		
	}

	public void visit(ExprAggregator arg0) {
		aggKey = arg0.getAggVar().getVarName();
		aggVal = FormatUtil.symbolMap(arg0.getAggregator().getName()) + "("; 
		for(Expr expr:arg0.getAggregator().getExprList()) {
			aggVal += FormatUtil.processExprType(expr, varMapping); 
		}
		aggVal += ")";
	}
	
	public String getAggKey() {
		return aggKey;
	}
	
	public String getAggVal() {
		return aggVal;
	}

	public String getExpression() {
		return expression;
	}

	public void setMapping(Map<String, String> varMapping) {
		this.varMapping = varMapping;
		
	}

}
