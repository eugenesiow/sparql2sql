package uk.ac.soton.ldanalytics.sparql2sql;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;

import uk.ac.soton.ldanalytics.sparql2sql.model.RdfTableMapping;
import uk.ac.soton.ldanalytics.sparql2sql.model.RdfTableMappingJena;
import uk.ac.soton.ldanalytics.sparql2sql.model.RdfTableMappingSesame;
import uk.ac.soton.ldanalytics.sparql2sql.model.SparqlOpVisitor;
import uk.ac.soton.ldanalytics.sparql2sql.util.SQLFormatter;

public class GenerateQueries {
	public static void main(String[] args) {
		Options options = new Options();
		
		Option src = Option.builder("I")
				.longOpt("src")
				.required(true)
				.hasArg()
				.desc( "the source folder path" )
				.argName("folder path")
				.build();
		
		Option config = Option.builder("C")
				.longOpt("config")
				.required(true)
				.hasArg()
				.desc( "the config folder path" )
				.argName("folder path")
				.build();
		
		Option output = Option.builder("O")
				.longOpt("output")
				.required(true)
				.hasArg()
				.desc( "the output folder path" )
				.argName("folder path")
				.build();
		
		Option engine = Option.builder("E")
				.longOpt("engine")
				.required(true)
				.hasArg()
				.desc( "the engine to use (e.g. Jena, Sesame)" )
				.argName("engine name")
				.build();
		
		options.addOption(src);
		options.addOption(config);
		options.addOption(output);
		options.addOption(engine);
		
//		args = new String[]{ "-I test_queries/smarthome", "-O test_queries/smarthome", "-C test_queries/smarthome", "-E Jena"};
		
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);
			
			String inputPath = cmd.getOptionValue( "I" ).trim();
			String configPath = cmd.getOptionValue( "C" ).trim();
			String outputPath = cmd.getOptionValue( "O" ).trim();
			String engineName = cmd.getOptionValue( "E" ).trim().toLowerCase();
			
			File folder = new File(inputPath);
			for(File file:folder.listFiles()) {
				String fileName = file.getName(); 
				if(fileName.endsWith(".sparql")) {
					String configFileName = fileName.replace(".sparql", ".config");
					String outputFileName = fileName.replace(".sparql", ".sql");
					File configFile = new File(configPath + File.separator + configFileName);
					if(configFile.exists()) {
						RdfTableMapping mapping = null;
						if(engineName.equals("sesame")) {
							mapping = new RdfTableMappingSesame(); 
						} else {
							mapping = new RdfTableMappingJena();
						}
						
						BufferedReader br = new BufferedReader(new FileReader(configFile));
						String line="";
						while((line=br.readLine())!=null) {
							mapping.loadMapping(line);
						}
						br.close();
						
						String queryStr = "";
						try {
							queryStr = FileUtils.readFileToString(file);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						Query query = QueryFactory.create(queryStr);
						Op op = Algebra.compile(query);
						SparqlOpVisitor v = new SparqlOpVisitor();
						v.useMapping(mapping);
						OpWalker.walk(op,v);
						SQLFormatter formatter = new SQLFormatter();
						String sql = formatter.format(v.getSQL());
						try {
							BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath + File.separator + outputFileName));
							bw.append(sql);
							bw.close();
						} catch(IOException e) {
							e.printStackTrace();
						}
					} else {
						System.err.println("Config File: "+configFileName+" not found.");
					}
				}
			}
		} catch (ParseException | IOException e) {
			System.err.println(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "LSDTransform", options, true );
		}
	}
}
