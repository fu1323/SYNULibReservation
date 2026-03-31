package xin.chunming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static String oldjobid;
    private static HashMap<String, String> seatsMap = new HashMap<>();

    public static void main(String[] args) throws URISyntaxException, IOException {


        String appPath = PathUtil.getAppPath();
        System.setProperty("LOG_DIR", appPath + File.separator + "logs");

        String path = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        // 2. 处理路径（如果是 JAR 运行，获取其父目录）
        Login.setJarPath(path);
        File jarFile = new File(path);
        String jarDir = jarFile.getParentFile().getAbsolutePath();
//        System.out.println("jarDir = " + jarDir);
        // 3. 拼接配置文件的完整路径
        File configFile = new File(jarDir, "synulib_config.json");
        File renewFile = new File(jarDir, "renew.json");
        Login.setPath(jarDir);

        System.out.println("+++++++座位自动预约系统 for SYNU+++++++");
        boolean renew = false;
        for (String arg : args) {
            if (arg.equals("renew")) {
                renew = true;
                System.out.println("续期");
                logger.info("续期");
            }
        }

        if (!configFile.exists()) {
            System.out.println("配置文件不存在 已创建 请填写配置文件！");
            logger.info("配置文件不存在 已创建 请填写配置文件！");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(configFile));
            bufferedWriter.write("""
                    { "unionid":"改成你自己的unionid",
                    "seatid":{
                    
                      "id0(必须从0开始)": "改成座位id1",
                      "id1": "改成座位id1, 可灵活修改",
                      "id2": "123123123"
                    },
                       "autorenew": "false",
                       "stop_renew_hour": "16",
                       "stop_renew_minute": "00"
                    }
                    """);
            bufferedWriter.flush();
            bufferedWriter.close();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
//                System.out.println(line);
                stringBuilder.append(line);
            }
            if (stringBuilder.toString().contains("改成你自己的union_id")) {
                System.out.println("配置文件不合法 请修改");
                logger.info("配置文件不合法 请修改");

            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(stringBuilder.toString());
                int seatid1 = jsonNode.get("seatid").size();
                System.out.println(seatid1 + "个座位");
                logger.info(seatid1 + "个座位");

                jsonNode.get("seatid").forEachEntry(new BiConsumer<String, JsonNode>() {
                    @Override
                    public void accept(String key, JsonNode value) {
                        seatsMap.put(key, value.asText());
                    }
                });


//                ArrayList<String> seats = jsnode2arrlist(jsonNode.get("seatid").);
                Bean b = null;
                if (!renewFile.exists()) {
                    System.out.println("续期配置文件不存在!");
                    logger.info("续期配置文件不存在!");
                }
                if (renewFile.exists()) {
                    System.out.println("续期配置文件存在,读取中");
                    logger.info("续期配置文件存在,读取中");
                    StringBuilder stringBuilder2 = new StringBuilder();
                    BufferedReader bufferedReader2 = new BufferedReader(new FileReader(renewFile));
                    String line2;
                    while ((line2 = bufferedReader2.readLine()) != null) {
//                System.out.println(line);
                        stringBuilder2.append(line2);
                    }
                    JsonNode jsonNode2 = objectMapper.readTree(stringBuilder2.toString());
                    String seatid = jsonNode2.get("seatid").asText();
                    String oldtime = jsonNode2.get("datetime").asText();
                    oldjobid = jsonNode2.get("jobid").asText();

                    bufferedReader2.close();
                    if (renew) {

                        HashMap<String, String> seatsMaptmp = new HashMap<>();
                        seatsMaptmp.put("id0", seatid);


                        b=new Bean(seatsMaptmp,jsonNode.get("unionid").asText(),Boolean.parseBoolean(jsonNode.get("autorenew").asText()),0,
                        Integer.parseInt(jsonNode.get("stop_renew_hour").asText()),
                        Integer.parseInt(jsonNode.get("stop_renew_minute").asText()),null);
                        Login.getToken(b, seatid, oldtime, oldjobid);
                    }
                }
                if (!renew) {
                    b=new Bean(seatsMap,jsonNode.get("unionid").asText(),Boolean.parseBoolean(jsonNode.get("autorenew").asText()),0,
                            Integer.parseInt(jsonNode.get("stop_renew_hour").asText()),
                            Integer.parseInt(jsonNode.get("stop_renew_minute").asText()),null);
                    for (int i = 0; i < seatsMap.size(); i++) {
                        if (seatsMap.get("id" + i).isBlank() || seatsMap.get("id" + i).isBlank()) {
                            continue;
                        }
                        int code = Login.getToken(b, seatsMap.get("id" + i), null, oldjobid);
                        if (code == Login.LIBRARY_OR_USER_UNAVAILABLE || code == Login.SEAT_OK) {
                            break;
                        }
                    }
//                    for (String seat : seats) {
//
//                    }
                }


            }
        }
    }

//    public static ArrayList<String> jsnode2arrlist(String jsnd) {
//        ArrayList<String> arrayList = new ArrayList<>();
////    System.out.println(jsnd);
//        for (String s : jsnd.split(",")) {
////        System.out.println(s);
//            arrayList.add(
//                    s.split(":")[1].strip().replace("\"", "").replace("{", "").replace("}", "").replace("[", "").replace("]", ""));
//        }
//        StringBuilder ss = new StringBuilder();
//        for (String s : arrayList) {
//            ss.append(s).append(" ");
//        }
//        System.out.println("读取到座位id列表: " + ss);
//        logger.info("读取到座位id列表: " + ss);
//        return arrayList;
//    }
}