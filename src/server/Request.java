package server;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.net.URLDecoder;


/**
 * Created by renmingxu on 17-3-7.
 */

enum RequestType{
    GET_REQUEST,
    POST_REQUEST,
    UNSUPPORTED_REQUEST,
        };
public class Request {
    private Map<String, String> headers;
    private RequestType type;
    private String typeStr;
    private String content;
    private String uri;
    private String filename;
    private Map<String, String> paras;
    private String version;
    private boolean finished;
    public Request(){
        this.headers = new HashMap<>();
        this.headers = new HashMap<>();
        this.content = null;
        this.finished = false;
    }
    public boolean parse(byte[] data) {
        String dataStr = new String(data);
        typeStr = dataStr.split(" ")[0];
        switch (typeStr) {
            case "GET":
                this.type = RequestType.GET_REQUEST;
                break;
            case "POST":
                this.type = RequestType.POST_REQUEST;
                break;
            default:
                this.type = RequestType.UNSUPPORTED_REQUEST;
                return true;
        }
        this.uri = dataStr.split(" ")[1];
        String orginFilename = this.uri.split("[?]")[0];
        try {
            this.filename = URLDecoder.decode(orginFilename, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            this.filename = orginFilename;
        }
        /*
        if (this.uri.length() >= orginFilename.length() + 1) {
            String parasStr = this.uri.substring(orginFilename.length() + 1);
            for (String s : parasStr.split("&")) {
                try {
                    this.paras.put( URLDecoder.decode(s.split("=")[0], "UTF-8"),
                            URLDecoder.decode(s.substring(s.split("=")[0].length() + 1), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
        */
        this.version = dataStr.split("\r\n")[0].split(" ")[2];
        String headerStr = dataStr.substring(dataStr.indexOf(dataStr.split("\r\n")[0]))
                .split("\r\n\r\n")[0];
        for (String str: headerStr.split("\r\n")) {
            if (str.contains(":")) {
                String[] l = str.split(":");
                headers.put(l[0], str.substring(l[0].length() + 1).trim());
            }
        }
        Set<String> keySet = headers.keySet();
        for (String key :
                keySet) {
            if (key.equalsIgnoreCase("Content-Length")) {
                try {
                    int length = Integer.parseInt(headers.get(key));
                    if (dataStr.split("\r\n\r\n")[0].length() + 4 + length != dataStr.length()) {
                        this.finished = false;
                        return true;
                    }
                    this.content = dataStr.substring(dataStr.split("\r\n\r\n")[0].length() + 4);
                    this.finished = true;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        this.finished = true;
        return true;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getParas() {
        return paras;
    }

    public String getFilename() {
        return filename;
    }

    public String getContent() {
        return content;
    }

    public RequestType getType() {
        return type;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getUri() {
        return uri;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Request " + typeStr + " : " + filename;
    }
}
