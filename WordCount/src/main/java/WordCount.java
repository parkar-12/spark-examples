import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.util.Arrays;
import java.util.List;

public class WordCount {

    public static void main(String args[]){
        SparkSession spark= SparkSession.builder().master("local").getOrCreate();
        Dataset<Row> input= spark.read().text("src/data/file.txt");
        JavaRDD<String> df=input.toJavaRDD().flatMap(row -> Arrays.asList(row.getString(0).split(" ")).iterator());
        JavaPairRDD<String, Integer> counts = df.mapToPair(s -> new Tuple2<>(s, 1)).reduceByKey((i1, i2) -> i1 + i2);
        List<Tuple2<String, Integer>> output = counts.collect();
        for (Tuple2<String,Integer> tuple : output) {
            System.out.println(tuple._1() + ": " + tuple._2());
        }
    }
}
