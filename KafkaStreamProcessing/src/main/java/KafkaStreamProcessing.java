import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.*;
import org.apache.spark.sql.avro.package$;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.apache.spark.sql.functions.*;

public class KafkaStreamProcessing {
    public static void main(String[] args) throws IOException, StreamingQueryException {

        SparkSession spark = SparkSession.builder().appName("KafkaStreaming").config("spark.master", "local").getOrCreate();
        String schemas = "partition long,offset long";

        /*Creating a dataframe from csv*/
        Dataset<Row> csvDf = spark.readStream().schema(schemas).csv("data/csvData");

        /*Writing csvDf dataframe into kafka in json format (Kafka Sink)*/
        Dataset<Row> jsonOutput = csvDf.selectExpr("to_json(struct(*)) AS value");
        jsonOutput.writeStream().format("kafka").option("kafka.bootstrap.servers", "localhost:9093").option("topic", "test_json").option("checkpointLocation", "checkpintJson").start().awaitTermination();

        /*Writing csvDf dataframe into kafka in avro format (Kafka Sink)*/
        Dataset<Row> avroOutput = csvDf.select(package$.MODULE$.to_avro(struct("*")).as("value"));
        avroOutput.writeStream().format("kafka")
                .option("kafka.bootstrap.servers", "localhost:9093")
                .option("topic", "test_avro").option("checkpointLocation", "checkpintAvro").start().awaitTermination();

        /*Reading from kafka topic having data in json format*/
        Dataset<Row> kafka_json_df = spark.readStream().format("kafka").option("kafka.bootstrap.servers", "localhost:9093").option("subscribe", "test_json").load();
        kafka_json_df.printSchema();
        Dataset<Row> kafka_temp_df = kafka_json_df.selectExpr("CAST(value AS STRING) as value");

        /*If schema is present*/
        StructType schema = DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField("partition", DataTypes.StringType, true),
                DataTypes.createStructField("offset", DataTypes.StringType, true)
        });
        Dataset<Row> intput_temp_df = kafka_temp_df.withColumn("data", functions.from_json(kafka_temp_df.col("value"), schema));
        Dataset<Row> input_json_df1 = intput_temp_df.select("data.*");

        /*Schema not present*/

        Dataset<Row> kafka_batch_df = spark.read().format("kafka").option("kafka.bootstrap.servers", "localhost:9093").option("subscribe", "test_json").load();
        Dataset<Row> input_batch_df = spark.read().json(kafka_batch_df.selectExpr("CAST(value AS STRING) as value").map(Row::mkString, Encoders.STRING()));
        Dataset<Row> input_json_df = kafka_json_df.selectExpr("CAST(value AS STRING) as value")
                .select(from_json(col("value"), input_batch_df.schema()).as("value"));

        input_json_df.show();

        /*Reading from kafka topic having data in avro format*/

        String json_schema = new String(Files.readAllBytes(Paths.get("data/schema.avsc")));
        Dataset<Row> kafka_avro_df = spark.readStream().format("kafka").option("kafka.bootstrap.servers", "localhost:9093").option("subscribe", "test_avro").load();
        Dataset<Row> input_avro_df = kafka_avro_df.select(package$.MODULE$.from_avro(kafka_avro_df.col("value"), json_schema).as("data")).select("data.*");
        input_avro_df.show();
    }
}
