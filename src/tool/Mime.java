package tool;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by renmingxu on 17-3-7.
 */
public class Mime {
    public static Map<String, String> getMimeMap(String filename) {
        Map<String, String> m = new HashMap<>();
        File file = new File(filename);
        if (file.exists()) {
            try {
                int len = (int) file.length();
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] data = new byte[len];
                int pos = 0;
                while ( pos < len) {
                    int l = fileInputStream.read(data, pos, len - pos);
                    if (l <= 0) {
                        return null;
                    }
                    pos += l;
                }
                String dataStr = new String(data);
                for (String str: dataStr.split("(\r)?\n")) {
                    str = str.trim();
                    if (str.endsWith(";")) {
                        String t = str.split("( )+")[0];
                        String end = str.substring(str.split("( )+")[0].length()).trim().split(";")[0];
                        for (String e:end.split("( )+")) {
                            m.put(e,t);
                        }
                    }
                }
                fileInputStream.close();
                return m;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
