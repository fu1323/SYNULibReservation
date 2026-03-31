package xin.chunming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Login {
    private static OkHttpClient client() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }

        return new OkHttpClient.Builder()
                .sslSocketFactory(sc.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true)
                .build();

    }

    private static final Logger logger = LoggerFactory.getLogger(Login.class);

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static String jarPath;

    public static String getPath() {
        return path;
    }

    public static void setPath(String path) {
        Login.path = path;
    }

    public static String getJarPath() {
        return jarPath;
    }

    public static void setJarPath(String jarPath) {
        Login.jarPath = jarPath;
    }

    private static boolean before;
    private static String path;
    public static final int SEAT_ERROR = 1;
    public static final int LIBRARY_OR_USER_UNAVAILABLE = 2;
    public static final int SEAT_OK = 0;

    public static int getToken(Bean bean, String seatid, String oldtime, String jobid) throws IOException {
// 1. 使用 HttpUrl.Builder 自动处理参数编码
        Request request = new Request.Builder()
                .url("https://ic.synu.edu.cn/ic-web/phoneSeatReserve/login")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("Referer", "https://ic.synu.edu.cn/scancode.html")
                .header("Sec-Fetch-Dest", "empty")
                .header("lan", "1")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "cors")
                .header("Connection", "keep-alive")
                .header("Origin", "https://ic.synu.edu.cn")
                .method("POST", RequestBody.create(MediaType.parse("application/json;charset=UTF-8"),
                        "{\"devSn\":\"" + seatid + "\",\"unionId\":\"" + bean.getUnionId() + "\",\"type\":\"1\",\"bind\":0}"))
                .build();

        try (Response response = client().newCall(request).execute()) {
            // 无论成功还是 400/500，都可以通过 response.body() 获取内容
            String s = response.body() != null ? response.body().string() : "";
            System.out.println(s);
            logger.info(s);
            if (response.isSuccessful()) {
                final String[] iccookie = new String[1];
                List<String> values = response.headers().values("Set-Cookie");
                values.forEach(cookie -> {
                    if (cookie.startsWith("ic-cookie")) {
                        iccookie[0] = cookie;
                    }
                });


                //s = s.replace(loginBean.getCallback() + "(", "").replace(")", "");
                JsonNode jsonNode = objectMapper.readTree(s);
                String code = jsonNode.get("code").asText();
                String message = jsonNode.get("message").asText();
                if (code.equals("1")) {
                    System.out.println("错误: getToken" + message);
                    logger.info("错误: getToken" + message);
                    if (message.contains("设备不在开放时间") || message.contains("即将闭馆") || message.contains("您预约的不是当前设备") || (message.contains("请去") && message.contains("处扫描二维码"))) {
                        return LIBRARY_OR_USER_UNAVAILABLE;
                    } else return SEAT_ERROR;
                }
                if (code.equals("0") && jsonNode.get("data") != null) {
                    String token = jsonNode.get("data").get("token").asText();
                    bean.setToken(token);
                    System.out.println("服务器返回当前token: " + token);
                    logger.info("服务器返回当前token: " + token);
                    //可预约:reserveInfo!=null(自己使用,同时)

                    if (!jsonNode.path("data").path("duration").isMissingNode()) {

                        System.out.println("预约未结束 还剩" + jsonNode.get("data").get("duration"));
                        logger.info("预约未结束 还剩" + jsonNode.get("data").get("duration"));
                        if (bean.isRenew()) {
                            String hour = jsonNode.get("data").get("duration").asText().split("时")[0].strip();
                            String minute = jsonNode.get("data").get("duration").asText().split("时")[1].split("分")[0].strip();
                            int durationMinute = Integer.parseInt(hour) * 60 + Integer.parseInt(minute) + 1;
                            before = LocalTime.now().isBefore(LocalTime.of(bean.getLastRenewHour(), bean.getLastRenewMinute()));

                            if (before) {
                                renewwriter.configWriter(path, String.valueOf(durationMinute), seatid, jarPath, jobid, oldtime);

                            } else {
                                System.out.println("时间晚于" + bean.getLastRenewHour() + "点" + bean.getLastRenewMinute() + "分 ,停止安排计划续期!");
                                logger.info("时间晚于" + bean.getLastRenewHour() + "点" + bean.getLastRenewMinute() + "分 ,停止安排计划续期!");


                            }


                        }
                        return LIBRARY_OR_USER_UNAVAILABLE;
                    } else {


                        System.out.println("尝试座位: " + jsonNode.get("data").get("devInfo").get(0).get("devName").asText());
                        logger.info("尝试座位: " + jsonNode.get("data").get("devInfo").get(0).get("devName").asText());
                        return reservationCheck(bean, seatid, iccookie[0], jobid, oldtime);
                    }


                } else {
                    System.out.println("发生问题: getToken" + s);
                    logger.info("发生问题: getToken" + s);
                    return SEAT_ERROR;
                }
            } else {
                System.out.println("发生问题: getToken" + s);
                logger.info("发生问题: getToken" + s);
                return SEAT_ERROR;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


    public static int reservationCheck(Bean bean, String seatid, String cookie, String jobid, String oldtime) throws IOException {
        Request request = new Request.Builder()
                .url("https://ic.synu.edu.cn/ic-web/phoneSeatReserve/duration")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Referer", "https://ic.synu.edu.cn/scancode.html")
                .header("Sec-Fetch-Dest", "empty")
                .header("lan", "1")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "cors")
                .header("Connection", "keep-alive")
                .header("Cookie", cookie)
                .header("token", bean.getToken())
                .build();
        try (Response response = client().newCall(request).execute()) {
            // 无论成功还是 400/500，都可以通过 response.body() 获取内容
            String s = response.body() != null ? response.body().string() : "";
            System.out.println(s);
            logger.info(s);
            if (response.isSuccessful()) {
                //s = s.replace(loginBean.getCallback() + "(", "").replace(")", "");
                JsonNode jsonNode = objectMapper.readTree(s);
                String code = jsonNode.get("code").asText();
                String message = jsonNode.get("message").asText();
                if (code.equals("1")) {
                    System.out.println("错误: /phoneSeatReserve/duration " + message);
                    logger.info("错误: /phoneSeatReserve/duration " + message);
                    if (message.contains("设备不在开放时间") || message.contains("即将闭馆") || message.contains("您预约的不是当前设备") || (message.contains("请去") && message.contains("处扫描二维码"))) {
                        return LIBRARY_OR_USER_UNAVAILABLE;
                    }

                    /*您预约的不是当前设备*/

                }
                if (code.equals("0") && jsonNode.get("data") != null) {
                    String maxMiniute = jsonNode.get("data").get("max").asText();
                    if (Integer.parseInt(maxMiniute) < 300) {
                        if (LocalDateTime.now().getHour() > 15) {//15点之后 maxMinute<300正常
                            bean.setMiniute(Integer.parseInt(maxMiniute));
                            return booking(Integer.parseInt(maxMiniute), bean, seatid, cookie, jobid, oldtime);

                        } else {
                            System.out.println("座位异常! 更换中 " + s);
                            logger.info("座位异常! 更换中 " + s);
                            return SEAT_ERROR;
                        }
                    } else {
                        bean.setMiniute(300);
                        return booking(300, bean, seatid, cookie, jobid, oldtime);
                    }

                } else {
                    System.out.println("发生问题: /phoneSeatReserve/duration " + s);
                    logger.info("发生问题: /phoneSeatReserve/duration " + s);
                    return SEAT_ERROR;
                }
            } else {
                System.out.println("发生问题: /phoneSeatReserve/duration " + s);
                logger.info("发生问题: /phoneSeatReserve/duration " + s);
                return SEAT_ERROR;
            }
        }
    }

    public static int booking(int times, Bean bean, String seatid, String cookie, String jobid, String oldtime) throws IOException {
        Request request = new Request.Builder()
                .url("https://ic.synu.edu.cn/ic-web/phoneSeatReserve/reserve")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Referer", "https://ic.synu.edu.cn/scancode.html")
                .header("Sec-Fetch-Dest", "empty")
                .header("lan", "1")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "cors")
                .header("Cookie", cookie)
                .header("Connection", "keep-alive")
                .header("token", bean.getToken())
                .method("POST", RequestBody.create(MediaType.parse("application/json;charset=UTF-8"),
                        "{\"duration\":" + times + "}"))
                .build();
        try (Response response = client().newCall(request).execute()) {
            // 无论成功还是 400/500，都可以通过 response.body() 获取内容
            String s = response.body() != null ? response.body().string() : "";
            System.out.println(s);
            logger.info(s);
            if (response.isSuccessful()) {
                //s = s.replace(loginBean.getCallback() + "(", "").replace(")", "");
                JsonNode jsonNode = objectMapper.readTree(s);
                String code = jsonNode.get("code").asText();
                String message = jsonNode.get("message").asText();
                if (code.equals("1")) {
                    System.out.println("错误: phoneSeatReserve/duration " + message);
                    logger.info("错误: phoneSeatReserve/duration " + message);
                    return SEAT_ERROR;

                }
                if (code.equals("0")) {
                    String msg = jsonNode.get("message").asText();
                    if (msg.contains("操作成功")) {
                        System.out.println("订座/续订 操作成功!");
                        logger.info("订座/续订 操作成功!");


                        before = LocalTime.now().isBefore(LocalTime.of(bean.getLastRenewHour(), bean.getLastRenewMinute()));


                        if (bean.isRenew() && before) {//本次续期/订座 只有小于下午3点 才可配置自动续期
                            renewwriter.configWriter(path, String.valueOf(times), seatid, jarPath, jobid, oldtime);
                            return SEAT_OK;

                        } else {
                            System.out.println("时间晚于" + bean.getLastRenewHour() + "点" + bean.getLastRenewMinute() + "分 ,停止安排计划续期!");
                            logger.info("时间晚于" + bean.getLastRenewHour() + "点" + bean.getLastRenewMinute() + "分 ,停止安排计划续期!");

                            return SEAT_OK;

                        }


                    } else {
                        System.out.println("发生问题: phoneSeatReserve/duration " + message);
                        logger.info("发生问题: phoneSeatReserve/duration " + message);
                        return SEAT_ERROR;

                    }

                } else {
                    System.out.println("发生问题: phoneSeatReserve/duration " + s);
                    logger.info("发生问题: phoneSeatReserve/duration " + s);
                    return SEAT_ERROR;

                }
            } else {
                System.out.println("发生问题: phoneSeatReserve/duration " + s);
                logger.info("发生问题: phoneSeatReserve/duration " + s);
                return SEAT_ERROR;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
