#!/bin/sh

set -o nounset
set -o errexit
set -o pipefail

data_dir="data"

base_dir="$(cd "$(dirname -- "$0")" ; cd .. ; pwd)"

jar_file="$(ls $base_dir/sample-generator/target/sample-generator-*.jar | grep -vi javadoc || true)"
if test -z "$jar_file"
then
    echo "Cannot find the application jar file. Is the project built? Exiting."
    exit 1
fi

app_name="$(basename $0)"
conf_dir="$base_dir/conf"
log_config_file="$conf_dir/log4j.xml"
remote_log_config_file="log4j.xml"
spark_config_file="$conf_dir/spark-yarn-cluster.conf"
spark-submit \
--name "$app_name" \
--properties-file "$spark_config_file" \
--files "$log_config_file#$remote_log_config_file" \
--conf "spark.driver.extraJavaOptions=-Dlog4j.configuration=$remote_log_config_file" \
--conf "spark.executor.extraJavaOptions=-Dlog4j.configuration=$remote_log_config_file" \
--class com.github.tashoyan.recommender.sample.SampleGeneratorMain \
"$jar_file" \
--data-dir "$data_dir" \
--place-count 30000 \
--person-count 3000000
