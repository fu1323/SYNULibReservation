package xin.chunming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class renewwriter {

    private static final Logger logger = LoggerFactory.getLogger(renewwriter.class);

    public static void configWriter(String renewjsonpath, String miniute, String seatid, String jarpath, String oldjobid, String oldtime,boolean weishifang,int weishifangcount) throws IOException, InterruptedException {
        LocalDateTime now = LocalDateTime.now();
        String jobid = null;
        if (!(oldjobid == null || oldjobid.equals(""))) {


            System.out.println(miniute);
            System.out.println("oldjobid: " + oldjobid);

            LocalDateTime localDateTime = now.plusMinutes(Integer.parseInt(miniute));

            ProcessBuilder processatq = new ProcessBuilder("atq");
            processatq.redirectErrorStream(true); // 合并错误流到标准输
            Process processa = processatq.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(processa.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {

                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 0 && parts[0].equals(oldjobid)) {
                        logger.info("移除oldjob: " + oldjobid);
                        System.out.println("移除oldjob: " + oldjobid);
                        ProcessBuilder processBuilder = new ProcessBuilder("atrm", oldjobid);
                        processBuilder.redirectErrorStream(true);
                        Process process = processBuilder.start();
                        process.waitFor();
                        String l;
//                   OutputStream outputStream = process.getOutputStream();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        while ((l = bufferedReader.readLine()) != null) {
                            System.out.println(l);
                            logger.info(l);
                        }
                        process.destroy();

                    }
                }
            }
        }

        System.out.println("尝试创建续期配置");
        logger.info("尝试创建续期配置");
        File parentFile = new File(jarpath).getParentFile();
       // ProcessBuilder processBuilder = new ProcessBuilder("at", "now", "+" + String.valueOf(Integer.parseInt(miniute) + 3), "minutes",">>",parentFile+File.separator+"atlog.log","2>&1");
        String command = "echo \"/usr/bin/java -jar " + jarpath + " renew >> " + parentFile + File.separator + "atlog.log 2>&1\" | at now + " + (Integer.parseInt(miniute) + 3) + " minutes";
        System.out.println(command);
        ProcessBuilder processBuilder = new ProcessBuilder("sh","-c",command);
        processBuilder.redirectErrorStream(true); // 合并错误流到标准输出

        Process p = processBuilder.start();
//        try (OutputStream os = p.getOutputStream()) {

//        try (OutputStream os = p.getOutputStream();
//             PrintWriter writer = new PrintWriter(os)) {
//            String command = "java -jar " + jarpath + " renew";
//            writer.println(command);
//            writer.flush();
//            // try-with-resources 会自动关闭 os，向 at 发送 EOF
//        }


        StringBuilder stringBuilder = new StringBuilder();
// 获取输入流并读取
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                System.out.println(line);
            }
        }
        Pattern pattern = Pattern.compile("job\\s+(\\d+)");
        Matcher matcher = pattern.matcher(stringBuilder.toString());
        String[] split = stringBuilder.toString().split(" at ");
        String nexttime = split[split.length - 1];

        if (matcher.find()) {
            jobid = matcher.group(1); // 输出: 5
        }

        int exitCode = p.waitFor();
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(renewjsonpath + File.separator + "renew.json")));

        bufferedWriter.write("{\n" +
                "  \"datetime\": \"" + nexttime + "\",\n" +
                "  \"seatid\": \"" + seatid + "\",\n" +
                "  \"jobid\": \"" + (jobid == null ? "" : jobid) + "\",\n" +
                "\"trycount\": \""+ String.valueOf(weishifang?++weishifangcount:0) + "\"\n"+
                "}");

        bufferedWriter.flush();
        bufferedWriter.close();
        if (exitCode == 0) {

            System.out.println("OK!");
            logger.info("OK!");
        }

    }
}
// 方案1：最简单，去掉无效的日志重定向
//String command = "echo \"/usr/bin/java -jar " + jarpath + " renew\" | at now + " + (Integer.parseInt(miniute) + 3) + " minutes";

// 方案2：让 at 任务本身记录日志

