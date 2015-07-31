package uk.ac.soton.ldanalytics.sparql2sql.util;

import com.hp.hpl.jena.sparql.expr.NodeValue;

public class FormatUtil {
	public static String parseNodeValue(NodeValue node) {
		if(node.isDateTime()) {
			return node.getDateTime().toString();
		}
		return null;		
	}
}
