java -cp sparql2sql-0.2.0.jar uk.ac.soton.ldanalytics.sparql2sql.GenerateQueries -I test_queries/srbench -O test_queries/srbench/jena -C test_queries/srbench -E Jena
java -cp sparql2sql-0.2.0.jar uk.ac.soton.ldanalytics.sparql2sql.GenerateQueries -I test_queries/srbench -O test_queries/srbench/sesame -C test_queries/srbench -E Sesame
diff -rq test_queries/srbench/jena test_queries/srbench/standard
diff -rq test_queries/srbench/sesame test_queries/srbench/standard