package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class SelectedNode {
	Statement stmt = null;
	Triple pattern = null;
	private Boolean isLeafMap = false;
	private Boolean isSubjectLeafMap = false;
	private Boolean isLeafValue = false;
	Boolean isSubjectVar = false;
	Boolean isObjectVar = false;
	private Boolean isFixedValue = false;
	String tableName = "";
	String columnName = "";
	String subjectTableName = "";
	String subjectColumnName = "";
	String subjectUri = "";
	String objectUri = "";
	
	public Triple getBinding() {
		return pattern;
	}
	
	public Resource getSubject() {
		Resource subject = null;
		if(stmt!=null)
			subject = stmt.getSubject();
		return subject;
	}
	
	public Boolean isLeafMap() {
		return isLeafMap;
	}
	
	public Boolean isFixedValue() {
		return isFixedValue;
	}
	
	public String getSubjectUri() {
		return subjectUri;
	}
	
	public String getObjectUri() {
		return objectUri;
	}
	
	public Boolean isSubjectLeafMap() {
		return isSubjectLeafMap;
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
	
	public String getSubjectTable() {
		return subjectTableName;
	}
	
	public String getSubjectColumn() {
		return subjectColumnName;
	}
	
	public String getWherePart() {
		String wherePart = "";
		Node object = pattern.getObject();
		if(stmt!=null) {
			if(!(tableName + "." + columnName).equals(FormatUtil.processLiteral(object)))
				wherePart = tableName + "." + columnName + "=" + FormatUtil.processLiteral(object) + " ";
		}
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
				if(Character.isDigit(parts[1].charAt(0))) { //column name shouldnt start with digit, it is possibly a float
					isFixedValue = true;
					columnName = "'"+object.toString()+"'";
				} 
			}
		} else if(object.isResource()) {
			checkIfResourceIsLeafMap(object.asResource().getURI(),true);
		}
		
		Resource subject = stmt.getSubject();
		if(subject.isURIResource()) {
			checkIfResourceIsLeafMap(subject.getURI(),false);
		}
	}
	
	private void checkIfResourceIsLeafMap(String uri, Boolean isObject) {
		if(uri!=null && uri.contains("{")) {
			Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(uri);
			while(m.find()) {
				String parts[] = m.group(1).split("\\.");
				if(parts.length>1) {
					if(isObject) {
						isLeafMap = true;
						tableName = parts[0];
						columnName = parts[1];
					} else {
						isSubjectLeafMap = true;
						subjectTableName = parts[0];
						subjectColumnName = parts[1];
					}
				}
			}
		} else {
			if(isObject)
				objectUri = uri;
			else
				subjectUri = uri;
		}
	}
	
	public void setBinding(Triple t) {
		pattern = t;
		if(t.getObject().isLiteral())
			isLeafValue = true;
		else if(t.getObject().isVariable())
			isObjectVar = true;
		if(t.getSubject().isVariable())
			isSubjectVar = true;
	}
	
	public String getVar() {
		String var = "";
		if(pattern.getObject().isVariable()) {
			var = pattern.getObject().getName();
		}
		return var;
	}
	
	public String getSubjectVar() {
		String var = "";
		if(pattern.getSubject().isVariable()) {
			var = pattern.getSubject().getName();
		}
		return var;
	}
	
	public String toString() {
		return stmt.toString() + ":" + pattern.toString();
	}
		
	
//	@Override
//	public boolean equals(Object obj) {
//		return false;
//	}
}
