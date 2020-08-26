package com.alink.ml.utils;

import com.alibaba.alink.common.io.filesystem.HadoopFileSystem;
import com.alibaba.alink.operator.batch.BatchOperator;
import com.alibaba.alink.operator.batch.source.CsvSourceBatchOp;
import com.alibaba.alink.operator.batch.sql.UnionAllBatchOp;
import com.alink.ml.test.ParamsBase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 常用工具类
 * Created by edc on 2020/8/12
 */
public final class Utils {

    public static Gson pGson =
            new GsonBuilder()
                    .serializeNulls()
                    .disableHtmlEscaping()
                    .serializeSpecialFloatingPointValues()
                    .create();

    public static Logger logger = LogManager.getLogger(Utils.class);

    /**
     * 读取hdfs上的csv文件 或者文件夹下的所有csv文件
     *
     * @param str "hdfs:/user/experiment/tmp/14b9ba80-853f-11ea-92f2-000c29192c75-00"
     * @param "hdfs://master:9000"
     * @return
     */
    public static BatchOperator readBatchOpFromHDFS(String str) {
        try {
            //读取hadoop上schema的数据
            String schemaStr = readSchemaFromHDFS(str);

            //截掉不需要的hdfs： 前缀 /user/experiment/tmp/14b9ba80-853f-11ea-92f2-000c29192c75-00
            String path = str.substring(5);

            logger.info("path is " + path);
            System.out.println("path is " + path);


            logger.info("schemaStr  is " + schemaStr);
            System.out.println("schemaStr  is " + schemaStr);

            //分情况读取所有的数据 读取成BatchOpertor并返回
            HadoopFileSystem hdfs = new HadoopFileSystem(Config.HADOOP_FSURI);
            if (hdfs.getFileStatus(path).isDir()) {

                List<String> strings = hdfs.listFiles(path);
                BatchOperator[] Batch = new BatchOperator[strings.size() - 1];
                int i = 0;
                for (String string : strings) {
                    if (!string.equals(Config.HADOOP_FSURI + path + "/_SUCCESS")) {

//                        logger.info("遍历的目录地址 " + string);
                        CsvSourceBatchOp csvSourceBatchOp = new CsvSourceBatchOp()
                                .setFilePath(string)
                                .setSchemaStr(schemaStr)
                                .setIgnoreFirstLine(true);
                        Batch[i] = csvSourceBatchOp;
                        i++;
                    }
                }

                return new UnionAllBatchOp().linkFrom(Batch);
            } else {

                CsvSourceBatchOp csvSourceBatchOp = new CsvSourceBatchOp()
                        .setFilePath(Config.HADOOP_FSURI + path)
                        .setSchemaStr(schemaStr)
                        .setIgnoreFirstLine(true);

                return csvSourceBatchOp;
            }

        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
        } finally {


        }
        logger.error("读取数据源失败");
        System.out.println("读取数据源失败");
        return new CsvSourceBatchOp();
    }

    /**
     * 从hdfs上读取schema文件 获取schemaStr
     *
     * @param path "hdfs:/user/experiment/tmp/14b9ba80-853f-11ea-92f2-000c29192c75-00"
     * @param "hdfs://master:9000"
     * @return "f0 double,f1 int"
     */
    public static String readSchemaFromHDFS(String path) {

        try {
            path = path.substring(5);
            String rrr = "/root/schema" + path.substring(20) + "_schema";
//            logger.info(rrr);
            System.out.println(rrr);
            FSDataInputStream in = new HadoopFileSystem(Config.HADOOP_FSURI).open(rrr);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String s = bufferedReader.readLine();
            System.out.println("getSchema  " + s);
            return s.trim();
        } catch (IOException e) {
            System.out.println("getSchema  error");
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 解析参数 参数对照参考api
     *
     * @param para json字符串
     * @return 解析的hashmap
     */
    public static HashMap<String, String> json2map(String para) {
        HashMap map = pGson.fromJson(para, HashMap.class);
        logger.info("json2map params is " + map);
        HashMap<String, String> map1 = new HashMap<>();
        map.forEach((o, o2) -> map1.put(o.toString(), o2.toString()));
        return map1;
    }


    /**
     * 从总的参数map里面获取str数组参数若没有返回null
     *
     * @param parameter
     * @param key
     * @return
     */
    public static String[] strArrayOrNull(HashMap<String, String> parameter, String key) {
        if (parameter.getOrDefault(key, "---").equals("---")) {
            return null;
        } else {
            String s = parameter.get("reservedCols");
            String s1 = s.substring(1, s.length() - 1);
            return s1.split(",");
        }

    }

    /**
     * 从总的参数map里面获取str数组参数若没有返回null
     *
     * @param parameter
     * @param key
     * @return
     */
    public static double[] douArrayOrDeafult(HashMap<String, String> parameter, String key, double[] dou) {
        if (parameter.getOrDefault(key, "---").equals("---")) {
            return dou;
        } else {
            String s = parameter.get(key);
            String[] split = s.substring(1, s.length() - 1).split(",");
            return Arrays.stream(split).mapToDouble(Double::parseDouble).toArray();
        }

    }

    /**
     * 从总的参数map里面获取int数组若没有返回null
     *
     * @param parameter
     * @param key
     * @return
     */
    public static int[] intArrayOrNull(HashMap<String, String> parameter, String key) {

        if (parameter.getOrDefault(key, "---").equals("---")) {
            return null;
        } else {
            String s = parameter.get(key);
            String[] split = s.substring(1, s.length() - 1).split(",");
            return Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
        }

    }


    /**
     * 从总的参数map里面获取bool若没有返回true
     *
     * @param parameter
     * @param key
     * @return
     */
    public static Boolean boolOrTrue(HashMap<String, String> parameter, String key) {

        return parameter.getOrDefault(key, "True").equals("True");

    }

    /**
     * 从总的参数map里面获取bool若没有返回false
     *
     * @param parameter
     * @param key
     * @return
     */
    public static Boolean boolOrFalse(HashMap<String, String> parameter, String key) {

        return !parameter.getOrDefault(key, "False").equals("False");

    }

    public static Double douOrDefault(HashMap<String, String> parameter, String key, String value) {

        return Double.valueOf(parameter.getOrDefault(key, value));
    }

    public static Integer intOrDefault(HashMap<String, String> parameter, String key, String value) {

        return Integer.valueOf(parameter.getOrDefault(key, value));
    }

    //测试用
    public static void main(String[] args) throws Exception {

//        BatchOperator batchOp = getBatchOp("/user/experiment/tmp/0131b504-ac6c-11ea-a731-000c2960831c-110", Config.HADOOP_FSURI);
//        System.out.println(batchOp.count());

//        System.out.println("hdfs:/user/experiment/tmp/0131b504-ac6c-11ea-a731-000c2960831c-110".substring(5));
//        logger.info("aaaa");

//        String str = "[2,2,3]";
//        System.out.println(str.substring(1, str.length() - 1));
//
//        System.out.println(Double.valueOf("222"));


        String sss = "{\"input_data_path\": \"hdfs:/user/experiment/tmp/1e139b2c-d789-11ea-b72e-000c29c9d8a2-100\", \"output_data_path\": \"hdfs:/user/experiment/tmp/1e139b2c-d789-11ea-b72e-000c29c9d8a2-90\", \"clause\": \"fea_0,fea_1,fea_2\"}";
        System.out.println(Utils.json2map(sss));

        ParamsBase paramsBase = new ParamsBase();
        HashMap<String, String> map = new HashMap<>();
        Object put = map.put("1", "3");

        paramsBase.setParams(map);
        String s = pGson.toJson(map);
        System.out.println(s);
    }
}
