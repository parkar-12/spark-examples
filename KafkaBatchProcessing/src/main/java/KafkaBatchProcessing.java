
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.*;
import org.apache.spark.sql.avro.package$;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.apache.spark.sql.functions.*;


public class KafkaBatchProcessing {
    public static void main(String[] args) throws IOException {

        SparkSession spark = SparkSession.builder().appName("KafkaBatch").config("spark.master", "local").getOrCreate();
        String schemas = "partition long,offset long";

        /*Creating a dataframe from csv*/
        Dataset<Row> csvDf = spark.read().option("header", "true").csv("data/test.csv");

        /*Writing csvDf dataframe into kafka in json format (Kafka Sink)*/
        Dataset<Row> jsonOutput = csvDf.selectExpr("to_json(struct(*)) AS value");
        jsonOutput.write().format("kafka").option("kafka.bootstrap.servers", "localhost:9093").option("topic", "test_json").save();

        /*Writing csvDf dataframe into kafka in avro format (Kafka Sink)*/
        Dataset<Row> avroOutput = csvDf.select(package$.MODULE$.to_avro(struct("*")).as("value"));
        avroOutput.write().format("kafka")
                .option("kafka.bootstrap.servers", "localhost:9093")
                .option("topic", "test_avro").save();

        /*Reading from kafka topic having data in json format*/
        Dataset<Row> kafka_json_df = spark.read().format("kafka").option("kafka.bootstrap.servers", "localhost:9093").option("subscribe", "test_json").load();
        kafka_json_df.printSchema();
        Dataset<Row> kafka_temp_df = kafka_json_df.selectExpr("CAST(value AS STRING) as value");

        /*If schema is present*/
        StructType schema = DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField("partition", DataTypes.StringType, true),
                DataTypes.createStructField("offset", DataTypes.StringType, true)
        });
        Dataset<Row> input_json = kafka_temp_df.withColumn("data", functions.from_json(kafka_temp_df.col("value"), schema)).select("data.*");
        input_json.show();

        /*Schema not present*/
        JavaRDD<String> store = kafka_temp_df.toJavaRDD().map(x -> x.mkString());
        Dataset<Row> input_json_df1 = spark.read().json(store);
        input_json_df1.show();

        /*OR*/
        Dataset<Row> input_json_df = spark.read().json(kafka_json_df.selectExpr("CAST(value AS STRING) as value").map(Row::mkString, Encoders.STRING()));
        input_json_df.show();

        /*Reading from kafka topic having data in avro format*/

        String json_schema = new String(Files.readAllBytes(Paths.get("data/schema.avsc")));
        Dataset<Row> kafka_avro_df = spark.read().format("kafka").option("kafka.bootstrap.servers", "localhost:9093").option("subscribe", "test_avro").load();
        Dataset<Row> input_avro_df = kafka_avro_df.select(package$.MODULE$.from_avro(kafka_avro_df.col("value"), json_schema).as("data")).select("data.*");
        input_avro_df.show();
    }
}
