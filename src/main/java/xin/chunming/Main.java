package xin.chunming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

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
                    { "unionid":"改成你自己的unionid","seatid":
                    [
                      {"id": "改成座位id1"},
                      {"id": "改成座位id2, 可灵活修改"}
                    ],
                       "autorenew": "false"
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
            if (stringBuilder.toString().contains("改成你自己的unionid")) {
                System.out.println("配置文件不合法 请修改");
                logger.info("配置文件不合法 请修改");

            } else {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(stringBuilder.toString());
                ArrayList<String> seats = jsnode2arrlist(jsonNode.get("seatid").toString());
                Bean b = null;
                if (!renewFile.exists()) {
                    System.out.println("续期配置文件不存在!");
                    logger.info("续期配置文件不存在!");
                }
                if (renew && renewFile.exists()) {
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
                    String oldjobid = jsonNode2.get("jobid").asText();

                    ArrayList<String> strings = new ArrayList<>();
                    strings.add(seatid);
                    bufferedReader2.close();

                    b = new Bean(strings, jsonNode.get("unionid").asText(), Boolean.parseBoolean(jsonNode.get("autorenew").asText()), 0, null);
                    Login.getToken(b, seatid, renew, oldtime, oldjobid);
                } else {

                    b = new Bean(seats, jsonNode.get("unionid").asText(), Boolean.parseBoolean(jsonNode.get("autorenew").asText()), 0, null);

                    for (String seat : seats) {
                        int code = Login.getToken(b, seat, renew, null, null);
                        if (code == Login.LIBRARY_OR_USER_UNAVAILABLE || code == Login.SEAT_OK) {
                            break;
                        }
                    }
                }


            }
        }
    }

    public static ArrayList<String> jsnode2arrlist(String jsnd) {
        ArrayList<String> arrayList = new ArrayList();
//    System.out.println(jsnd);
        for (String s : jsnd.split(",")) {
//        System.out.println(s);
            arrayList.add(
                    s.split(":")[1].strip().replace("\"", "").replace("{", "").replace("}", "").replace("[", "").replace("]", ""));
        }
        for (String s : arrayList) {
            System.out.println(s);
            logger.info(s);
        }
        return arrayList;
    }
}