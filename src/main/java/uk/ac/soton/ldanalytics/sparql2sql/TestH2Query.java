//package uk.ac.soton.ldanalytics.sparql2sql;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//
//import org.apache.commons.io.FileUtils;
//
//import uk.ac.soton.ldanalytics.sparql2sql.model.RdfTableMapping;
//import uk.ac.soton.ldanalytics.sparql2sql.model.SparqlOpVisitor;
//
//import org.apache.jena.query.Query;
//import org.apache.jena.query.QueryFactory;
//import org.apache.jena.sparql.algebra.Algebra;
//import org.apache.jena.sparql.algebra.Op;
//import org.apache.jena.sparql.algebra.OpWalker;
//
//public class TestH2Query {
//	public static void main(String[] args) {
//		String folderPath = "/Users/eugene/Downloads/knoesis_observations_map_meta/";
//		String queryPath = "/Users/eugene/Dropbox/Private/WORK/LinkedSensorData/queries/";
//		String outputPath = "/Users/eugene/Downloads/knoesis_results/";
//		File folder = new File(folderPath);
//		
//		try {
//			Class.forName("org.h2.Driver");
//			
//			String queryName = "q9";
//			for(int run=1;run<=3;run++) {
//				String queryStr = FileUtils.readFileToString(new File(queryPath + queryName + ".sparql"));
//			
//				int totalCount = 0;
//				BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath + "results_"+queryName+"_run"+run+".csv"));
//				
//				Query query = QueryFactory.create(queryStr);
//				Op op = Algebra.compile(query);
//	//			System.out.println(op);
//			
//				for(File file:folder.listFiles()) {
//					String tempFileName = file.getName();
//					if(tempFileName.startsWith("."))
//						continue;
//					String stationName = tempFileName.replace(".nt", "");
//					
//					//as this is a server, it will already be loaded, hence, no need to measure this time
//					RdfTableMapping mapping = new RdfTableMapping();
//					mapping.loadMapping(file.getPath());
//					
//					long startTime = System.currentTimeMillis();
//					SparqlOpVisitor v = new SparqlOpVisitor();
//					v.useMapping(mapping);
//					OpWalker.walk(op,v);
//	//				SQLFormatter formatter = new SQLFormatter();
//					String sql = v.getSQL();
//					long translationTime = System.currentTimeMillis() - startTime;
//					
//					if(!sql.trim().equals("")) {
//						startTime = System.currentTimeMillis();
//						try {
//							Connection conn = DriverManager.getConnection("jdbc:h2:tcp://192.168.0.103/~/h2/LSD_h2_databases/"+stationName, "sa", "");
//							Statement stat = conn.createStatement();
//							ResultSet rs = stat.executeQuery(sql);
//							while (rs.next()) {
//								totalCount++;
//							}
//							rs.close();
//							conn.close();
//							long executionTime = System.currentTimeMillis() - startTime;
//							bw.append(stationName+","+translationTime+","+executionTime+"\n");
//							bw.flush();
//						} catch(SQLException se) {
//							bw.append(stationName+","+translationTime+",err\n");
//							bw.flush();
//							System.out.println(stationName);
//	//						se.printStackTrace();
//						}
//					} else {
//						bw.append(stationName+","+translationTime+",0\n");
//						bw.flush();
//					}
//					
//				}
//				System.out.println(totalCount);
//				
//				bw.close();
//			}
//		} catch (ClassNotFoundException | IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//}
