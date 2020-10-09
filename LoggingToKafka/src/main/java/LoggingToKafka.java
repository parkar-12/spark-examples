import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQueryException;

public class LoggingToKafka {
    private static final Logger logger = Logger.getLogger(LoggingToKafka.class);
    public static void main(String[] args) throws IOException, StreamingQueryException {


        SparkSession spark = SparkSession.builder().appName("LoggingToKafka").config("spark.master", "local").getOrCreate();
        Dataset<Row> csvDf=spark.read().option("header","true").csv("data/test.csv");
        logger.info("Info message from LoggingToKafka.main" );
        logger.warn("Warn message from LoggingToKafka.main");
        logger.error("Error message from LoggingToKafka.main" );
        csvDf.show();
    }
}
