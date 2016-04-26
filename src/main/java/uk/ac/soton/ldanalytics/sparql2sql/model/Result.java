package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result {
	Map<String,String> varMapping = new HashMap<String,String>();
	List<String> tableList = new ArrayList<String>();
	String whereClause = "";
	
	public void addVarMapping(String varName, String val) {
		varMapping.put(varName,val);
	}
	
	public void addTable(String tableName) {
		tableList.add(tableName);
	}
	
	public Map<String,String> getVarMapping() {
		return varMapping;
	}

	public List<String> getTableList() {
		return tableList;
	}
	
	public void addWhere(String where) {
		whereClause = where;
	}
	
	public String getWhere() {
		return whereClause;
	}
	
	public String toString() {
		return varMapping.toString();
	}
}
