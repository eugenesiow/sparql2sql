package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import uk.ac.soton.ldanalytics.sparql2sql.riot.RDFReaderMap;
import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

public class RdfTableMapping {
	Model mapping = null;
	Map<String,Set<String>> joinData = new HashMap<String,Set<String>>();
	
	public RdfTableMapping() {
		mapping = ModelFactory.createDefaultModel();
	}
	
	public void loadMapping(String filename) {
		Model map = ModelFactory.createDefaultModel();
		RDFReaderMap rd = new RDFReaderMap("N-Triples");
		try {
		InputStream in = new FileInputStream(filename);
		rd.read(map, in,"");
		in.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
//		map.read(filename);
		mapping.add(JoinMap(map));
	}
	
	private Model JoinMap(Model map) {
		Model newMap = ModelFactory.createDefaultModel();
		StmtIterator stmts = map.listStatements();
		while(stmts.hasNext()) {
			Statement s = stmts.next();
			if(s.getSubject().isURIResource() || s.getObject().isURIResource()) {
				s = newMap.createStatement(checkUri(s.getSubject().asNode(),newMap).asResource(), s.getPredicate(), checkUri(s.getObject().asNode(),newMap));
			}
			newMap.add(s);
		}
		return newMap;
	}

	private RDFNode checkUri(Node node, Model map) {
		if(node.isURI()) {
			String uri = node.getURI();
			if(uri.contains("{")) {
				List<String> colList = FormatUtil.extractCols(uri);
				uri = uri.replaceAll("\\{.*?}", "\\{\\}");
				Set<String> existing = joinData.get(uri);
				if(existing==null) {
					existing = new HashSet<String>();
				}
				existing.addAll(colList);
				joinData.put(uri, existing);
			}
		}
		return map.asRDFNode(node);
	}

	public Model getCombinedMapping() {
		return mapping;
	}
}
