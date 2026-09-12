package xin.chunming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class renewwriter {

    private static final Logger logger = LoggerFactory.getLogger(renewwriter.class);

    public static void configWriter(String renewjsonpath, String miniute, String seatid, String jarpath, String oldjobid, String oldtime,boolean weishifang,int weishifangcount,boolean fallback) throws IOException, InterruptedException {
        String jobid = null;
        if (!(oldjobid == null || oldjobid.equals(""))) {


            System.out.println(miniute);
            System.out.println("oldjobid: " + oldjobid);

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
        int delayMinutes = Integer.parseInt(miniute) + 3;
        String javaExecutable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String scheduledCommand = shellQuote(javaExecutable) + " -jar " + shellQuote(jarpath)
                + " renew >> " + shellQuote(new File(parentFile, "atlog.log").getPath()) + " 2>&1";
        System.out.println("将在 " + delayMinutes + " 分钟后执行续期任务");
        ProcessBuilder processBuilder = new ProcessBuilder("at", "now", "+", String.valueOf(delayMinutes), "minutes");
        processBuilder.redirectErrorStream(true); // 合并错误流到标准输出

        Process p = processBuilder.start();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()))) {
            writer.write(scheduledCommand);
            writer.newLine();
        }
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
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("创建续期任务失败: " + stringBuilder);
        }

        Pattern pattern = Pattern.compile("job\\s+(\\d+)");
        Matcher matcher = pattern.matcher(stringBuilder.toString());
        String[] split = stringBuilder.toString().split(" at ", 2);
        String nexttime = split.length == 2 ? split[1].trim() : "";

        if (matcher.find()) {
            jobid = matcher.group(1); // 输出: 5
        }

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(renewjsonpath + File.separator + "renew.json")));

        bufferedWriter.write("{\n" +
               // "\"fallback\":\""+fallback+"\", "+
                "  \"datetime\": \"" + nexttime + "\",\n" +
                "  \"seatid\": \"" + seatid + "\",\n" +
                "  \"jobid\": \"" + (jobid == null ? "" : jobid) + "\",\n" +
                "\"trycount\": \""+ String.valueOf(weishifang?++weishifangcount:0) + "\"\n"+
                "}");

        bufferedWriter.flush();
        bufferedWriter.close();
        System.out.println("OK!");
        logger.info("OK!");

    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\\"'\\\"'") + "'";
    }
}
// 方案1：最简单，去掉无效的日志重定向
//String command = "echo \"/usr/bin/java -jar " + jarpath + " renew\" | at now + " + (Integer.parseInt(miniute) + 3) + " minutes";

// 方案2：让 at 任务本身记录日志
