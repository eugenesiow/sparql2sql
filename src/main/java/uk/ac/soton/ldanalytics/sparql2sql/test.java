package uk.ac.soton.ldanalytics.sparql2sql;

import uk.ac.soton.ldanalytics.sparql2sql.model.RdfTableMapping;
import uk.ac.soton.ldanalytics.sparql2sql.model.SparqlOpVisitor;

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
				   "  ?obsval a iot:InternalTemperatureValue;\r\n" + 
				   "    iot:hasQuantityValue ?val.\r\n" + 
				   "  FILTER (?date > \"2012-07-20T00:00:00\"^^xsd:dateTime && ?date < \"2012-07-21T00:00:00\"^^xsd:dateTime)\r\n" + 
				   "} GROUP BY (hours(xsd:dateTime(?date)) as ?hours)";
		
//		String queryStr = "PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\r\n" + 
//				"PREFIX  iotsn: <http://iot.soton.ac.uk/smarthome/sensor#>\r\n" + 
//				"PREFIX  time: <http://www.w3.org/2006/time#>\r\n" + 
//				"PREFIX  ssn:  <http://purl.oclc.org/NET/ssnx/ssn#>\r\n" + 
//				"PREFIX  iot:  <http://purl.oclc.org/NET/iot#>\r\n" + 
//				"\r\n" + 
//				"SELECT ?sensor\r\n" + 
//				"  WHERE\r\n" + 
//				"  {\r\n" +
//				"    ?sensor ssn:onPlatform <http://iot.soton.ac.uk/smarthome/platform#kitchen>\r\n" + 
//				"  }";
		
//		String queryStr = "PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#>\n" + 
//				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
//				"PREFIX time: <http://www.w3.org/2006/time#>\n" + 
//				"PREFIX iotsn: <http://iot.soton.ac.uk/smarthome/sensor#>\n" + 
//				"PREFIX iot: <http://purl.oclc.org/NET/iot#>\n" + 
//				"\n" + 
//				"SELECT (max(?val) as ?max) (min(?val) as ?min) ?day\n" + 
//				"WHERE {\n" + 
//				"  ?instant time:inXSDDateTime ?date.\n" + 
//				"  ?obs ssn:observationSamplingTime ?instant;\n" + 
//				"  	ssn:observedBy iotsn:environmental1;\n" + 
//				"    ssn:observationResult ?snout.\n" + 
//				"  ?snout ssn:hasValue ?obsval.\n" + 
//				"  ?obsval a iot:InternalTemperatureValue;\n" + 
//				"    iot:hasQuantityValue ?val.\n" + 
//				"  FILTER (?date > \"2012-07-01T00:00:00\"^^xsd:dateTime && ?date < \"2012-07-30T00:00:00\"^^xsd:dateTime)\n" + 
//				"} GROUP BY (day(xsd:dateTime(?date)) as ?day)";
		
//		String queryStr = "  PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\n" + 
//				"  PREFIX  iotsn: <http://iot.soton.ac.uk/smarthome/sensor#>\n" + 
//				"  PREFIX  time: <http://www.w3.org/2006/time#>\n" + 
//				"  PREFIX  ssn:  <http://purl.oclc.org/NET/ssnx/ssn#>\n" + 
//				"  PREFIX  iot:  <http://purl.oclc.org/NET/iot#>\n" + 
//				"  \n" + 
//				"  SELECT ?platform ?dateOnly (sum(?power) as ?totalpower)\n" + 
//				"  WHERE\n" + 
//				"  {\n" + 
//				"    {\n" + 
//				"      SELECT ?platform ?hours ?dateOnly (avg(?meterval) as ?power)\n" + 
//				"      WHERE\n" + 
//				"      {\n" + 
//				"        ?meter ssn:onPlatform ?platform.\n" + 
//				"        ?meterobs ssn:observedBy ?meter.\n" + 
//				"        ?meterobs ssn:observationSamplingTime ?meterinstant;\n" + 
//				"          ssn:observationResult ?metersnout.\n" + 
//				"        ?meterinstant time:inXSDDateTime ?meterdate.\n" + 
//				"        ?metersnout ssn:hasValue ?meterobsval.\n" + 
//				"        ?meterobsval a iot:EnergyValue.\n" + 
//				"        ?meterobsval iot:hasQuantityValue ?meterval.\n" + 
//				"        FILTER(?meterval > 0)\n" + 
//				"        FILTER (?meterdate > \"2012-07-01T00:00:00\"^^xsd:dateTime && ?meterdate < \"2012-07-02T00:00:00\"^^xsd:dateTime)\n" + 
//				"      } GROUP BY ?platform ?meter (hours(?meterdate) as ?hours) (xsd:date(?meterdate) as ?dateOnly)\n" + 
//				"    }\n" + 
//				"  } GROUP BY ?platform ?dateOnly";

		Query query = QueryFactory.create(queryStr);
		Op op = Algebra.compile(query);
		System.out.println(op);
		
		RdfTableMapping mapping = new RdfTableMapping();
		mapping.loadMapping("mapping/smarthome_environment.nt");
		mapping.loadMapping("mapping/smarthome_meter.nt");
		mapping.loadMapping("mapping/smarthome_sensors.nt");
		
		SparqlOpVisitor v = new SparqlOpVisitor();
		v.useMapping(mapping);
		OpWalker.walk(op,v);
		System.out.println(v.getSQL());
		
	}
	
}
