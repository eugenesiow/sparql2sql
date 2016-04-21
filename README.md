# sparql2sql
SPARQL to SQL with less joins. Most information about sparql2sql can be found on the [wiki](https://github.com/eugenesiow/sparql2sql/wiki). This includes a reference for the [S2SML](https://github.com/eugenesiow/sparql2sql/wiki/S2SML) language for sparql2sql mappings, [benchmarks](https://github.com/eugenesiow/sparql2sql/wiki/Benchmarks), implementation of the [swappable BGP resolution interface](https://github.com/eugenesiow/sparql2sql/wiki/SWIBRE), etc.

### Transforming Linked Sensor Data from RDF to RDBMS 
To run SRBench on sparql2sql, follow the procedure using the LSD-ETL tool to transform LSD from RDF to RDBMS at https://github.com/eugenesiow/lsd-ETL.

### SRBench

The translated queries using the engine can be found on https://github.com/eugenesiow/sparql2sql/wiki 

### Smart Home Benchmark

The translated queries using the engine can be found on https://github.com/eugenesiow/ldanalytics-PiSmartHome/wiki.

### Running the engine for translating benchmark queries
Download the [release](https://github.com/eugenesiow/sparql2sql/releases/download/0.1.0/sparql2sql.zip).

- Uncompress the release `unzip sparql2sql.zip`
- `chmod 775 test_queries_smarthome.sh`
- `chmod 775 test_queries_srbench.sh`
- `./test_queries_smarthome.sh` to run the smart home benchmark
- `./test_queries_srbench.sh` to run SRBench
- Output will contain the time taken to run reach query and after that will do a diff in the directories of all engines (e.g. Jena, Sesame) and a gold standard SQL output
- Modify `test_queries_smarthome.sh` or `test_queries_srbench.sh` to change parameters of the run
- The usage of the GenerateQueries class is as follows

		usage: LSDTransform -C <folder path> -E <engine name> -I <folder path> -O
		       <folder path>
		 -C,--config <folder path>   the config folder path
		 -E,--engine <engine name>   the engine to use (e.g. Jena, Sesame)
		 -I,--src <folder path>      the source folder path
		 -O,--output <folder path>   the output folder path
- Config files contain a relative path to the mapping from the config path on each line

### Running sparql2sql benchmarks
Benchmarking with SRBench and the Smart Home Analytics Benchmark.
 
* For [GraphDB (OWLIM)](https://github.com/eugenesiow/lsd-ETL/wiki/GraphDB)
* For [Jena Tuple Database (TDB)](https://github.com/eugenesiow/lsd-ETL/wiki/TDB)
* For [sparql2sql (with H2)](https://github.com/eugenesiow/lsd-ETL/wiki/H2)
* For [ontop](https://github.com/eugenesiow/lsd-ETL/wiki/ontop)
* For [morph](https://github.com/eugenesiow/lsd-ETL/wiki/morph)

### Streaming version and benchmarks (sparql2stream)
Benchmarking with SRBench and the Smart Home Analytics Benchmark.
 
* For [sparql2stream](https://github.com/eugenesiow/sparql2stream)
* For [CQELS](https://github.com/eugenesiow/cqels)

### sparql2sql Server

A Jetty-based server to provide a SPARQL endpoint with an RDBMS backend and using the sparql2sql translation engine can be found at  https://github.com/eugenesiow/sparql2sql-server.

### Other Projects
* [LSD-ETL](https://github.com/eugenesiow/lsd-ETL)
* [sparql2stream](https://github.com/eugenesiow/sparql2stream)
* [sparql2sql-server](https://github.com/eugenesiow/sparql2sql-server)
* [Linked Data Analytics](http://eugenesiow.github.io/iot/)
