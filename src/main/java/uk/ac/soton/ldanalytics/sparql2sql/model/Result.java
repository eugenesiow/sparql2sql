package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.HashMap;
import java.util.Map;

public class Result {
	Map<String,String> varMapping = new HashMap<String,String>();

	public void addVarMapping(String varName, String val) {
		varMapping.put(varName,val);
	}
	
	public Map<String,String> getVarMapping() {
		return varMapping;
	}

}
