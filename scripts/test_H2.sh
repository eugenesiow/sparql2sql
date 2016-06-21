for ((i=$1;i<=$2;i++))
do
	java -cp sparql2sql-0.2.0.jar uk.ac.soton.ldanalytics.sparql2sql.TestH2Query /Users/eugene/Downloads/knoesis_observations_map_snow_meta/ /Users/eugene/Dropbox/Private/WORK/LinkedSensorData/queries/ /Users/eugene/Downloads/knoesis_results_gizmo2/ q$i 3 jdbc:h2:tcp://192.168.0.101/~/LSD_h2_databases/
done