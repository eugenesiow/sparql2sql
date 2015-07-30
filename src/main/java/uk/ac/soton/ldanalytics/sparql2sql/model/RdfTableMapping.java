package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RdfTableMapping {
	List<Model> mapping = null;
	
	public RdfTableMapping() {
		mapping = new ArrayList<Model>();
	}
	
	public void loadMapping(String filename) {
		Model map = ModelFactory.createDefaultModel();
		map.read(filename);
		mapping.add(map);
	}
	
	public List<Model> getMapping() {
		return mapping;
	}
}
