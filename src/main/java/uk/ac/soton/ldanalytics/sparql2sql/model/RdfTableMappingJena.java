package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.openrdf.repository.Repository;

import uk.ac.soton.ldanalytics.sparql2sql.riot.RDFReaderMap;
import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

public class RdfTableMappingJena implements RdfTableMapping {
	Model mapping = null;
	Map<String,Set<String>> joinData = new HashMap<String,Set<String>>();
	Boolean hasResults = false;
	String dialect = "H2";
	
	public RdfTableMappingJena() {
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
	
	public ResultSet executeQuery(String queryStr, String dialect) {
		Query query = QueryFactory.create(queryStr);
		QueryExecution qe = QueryExecutionFactory.create(query, mapping);
		this.dialect = dialect;
		
		ResultSet rs = ConvertResults(qe.execSelect());
		qe.close();
		
		return rs;
	}
	
	private ResultSet ConvertResults(org.apache.jena.query.ResultSet results) {
		ResultSet rs = new ResultSet();
		while(results.hasNext()) {
			Binding b = results.nextBinding();
			Result result = new Result();
			Iterator<Var> v = b.vars();
			while(v.hasNext()) {
				Var currentV = v.next();
				Node val = b.get(currentV);
				if(currentV.toString().contains("_info_")) {
					String[] parts = val.getLiteralValue().toString().split("=");
					if(parts.length>1) {
						for(int i=0;i<parts.length;i++) {
							String[] subParts = parts[i].split("\\.");
							if(subParts.length>1) {
								if(!Character.isDigit(subParts[1].charAt(0)))
									result.addTable(subParts[0]);
							}
						}
					}
					result.addWhere(val.getLiteralValue().toString());
				} else {
					if(val.isLiteral()) {
						String value = val.getLiteralValue().toString();
						String[] parts = value.split("\\.");
						if(parts.length>1) {
							if(!Character.isDigit(parts[1].charAt(0)))
								result.addTable(parts[0]);
						}
					}
					result.addVarMapping(currentV.toString().replace("?", ""),FormatUtil.processNode(val,dialect));
				}
			}
			rs.add(result);
		}
		return rs;
	}

	public Boolean hasResults() {
		return hasResults;
	}

//	public Model getCombinedMapping() {
//		return mapping;
//	}
}
