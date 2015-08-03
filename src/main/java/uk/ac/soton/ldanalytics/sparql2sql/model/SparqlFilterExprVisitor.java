package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

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

public class SparqlFilterExprVisitor implements ExprVisitor {
	String expression = "";
	String currentPart = "";
	List<String> exprParts = new ArrayList<String>();
	String[] fList = {"<",">","=","!="};
	List<String> functionList = Arrays.asList(fList);
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
	}
	
	public String getExpression() {
		return expression;
	}

	public void startVisit() {
		// TODO Auto-generated method stub
		
	}

	public void visit(ExprFunction0 arg0) {
	}

	public void visit(ExprFunction1 arg0) {
	}

	public void visit(ExprFunction2 arg0) {
		if(functionList.contains(arg0.getOpName())) {
			currentPart += FormatUtil.mapVar(arg0.getArg1().getVarName(),varMapping) + arg0.getOpName() +FormatUtil.parseNodeValue(arg0.getArg2().getConstant());
			exprParts.add(currentPart);
			currentPart = "";
		}
		else if(arg0.getOpName().equals("&&")) 
			combinePart = " AND ";
		else if(arg0.getOpName().equals("&&")) 
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

}
