package xin.chunming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class DelayTryCount {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /** Keep retry state beside the executable/configuration, never inside the JAR itself. */
    private static File delayFile() {
        return new File(PathUtil.getAppPath(), "delay.json");
    }

    public static void writeout(int count) throws IOException {

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(delayFile(), false))) {
            bufferedWriter.write("{\"count\":\"" + count + "\"}");
        }


    }

    public static String readinCount() throws IOException {
        File file = delayFile();
        if (file.exists()) {
            outAndLog("Delay文件存在,进入续期时 候选座位都没有释放时延迟重试机制");
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            stringBuilder.append(bufferedReader.readLine());
            bufferedReader.close();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(stringBuilder.toString());
            JsonNode jsonNode1 = jsonNode.get("count");
            return jsonNode1.asText();

        } else return null;
    }

    public static void outAndLog(String s) {
        logger.info(s);
        System.out.println(s);
    }
}
