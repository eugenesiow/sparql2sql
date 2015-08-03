package uk.ac.soton.ldanalytics.sparql2sql.model;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class SelectedNode {
	Statement stmt = null;
	Triple pattern = null;
	Boolean isLeafMap = false;
	Boolean isLeafValue = false;
	String tableName = "";
	String columnName = "";
	String wherePart = "";
	
	public Resource getSubject() {
		Resource subject = null;
		if(stmt!=null)
			subject = stmt.getSubject();
		return subject;
	}
	
	public Boolean isLeafMap() {
		return isLeafMap;
	}
	
	public Boolean isLeafValue() {
		return isLeafValue;
	}
	
	public String getTable() {
		return tableName;
	}
	
	public String getColumn() {
		return columnName;
	}
	
	public String getWherePart() {
		return wherePart;
	}
	
	public void setStatement(Statement stmt) {
		this.stmt = stmt;
		RDFNode object = stmt.getObject();
		if(object.isLiteral()) {
			String[] parts = object.toString().split("\\.");
			if(parts.length>1) {
				isLeafMap = true;
				tableName = parts[0];
				columnName = parts[1];
			}
		}
	}
	public void setBinding(Triple t) {
		pattern = t;
		Node object = pattern.getObject();
		if(object.isLiteral()) {
			isLeafValue = true;
			if(stmt!=null)
				wherePart = columnName + "=" + object.toString() + " ";
		}
	}
	
	public String getVar() {
		String var = "";
		if(pattern.getObject().isVariable()) {
			var = pattern.getObject().getName();
		}
		return var;
	}
		
	
//	@Override
//	public boolean equals(Object obj) {
//		return false;
//	}
}
