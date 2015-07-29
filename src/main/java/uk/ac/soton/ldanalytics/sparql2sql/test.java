package uk.ac.soton.ldanalytics.sparql2sql;

import uk.ac.soton.ldanalytics.sparql2sql.parse.SparqlOpVisitor;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpWalker;

public class test {
	public static void main(String[] args) {
		String queryStr = 
				   "PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#>\r\n" + 
				   "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\r\n" + 
				   "PREFIX time: <http://www.w3.org/2006/time#>\r\n" + 
				   "PREFIX iotsn: <http://iot.soton.ac.uk/smarthome/sensor#>\r\n" + 
				   "PREFIX iot: <http://purl.oclc.org/NET/iot#>\r\n" + 
				   "\r\n" + 
				   "SELECT (avg(?val) as ?sval) ?hours\r\n" + 
				   "WHERE {\r\n" + 
				   "  ?instant time:inXSDDateTime ?date.\r\n" + 
				   "  ?obs ssn:observationSamplingTime ?instant;\r\n" + 
				   "  	ssn:observedBy iotsn:environmental1;\r\n" + 
				   "    ssn:observationResult ?snout.\r\n" + 
				   "  ?snout ssn:hasValue ?obsval.\r\n" + 
				   "  ?obsval a iot:internal;\r\n" + 
				   "    iot:hasQuantityValue ?val.\r\n" + 
				   "  FILTER (?date > \"2012-07-20\"^^xsd:dateTime && ?date < \"2012-07-21\"^^xsd:dateTime)\r\n" + 
				   "} GROUP BY (hours(xsd:dateTime(?date)) as ?hours)";

		Query query = QueryFactory.create(queryStr);
		Op op = Algebra.compile(query);
		System.out.println(op);
		SparqlOpVisitor v = new SparqlOpVisitor();
		OpWalker.walk(op,v);
		
	}
	
}
