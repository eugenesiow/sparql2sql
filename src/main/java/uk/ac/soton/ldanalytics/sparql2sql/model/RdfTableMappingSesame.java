package uk.ac.soton.ldanalytics.sparql2sql.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.OpenRDFException;
import org.openrdf.model.IRI;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;

public class RdfTableMappingSesame implements RdfTableMapping {
	Repository mapping = null;
	Map<String,Set<String>> joinData = new HashMap<String,Set<String>>();
	Boolean hasResults = false;
	String dialect = "H2";
	
	public RdfTableMappingSesame() {
		mapping = new SailRepository(new MemoryStore());
		mapping.initialize();
	}
	
	public void loadMapping(String filename) {
		Repository repo = new SailRepository(new MemoryStore());
		repo.initialize();
		
		File file = new File(filename);
		try (RepositoryConnection con = repo.getConnection()) {
		   con.add(file, null, RDFFormat.RDFXML);
		   try (RepositoryConnection mapCon = mapping.getConnection()) {
			   try (RepositoryResult<Statement> statements = con.getStatements(null, null, null, false)) {
				   while (statements.hasNext()) {
				      Statement s = statements.next();
				      if(s.getSubject() instanceof IRI || s.getObject() instanceof IRI) {
				    	  mapCon.add((Resource)checkUri(s.getSubject()), s.getPredicate(), checkUri(s.getObject()));
				      }
				   }
			   }
		   } catch (OpenRDFException e1) {
			   e1.printStackTrace();
		   }
		}
		catch (OpenRDFException e) {
		   e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private Value checkUri(Value node) {
		if(node instanceof IRI) {
			String uri = node.stringValue();
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
		return node;
	}

	public ResultSet executeQuery(String queryStr, String dialect) {
		ResultSet rs = new ResultSet();
//		try (RepositoryConnection conn = repo.getConnection()) {
//			   String queryString = "SELECT ?x ?y WHERE { ?x ?p ?y } ";
//			   TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
//
//			   TupleQueryResult result = tupleQuery.evaluate(); 
//			   try (TupleQueryResult result = tupleQuery.evaluate()) {
//			      while (result.hasNext()) {  // iterate over the result
//				 BindingSet bindingSet = result.next();
//				 Value valueOfX = bindingSet.getValue("x");
//				 Value valueOfY = bindingSet.getValue("y");
//
//				 // do something interesting with the values here...
//			      }
//			   }  
//			}
		
		return rs;
	}

	public Boolean hasResults() {
		return hasResults;
	}
}
