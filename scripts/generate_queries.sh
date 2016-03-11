java -cp sparql2sql-0.1.0.jar uk.ac.soton.ldanalytics.sparql2sql.GenerateQueries -I test_queries/smarthome -O test_queries/smarthome/jena -C test_queries/smarthome -E Jena
java -cp sparql2sql-0.1.0.jar uk.ac.soton.ldanalytics.sparql2sql.GenerateQueries -I test_queries/smarthome -O test_queries/smarthome/sesame -C test_queries/smarthome -E Sesame
diff -rq test_queries/smarthome/jena test_queries/smarthome/standard
diff -rq test_queries/smarthome/sesame test_queries/smarthome/standard