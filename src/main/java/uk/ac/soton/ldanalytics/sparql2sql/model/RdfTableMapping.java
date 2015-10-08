package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class RdfTableMapping {
	Model mapping = null;
	Map<String,Set<String>> joinData = new HashMap<String,Set<String>>();
	
	public RdfTableMapping() {
		mapping = ModelFactory.createDefaultModel();
	}
	
	public void loadMapping(String filename) {
		Model map = ModelFactory.createDefaultModel();
		map.read(filename);
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
