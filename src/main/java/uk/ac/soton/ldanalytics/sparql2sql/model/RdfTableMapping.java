package uk.ac.soton.ldanalytics.sparql2sql.model;

public interface RdfTableMapping {	
	public void loadMapping(String filename);
	public void loadMappingStr(String query);
	public ResultSet executeQuery(String queryStr, String dialect);
	public Boolean hasResults();
}
