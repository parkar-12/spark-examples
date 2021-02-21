import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQueryException;

public class LoggingToKafka {
    private static final Logger logger = Logger.getLogger(LoggingToKafka.class);
    public static void main(String[] args) throws IOException, StreamingQueryException {

        try {
            SparkSession spark = SparkSession.builder().appName("LoggingToKafka").config("spark.master", "cluster").getOrCreate();
            Dataset<Row> csvDf = spark.read().option("header", "true").csv("/Users/b0212103/IdeaProjects/LoggingToKafka/data/test.csv");
        }catch (Exception e){
            e.printStackTrace();
        }
        try{
         //  Dataset<Row> dt=csvDf.select("off");
          // dt.show();
        } catch (Exception e) {
           // logger.printStackTrace();
        }
        //csvDf.show();
    }
}
