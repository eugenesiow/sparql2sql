package uk.ac.soton.ldanalytics.sparql2sql;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import uk.ac.soton.ldanalytics.sparql2sql.model.RdfTableMapping;
import uk.ac.soton.ldanalytics.sparql2sql.model.SparqlOpVisitor;
import uk.ac.soton.ldanalytics.sparql2sql.util.SQLFormatter;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpWalker;

public class TestGenerateQueries {
	public static void main(String[] args) {
		String queryPath = "/Users/eugene/Dropbox/Private/WORK/LinkedSensorData/queries/";
		String outputPath = "/Users/eugene/Dropbox/Private/WORK/LinkedSensorData/SQLqueries2/";
		String mapPath = "/Users/eugene/Downloads/knoesis_observations_map_meta/4UT01.nt";
		File folder = new File(queryPath);
		
		for(File file:folder.listFiles()) {
			String tempFileName = file.getName();
			System.out.println(tempFileName);
			if(tempFileName.startsWith("."))
				continue;
			if(tempFileName.endsWith(".sparql")) {
				String queryStr = "";
				try {
					queryStr = FileUtils.readFileToString(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
				RdfTableMapping mapping = new RdfTableMapping();
				mapping.loadMapping(mapPath);
				
				Query query = QueryFactory.create(queryStr);
				Op op = Algebra.compile(query);
				SparqlOpVisitor v = new SparqlOpVisitor();
				v.useMapping(mapping);
				OpWalker.walk(op,v);
				SQLFormatter formatter = new SQLFormatter();
				String sql = formatter.format(v.getSQL());
				try {
					BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath + tempFileName.replace("sparql", "sql")));
					bw.append(sql);
					bw.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
