package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.ArrayList;
import java.util.List;

public class ResultSet {
	List<Result> results = new ArrayList<Result>();

	public List<Result> getResults() {
		return results;
	}
	
	public void add(Result r) {
		results.add(r);
	}
	
}
