package xin.chunming;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class PathUtil {
    public static String getAppPath() {
        String path = PathUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        File file = new File(path);
        // 如果是 jar 包运行，getParent() 得到的是 jar 所在目录
        // 如果是 IDE 运行，得到的是 target/classes 的上级目录
        return file.getParentFile().getAbsolutePath();
    }
}
