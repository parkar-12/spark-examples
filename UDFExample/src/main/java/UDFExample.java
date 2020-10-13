import org.apache.spark.sql.*;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.api.java.UDF2;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.DoubleType;

import static org.apache.spark.sql.types.DataTypes.IntegerType;
import static org.apache.spark.sql.types.DataTypes.StringType;

public class UDFExample {
    public static void main(String[] args){
        SparkSession spark= SparkSession.builder().appName("UDFExample").master("local").getOrCreate();
        Dataset<Row>csvDf=spark.read().option("header","true").csv("data/test.csv");
        csvDf.show();

        //UDF to calculate column/10
        spark.udf().register("divbyten", new UDF1<String, Double>() {
            @Override
            public Double call(String input) throws Exception {
                return Double.parseDouble(input)/10;
            }
        }, DataTypes.DoubleType);

        //UDF to calculate sum of two columns
        spark.udf().register("sum", new UDF2<String, String, Integer>() {
            @Override
            public Integer call(String input1, String input2) throws Exception {
                return Integer.parseInt(input1)+Integer.parseInt(input2);
            }
        }, IntegerType);

        //UDF to calculate difference of two columns
        spark.udf().register("difference", new UDF2<String, String, Integer>() {
            @Override
            public Integer call(String input1, String input2) throws Exception {
                return Integer.parseInt(input1)-Integer.parseInt(input2);
            }
        }, IntegerType);
        csvDf.createOrReplaceTempView("test");
        Dataset<Row> output=spark.sql("select partition,offset,sum(partition,offset) as sum,difference(partition,offset) as difference,divbyten(offset) as offsetbyten from test");
        output.show();
    }
}
