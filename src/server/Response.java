package server;

import tool.ByteTools;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by renmingxu on 17-3-7.
 */
enum ResponseType {
    StringContent,
    FileContent,
}
public class Response {
    private static Map<Integer, String> statusMap;
    static {
        statusMap = new HashMap<>();
        statusMap.put(200, "OK");
        statusMap.put(400, "Bad Request");
        statusMap.put(403, "Forbidden");
        statusMap.put(404, "Not Found");
    }
    private int status_code;
    private ResponseType type;
    private String info;
    private Map<String, String> headers;
    private String version;
    private String contentStr;
    private byte[] contentByte;
    private int pos;
    private int limit;
    private boolean contentSeted;
    private FileInputStream fileInputStream;
    private String headersStr;

    public Response(int status_code, ResponseType type, String info) {
        this.status_code = status_code;
        this.type = type;
        this.version = "HTTP/1.1";
        this.contentSeted = false;
        this.headers = new HashMap<>();
        if (info == null) {
            if (this.statusMap.containsKey(status_code)) {
                this.info = this.statusMap.get(status_code);
            } else {
                this.info = "";
            }
        } else {
            this.info = info;
        }
        headers.put("Connection", "keep-alive");
        headers.put("Server", "Java_Nio_HttpServer");
    }
    public Response(int status_code, ResponseType type) {
        this(status_code, type, null);
    }

    public void setContent(String content) {
        this.headers.put("Content-Length", String.valueOf(content.getBytes().length));
        this.headersStr = getResponseHeaderString();
        this.contentStr = this.headersStr + content;
        try {
            this.contentByte = contentStr.getBytes("UTF8");
            this.limit = this.contentByte.length;
        } catch (UnsupportedEncodingException e) {
            this.contentByte = null;
        }
        this.pos = 0;
        this.contentSeted = true;
    }

    public void setContent(File file) {
        try {
            this.headers.put("Content-Length", String.valueOf(file.length()));
            this.headersStr = getResponseHeaderString();
            this.limit = headersStr.length();
            this.fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return;
        }
        this.contentSeted = true;
    }

    public void addHeaders(Map<String, String> h) {
        this.headers.putAll(h);
    }
    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public byte[] getContent(int l) {
        if (!contentSeted) {
            return null;
        }
        if (type == ResponseType.StringContent) {
            int len = 0;
            int start = pos;
            if (pos + l <= limit) {
                pos += l;
                len = l;
            } else {
                len = limit - pos;
                pos = limit;
            }
            return ByteTools.subByte(this.contentByte, start, len);
        } else if (type == ResponseType.FileContent) {
            if (pos >= limit) {
                return readFromFile(l);
            } else {
                int oldPos = pos;
                if (pos + l <= limit) {
                    pos += l;
                    return this.headersStr.substring(oldPos, l).getBytes();
                } else {
                    pos += l;
                    return ByteTools.addByte(this.headersStr.substring(oldPos, limit - oldPos).getBytes()
                            , readFromFile(l - (limit- oldPos)) );
                }
            }
        }
        return null;
    }
    private byte[] readFromFile(int l) {
        byte[] tmp = new byte[l];
        int len = 0;
        try {
            len = this.fileInputStream.read(tmp);
            if (len <= 0) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        return ByteTools.subByte(tmp, 0, len);
    }

    private String getResponseHeaderString() {
        return getResponseHeaderString(this.headers);
    }

    private String getResponseHeaderString(Map<String, String> responseHeaders) {
        StringBuffer buf = new StringBuffer();

        if (statusMap.containsKey(status_code)) {
            buf.append(version + " " + status_code + " " + statusMap.get(status_code));
        } else {
            buf.append(version + " " + status_code);
        }
        buf.append("\r\n");
        if (responseHeaders.containsKey("Server")) {
            buf.append("Server: " + responseHeaders.get("Server") + "\r\n");
        }
        Calendar cd = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE',' dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String dateStr = sdf.format(cd.getTime());
        buf.append("Date: " + dateStr + "\r\n");
        for (Map.Entry<String, String> entry: responseHeaders.entrySet()) {
            if (entry.getKey().equals("Server")) {
                continue;
            }
            buf.append(entry.getKey() + ": " + entry.getValue() + "\r\n");
        }
        buf.append("\r\n");
        return buf.toString();
    }
}
