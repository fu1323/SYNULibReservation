package xin.chunming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.chunming.bean.Bean;

import java.io.*;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static String oldjobid;
    private static HashMap<String, String> seatsMap = new HashMap<>();

public static Bean b = null;
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
                    "seatid":[
                    
                      {"id0(必须从0开始)": "改成座位id1","comment":"座位号(方便人类阅读)(如A-123)"},
                      {"id1": "改成座位id1, 可灵活修改","comment":"4F A-123"},
                      {"id2": "123123123(数量不限,id后的数字为优先级)","comment":"3F B-567"}
                    ],
                       "autorenew": "false",
                       "stop_renew_hour": "16",
                       "stop_renew_minute": "00"
                       "fallback": "false"
                    }
                    """);
            bufferedWriter.flush();
            bufferedWriter.close();
            System.exit(0);
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

                jsonNode.get("seatid").forEach(s -> {
                    s.fields().forEachRemaining(new Consumer<Map.Entry<String, JsonNode>>() {
                        @Override
                        public void accept(Map.Entry<String, JsonNode> stringJsonNodeEntry) {

                            if (!stringJsonNodeEntry.getKey().equalsIgnoreCase("comment")) {
                                seatsMap.put(stringJsonNodeEntry.getKey(), String.valueOf(stringJsonNodeEntry.getValue()).replaceAll("\"",""));
                            }
                        }
                    });
                });

//                ArrayList<String> seats = jsnode2arrlist(jsonNode.get("seatid").);
                if (!renewFile.exists() && renew) {
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
                    //Reading renew.json
                    JsonNode jsonNode2 = objectMapper.readTree(stringBuilder2.toString());
                    String seatid = jsonNode2.get("seatid").asText();
                    String oldtime = jsonNode2.get("datetime").asText();
                    String trycount = jsonNode2.get("trycount").asText();

                    oldjobid = jsonNode2.get("jobid").asText();

                    bufferedReader2.close();
                    if (renew) {

                        HashMap<String, String> seatsMaptmp = new HashMap<>();
                        seatsMaptmp.put("id0", seatid);


                        b = new Bean(seatsMaptmp, jsonNode.get("unionid").asText(), Boolean.parseBoolean(jsonNode.get("autorenew").asText()), 0,
                                Integer.parseInt(jsonNode.get("stop_renew_hour").asText()),
                                Integer.parseInt(jsonNode.get("stop_renew_minute").asText()),
                                Boolean.parseBoolean(jsonNode.get("fallback").asText()),
                                null);
                        int token = Login.getToken(b, seatid, oldtime, oldjobid, trycount);
                        if (token==Login.OCCUPIED&&b.isFallback()){
                            System.out.println("座位续期被占,fallback尝试重新预约新座位!");
                                   logger.info("座位续期被占,fallback尝试重新预约新座位!");
                            normalbooking(jsonNode);
                        }
                    }
                }
                if (!renew) {
                    normalbooking(jsonNode);
                }


            }
        }
    }

    private static void normalbooking(JsonNode jsonNode) throws IOException {
       // Bean b;
        b = new Bean(seatsMap, jsonNode.get("unionid").asText(), Boolean.parseBoolean(jsonNode.get("autorenew").asText()), 0,
                Integer.parseInt(jsonNode.get("stop_renew_hour").asText()),
                Integer.parseInt(jsonNode.get("stop_renew_minute").asText()),
                Boolean.parseBoolean(jsonNode.get("fallback").asText()),
                null);
        int a = seatsMap.size();
        for (int i = 0; i < a; i++) {
            if (seatsMap.get("id" + i) == null) {
                a++;
                continue;
            }
            if (seatsMap.get("id" + i).isBlank() || seatsMap.get("id" + i).isEmpty()) {
                continue;
            }
           int code = Login.getToken(b, seatsMap.get("id" + i), null, oldjobid, String.valueOf(0));
           if (code == Login.LIBRARY_OR_USER_UNAVAILABLE || code == Login.SEAT_OK) {
               break;
           }
            System.out.println(seatsMap.get("id" + i));
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