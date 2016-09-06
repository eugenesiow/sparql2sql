package uk.ac.soton.ldanalytics.sparql2sql.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprFunction;
import org.apache.jena.sparql.expr.NodeValue;
import org.openrdf.model.IRI;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;

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
	
	public static String processExprType(Expr expr, List<Map<String, String>> varMappings) { //if multiple var mappings
		String expression = "";
		for(Map<String, String> varMapping:varMappings) {
			String newExpr = processExprType(expr,varMapping);
			if(!expr.toString().equals(newExpr)) { //if its not equals it must be mapped, so return the result of the mapping
				return newExpr;
			} else {
				expression = newExpr;
			}
		}
		return expression;
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
		} else {
			mappedName = "?"+varName;
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

	private static String processLiteral(String litVal, String datatype) {
//		System.out.println(litVal + "," + datatype);
		//put brackets around literal values if they are not mappings
		if(datatype.equals(S2SML.LITERAL_MAP_IRI)) {
			String[] parts = litVal.split("\\.");
			if(parts.length>1) {
				if(Character.isDigit(parts[1].charAt(0)))
					litVal = "'" + litVal + "'";
			} else {
				litVal = "'" + litVal + "'";
			}
		} else {
			litVal = "'" + litVal + "'";
		}		
		return litVal;
	}
	
	private static String processURI(String uri, String dialect) {
		String concatHead;
		String concatSeperator;
		String concatTail;
		
		switch(dialect) {
			case "ESPER":
				concatHead = "(";
				concatSeperator = "||";
				concatTail = ")";
				break;
			default: 
				concatHead = "CONCAT(";
				concatSeperator = ",";
				concatTail = ")";
				break;
		}
		
		if(uri.contains("{")) {
			String[] parts = uri.split("\\{");
			uri = concatHead;
			for(int i=0;i<parts.length-1;i++) {
				uri += "'"+parts[i]+"'";
				String[] subParts = parts[i+1].split("}");
				uri += concatSeperator + subParts[0];
			}
			uri+=concatTail;
		} else {
			uri = "'" + uri + "'";
		}
		return uri;
	}
	
	public static String processValue(Value v,String dialect) {
		if(v instanceof Literal) {
			return processLiteral(v.stringValue(),((Literal) v).getDatatype().stringValue());
		} else if(v instanceof IRI) {
			return processURI(v.stringValue(),dialect);
		}
		return v.stringValue();
	}
	
	public static String processNode(Node n,String dialect) {
		if(n.isLiteral()) {
			return processLiteral(n.getLiteralLexicalForm(),n.getLiteralDatatypeURI());
		} else if(n.isURI()) {
			return processURI(n.getURI(),dialect);
		}
		return n.toString();
	}

	public static boolean isAggVar(Expr expr) {
		return expr.getVarName().startsWith(".");
	}

	public static String timePeriod(String timeShort) {
		String timeLong = "";
		switch(timeShort) {
		case "ms":
			timeLong = "msec";
			break;
		case "s":
			timeLong = "sec";
			break;
		case "m":
			timeLong = "min";
			break;
		case "h":
			timeLong = "hour";
			break;
		case "d":
			timeLong = "day";
			break;
		}
		return timeLong;
	}

	public static boolean isConstant(String expr) {
		if(expr.startsWith("'") && expr.endsWith("'"))
			return true;
		return false;
	}
	
	
}
