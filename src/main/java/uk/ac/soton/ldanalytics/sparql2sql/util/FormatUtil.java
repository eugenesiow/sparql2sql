package uk.ac.soton.ldanalytics.sparql2sql.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprFunction;
import com.hp.hpl.jena.sparql.expr.NodeValue;

public class FormatUtil {	
	public static String parseNodeValue(NodeValue node) {
		if(node.isDateTime()) {
			return "'" + node.getDateTime().toString() + "'";
		} else if(node.isInteger()) {
			return node.getInteger().toString();
		} else if(node.isFloat()) {
			return Float.toString(node.getFloat());
		}
		else {
			return "'" + node.getString() + "'";
		}		
	}

	public static String processExprType(Expr expr, Map<String, String> varMapping) {
		String expression = "";
		if(expr.isFunction()) {
			ExprFunction func = expr.getFunction();
			if(varMapping.containsKey(func.toString())) {
				return varMapping.get(func.toString());
			}
			if(func.getFunctionIRI()!=null) {
				if(func.getFunctionIRI().equals("http://www.w3.org/2001/XMLSchema#date"))
					expression = "CAST(" + mapVar(func.getArg(1).getVarName(),varMapping) + " AS DATE)";
				else
					expression = mapVar(func.getArg(1).getVarName(),varMapping);
			}
			else {
				String innerExpression = "";
				if(func.getArg(1).isFunction()) {
					innerExpression = processExprType(func.getArg(1),varMapping);
				} else {
					innerExpression = mapVar(func.getArg(1).getVarName(),varMapping);
				}
				if(func.getArgs().size()==1) {
					expression = symbolMap(func.getFunctionSymbol().getSymbol()) + "(" + innerExpression + ")";
				} else if((func.getArgs().size()==2)) {
					//TODO: process arg2
					expression = innerExpression + " " + symbolMap(func.getFunctionSymbol().getSymbol()) + " " + func.getArg(2);
				} else {
					expression = symbolMap(func.getFunctionSymbol().getSymbol()) + "(" + innerExpression + ")";
				}
			}
		} else if(expr.isVariable()) {
			expression = mapVar(expr.getVarName(),varMapping);
		} else {
			expression = expr.toString();
		}
		return expression;
	}

	public static String symbolMap(String symbol) {
		String inputSymbol = symbol.toLowerCase();
		Map<String, String> symbolMap = new HashMap<String,String>();
		symbolMap.put("lt", "<");
		symbolMap.put("le", "<=");
		symbolMap.put("ge", ">=");
		symbolMap.put("gt", ">");
		symbolMap.put("eq", "=");
		symbolMap.put("hours", "HOUR");
		symbolMap.put("sample", "MAX");
		if(symbolMap.containsKey(inputSymbol)) {
			inputSymbol = symbolMap.get(inputSymbol);
		}
		return inputSymbol;
	}

	public static String mapVar(String varName, Map<String, String> varMapping) {
		String mappedName = varName;
		if(varMapping.containsKey(varName)) {
			mappedName = varMapping.get(varName);
		}
		return mappedName;
	}

	public static Boolean compareUriPattern(String uri, String pattern) {
		String stripPattern = pattern.replaceAll("\\{.*?}", "\\|\\|");
		String[] splitPattern = stripPattern.split("\\|\\|");
		for(String part:splitPattern) {
			if(!uri.contains(part))
				return false;
		}
		return true;
	}
	
	public static List<String> extractCols(String uri) {
		List<String> cols = new ArrayList<String>();
		Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(uri);
		while(m.find()) {
			cols.add(m.group(1));				
		}
		return cols;
	}

	public static String handleExpr(Expr expr, Map<String, String> varMapping) {
		if(expr.isVariable()) {
			return FormatUtil.mapVar(expr.getVarName(),varMapping);
		} else if(expr.isConstant()) {
			return parseNodeValue(expr.getConstant());
		}
		return expr.toString();
	}

	public static String processLiteral(Node object) {
		return object.getLiteral().getValue().toString();
	}
	
	public static String processNode(Node n) {
		if(n.isLiteral()) {
			return processLiteral(n);
		} else if(n.isURI()) {
			String uri = n.getURI();
			if(uri.contains("{")) {
				String[] parts = uri.split("\\{");
				uri = "CONCAT(";
				for(int i=0;i<parts.length-1;i++) {
					uri += "'"+parts[i]+"'";
					String[] subParts = parts[i+1].split("}");
					uri += "," + subParts[0];
				}
				uri+=")";
			} else {
				uri = "'" + uri + "'";
			}
			return uri;
		} else {
			return n.toString();
		}
	}

	public static boolean isAggVar(Expr expr) {
		return expr.getVarName().startsWith(".");
	}
	
	
}
