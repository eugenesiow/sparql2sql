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
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;

import uk.ac.soton.ldanalytics.sparql2sql.util.FormatUtil;
import uk.ac.soton.ldanalytics.sparql2sql.util.S2SML;

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
		   con.add(file, null, RDFFormat.NTRIPLES);
		   try (RepositoryConnection mapCon = mapping.getConnection()) {
			   ValueFactory vf = mapCon.getValueFactory();
			   try (RepositoryResult<Statement> statements = con.getStatements(null, null, null, false)) {
				   while (statements.hasNext()) {
				      Statement s = statements.next();
				      if(s.getSubject() instanceof IRI || s.getObject() instanceof IRI) {
				    	  s = vf.createStatement((Resource)checkUri(s.getSubject(),vf), s.getPredicate(), checkUri(s.getObject(),vf));
				      }
				      mapCon.add(s);
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
	
	private Value checkUri(Value node, ValueFactory vf) {
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
			node = vf.createIRI(uri);
		}
		return node;
	}

	public ResultSet executeQuery(String queryStr, String dialect) {
		ResultSet rs = null;
		try (RepositoryConnection con = mapping.getConnection()) {
			//to get sesame to play nice with SPARQL queries with literal types - we remove them temporarily in the queries
			queryStr = queryStr.replaceAll("\\^\\^<.*?>", "");
			
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			rs = ConvertResults(tupleQuery.evaluate(),con.getValueFactory());
		}
		
		return rs;
	}
	
	private ResultSet ConvertResults(TupleQueryResult results, ValueFactory vf) {
		ResultSet rs = new ResultSet();
		while (results.hasNext()) {  // iterate over the result
			BindingSet b = results.next();
			Result result = new Result();
			for(String currentV:b.getBindingNames()) {
				Value val = b.getValue(currentV);
				if(val instanceof IRI && val.stringValue().contains("{")) {
					String uri = val.stringValue();
					Set<String> joins = joinData.get(val.stringValue());
					String joinStr = "";
					if(joins!=null) {
						uri = uri.replace("{}", "{"+joins.iterator().next()+"}");
						for(String parts:joins) {
							String[] subParts = parts.split("\\.");
							if(subParts.length>1) {
								if(!Character.isDigit(subParts[1].charAt(0)))
									result.addTable(subParts[0]);
							}
							if(!joinStr.equals(""))
								joinStr+="=";
							joinStr+=parts;
						}
					}
					if(joinStr.contains("=")) {
						result.addWhere(joinStr);
					}
					val = vf.createIRI(uri);
				} else {
					if(val instanceof Literal) {
						String value = val.stringValue();
						String datatype = ((Literal) val).getDatatype().stringValue();
						if(datatype.equals(S2SML.LITERAL_MAP_IRI)) {
							String[] parts = value.split("\\.");
							if(parts.length>1) {
								if(!Character.isDigit(parts[1].charAt(0))) {
									result.addTable(parts[0]);
								}
							}
						}
					}
				}
//				System.out.println(currentV.replace("?", "") + " "+FormatUtil.processValue(val,dialect));
				result.addVarMapping(currentV.replace("?", ""),FormatUtil.processValue(val,dialect));
			}
			rs.add(result);
		}
		results.close();
		return rs;
	}

	public Boolean hasResults() {
		return hasResults;
	}
}
