package uk.ac.soton.ldanalytics.sparql2sql.riot;

import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.adapters.RDFReaderRIOT;

public class RDFReaderMap extends RDFReaderRIOT {

	public RDFReaderMap(String lang) {
		super(lang);
	}
	
	@Override
    public void read(Model model, InputStream r, String base) {
        startRead(model) ;
        RDFDataManager.read(model, r, base, hintlang) ;
        finishRead(model) ;
    }
	
}
