package uk.ac.soton.ldanalytics.sparql2sql;

import uk.ac.soton.ldanalytics.sparql2sql.model.RdfTableMapping;
import uk.ac.soton.ldanalytics.sparql2sql.model.SparqlOpVisitor;
import uk.ac.soton.ldanalytics.sparql2sql.util.SQLFormatter;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpWalker;

public class test {
	public static void main(String[] args) {
//		String queryStr = 
//				   "PREFIX ssn: <http://purl.oclc.org/NET/ssnx/ssn#>\r\n" + 
//				   "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\r\n" + 
//				   "PREFIX time: <http://www.w3.org/2006/time#>\r\n" + 
//				   "PREFIX iotsn: <http://iot.soton.ac.uk/smarthome/sensor#>\r\n" + 
//				   "PREFIX iot: <http://purl.oclc.org/NET/iot#>\r\n" + 
//				   "\r\n" + 
//				   "SELECT (avg(?val) as ?sval) ?hours\r\n" + 
//				   "WHERE {\r\n" + 
//				   "  ?instant time:inXSDDateTime ?date.\r\n" + 
//				   "  ?obs ssn:observationSamplingTime ?instant;\r\n" + 
//				   "  	ssn:observedBy iotsn:environmental1;\r\n" + 
//				   "    ssn:observationResult ?snout.\r\n" + 
//				   "  ?snout ssn:hasValue ?obsval.\r\n" + 
//				   "  ?obsval a iot:InternalTemperatureValue;\r\n" + 
//				   "    iot:hasQuantityValue ?val.\r\n" + 
//				   "  FILTER (?date > \"2012-07-20T00:00:00\"^^xsd:dateTime && ?date < \"2012-07-21T00:00:00\"^^xsd:dateTime)\r\n" + 
//				   "} GROUP BY (hours(xsd:dateTime(?date)) as ?hours)";
		
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
		
//		String queryStr = "  PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\n" + 
//				"  PREFIX  iotsn: <http://iot.soton.ac.uk/smarthome/sensor#>\n" + 
//				"  PREFIX  time: <http://www.w3.org/2006/time#>\n" + 
//				"  PREFIX  ssn:  <http://purl.oclc.org/NET/ssnx/ssn#>\n" + 
//				"  PREFIX  iot:  <http://purl.oclc.org/NET/iot#>\n" + 
//				"  PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" + 
//				"  SELECT ?motiondate ?motionhours ?motionplatform ?power ?meter ?name WHERE {\n" + 
//				"    {\n" + 
//				"      SELECT (?platform as ?meterplatform) (?hours as ?meterhours) (?dateOnly as ?meterdate) (avg(?meterval) as ?power) ?meter (sample(?label) as ?name)\n" + 
//				"      WHERE\n" + 
//				"      {\n" + 
//				"        ?meter rdfs:label ?label.\n" + 
//				"        ?meter ssn:onPlatform ?platform.\n" + 
//				"        ?meterobs ssn:observedBy ?meter.\n" + 
//				"        ?meterobs ssn:observationSamplingTime ?meterinstant;\n" + 
//				"          ssn:observationResult ?metersnout.\n" + 
//				"        ?meterinstant time:inXSDDateTime ?date.\n" + 
//				"        ?metersnout ssn:hasValue ?meterobsval.\n" + 
//				"        ?meterobsval a iot:EnergyValue.\n" + 
//				"        ?meterobsval iot:hasQuantityValue ?meterval.\n" + 
//				"        FILTER(?meterval > 0)\n" + 
//				"        FILTER (?date > \"2012-07-01T00:00:00\"^^xsd:dateTime && ?date < \"2012-07-07T00:00:00\"^^xsd:dateTime)\n" + 
//				"      } GROUP BY ?platform (hours(?date) as ?hours) (xsd:date(?date) as ?dateOnly) ?meter\n" + 
//				"    }\n" + 
//				"    {\n" + 
//				"      SELECT (sum(?motionOrNot) as ?isMotion) (?platform as ?motionplatform) (?hours as ?motionhours) (?dateOnly as ?motiondate)\n" + 
//				"      WHERE\n" + 
//				"      {\n" + 
//				"        ?obsval a iot:MotionValue;\n" + 
//				"          iot:hasQuantityValue ?motionOrNot.\n" + 
//				"        ?snout ssn:hasValue ?obsval.\n" + 
//				"        ?obs ssn:observationSamplingTime ?instant;\n" + 
//				"          ssn:observationResult ?snout.\n" + 
//				"        ?instant time:inXSDDateTime ?date.  \n" + 
//				"        ?obs ssn:observedBy ?sensor.\n" + 
//				"        ?sensor ssn:onPlatform ?platform.\n" + 
//				"        FILTER (?date > \"2012-07-01T00:00:00\"^^xsd:dateTime && ?date < \"2012-07-07T00:00:00\"^^xsd:dateTime)\n" + 
//				"      } GROUP BY ?platform (hours(?date) as ?hours) (xsd:date(?date) as ?dateOnly)\n" + 
//				"    }\n" + 
//				"    FILTER(?motionplatform = ?meterplatform && ?motionhours = ?meterhours && ?motiondate = ?meterdate && ?isMotion=0)\n" + 
//				"  }";
		
//		String queryStr = "PREFIX om-owl: <http://knoesis.wright.edu/ssw/ont/sensor-observation.owl#>\n" + 
//				"PREFIX weather: <http://knoesis.wright.edu/ssw/ont/weather.owl#>\n" + 
//				"PREFIX owl-time: <http://www.w3.org/2006/time#>\n" + 
//				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
//				"\n" + 
//				"SELECT DISTINCT ?sensor ?value ?uom\n" + 
//				"WHERE {\n" + 
//				"  ?observation om-owl:procedure ?sensor ;\n" + 
//				"               a weather:RainfallObservation ;\n" + 
//				"               om-owl:result ?result ;\n" + 
//				"               om-owl:samplingTime ?instant .\n" + 
//				"  ?instant owl-time:inXSDDateTime ?time .\n" + 
//				"  ?result om-owl:floatValue ?value ;\n" + 
//				"          om-owl:uom ?uom .\n" + 
//				"  FILTER (?time>\"2003-04-01T00:00:00\"^^xsd:dateTime && ?time<\"2003-04-01T01:00:00\")\n" + 
//				"}";

//		String queryStr = "PREFIX om-owl: <http://knoesis.wright.edu/ssw/ont/sensor-observation.owl#>\n" + 
//				"PREFIX weather: <http://knoesis.wright.edu/ssw/ont/weather.owl#>\n" + 
//				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
//				"PREFIX owl-time: <http://www.w3.org/2006/time#>\n" + 
//				"\n" + 
//				"SELECT ?sensor WHERE {\n" + 
//				"	?observation om-owl:procedure ?sensor ;\n" + 
//				"	           om-owl:observedProperty weather:_WindSpeed ;\n" + 
//				"	           om-owl:result [ om-owl:floatValue ?value ] ;\n" + 
//				"	           om-owl:samplingTime ?instant .\n" + 
//				"	?instant owl-time:inXSDDateTime ?time .\n" + 
//				"	FILTER (?time>\"2003-04-01T00:00:00\"^^xsd:dateTime && ?time<\"2003-04-01T01:00:00\"^^xsd:dateTime)\n" + 
//				"} GROUP BY ?sensor\n" + 
//				"HAVING ( AVG(?value) >= \"74\"^^xsd:float )";
		
		String queryStr = "\n" + 
				"PREFIX om-owl: <http://knoesis.wright.edu/ssw/ont/sensor-observation.owl#>\n" + 
				"PREFIX weather: <http://knoesis.wright.edu/ssw/ont/weather.owl#>\n" + 
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
				"PREFIX owl-time: <http://www.w3.org/2006/time#>\n" + 
				"\n" + 
				"SELECT ?sensor (AVG(?windSpeed) AS ?averageWindSpeed)\n" + 
				"               (AVG(?temperature) AS ?averageTemperature)\n" + 
				"WHERE {\n" + 
				"  ?temperatureObservation om-owl:procedure ?sensor ;\n" + 
				"                          a weather:TemperatureObservation ;\n" + 
				"                          om-owl:result ?temperatureResult ;\n" + 
				"                          om-owl:samplingTime ?instant .\n" + 
				"  ?instant owl-time:inXSDDateTime ?time .\n" + 
				"  FILTER (?time>\"2003-04-01T00:00:00\"^^xsd:dateTime && ?time<\"2003-04-01T01:00:00\"^^xsd:dateTime)\n" + 
				"  ?temperatureResult om-owl:floatValue ?temperature.\n" + 
				"  FILTER(?temperature > \"32\"^^xsd:float)\n" + 
				"  ?windSpeedObservation om-owl:procedure ?sensor ;\n" + 
				"                        a weather:WindSpeedObservation ;\n" + 
				"                        om-owl:samplingTime ?instant ;\n" + 
				"                        om-owl:result [ om-owl:floatValue ?windSpeed ]  .\n" + 
				"}\n" + 
				"GROUP BY ?sensor";
		
		RdfTableMapping mapping = new RdfTableMapping();
		mapping.loadMapping("mapping/4UT01.nt");
//		mapping.loadMapping("mapping/smarthome_environment.nt");
//		mapping.loadMapping("mapping/smarthome_meter.nt");
//		mapping.loadMapping("mapping/smarthome_sensors.nt");
//		mapping.loadMapping("mapping/smarthome_motion.nt");
		
		long startTime = System.currentTimeMillis();
		Query query = QueryFactory.create(queryStr);
		Op op = Algebra.compile(query);
		System.out.println(op);
		
		SparqlOpVisitor v = new SparqlOpVisitor();
		v.useMapping(mapping);
		OpWalker.walk(op,v);
		SQLFormatter formatter = new SQLFormatter();
		
		System.out.println(System.currentTimeMillis() - startTime);
		
		System.out.println(formatter.format(v.getSQL()));
		
	}
	
}
