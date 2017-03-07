package client;

import jdk.internal.util.xml.impl.Input;
import sun.reflect.annotation.ExceptionProxy;
import tool.ByteTools;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;

/**
 * Created by renmingxu on 17-3-3.
 */
public class HttpClient {
    private static Map<String, String> defaultHeaders;
    private static String versionString = "HTTP/1.1";
    private static String defaultCharsetName = "UTF8";
    static {
        defaultHeaders = new HashMap<String, String>();
        defaultHeaders.put("User-Agent", "HttpClient");
        defaultHeaders.put("Accept", "*/*");
    }
    private String url;
    private boolean keepAlive;
    private Map<String, String> headers;

    private boolean headersChanged;
    private boolean urlAnalysis;
    private boolean connected;
    private String host;
    private String uri;
    private int port;
    private String charsetName;
    private Socket socket;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;



    public HttpClient(String url) {
        this(url, defaultHeaders);
    }

    public HttpClient(String url, Map<String, String> headers) {
        this(url, headers, true);
    }

    public HttpClient(String url, Map<String, String> headers, boolean keepAlive) {
        this.url = url;
        this.headers = headers;
        this.urlAnalysis = false;
        this.headersChanged = false;
        this.connected = false;
        this.keepAlive = keepAlive;
        this.charsetName = HttpClient.defaultCharsetName;
    }

    private boolean init() {
        if (!this.analysisUrl())
            return false;
        if (!this.changeHeaders())
            return false;
        if (!this.connect())
            return false;
        return true;
    }

    private boolean changeHeaders(){
        if (this.headersChanged){
            return true;
        }
        if (this.keepAlive) {
            this.headers.put("Connection", "keep-alive");
        }
        this.headers.put("Host" , this.host);
        this.headersChanged = true;
        return true;
    }

    private boolean analysisUrl() {
        if (urlAnalysis) {
            return true;
        }
        try {
            if (!"http://".equals(this.url.substring(0, 7))) {
                return false;
            }
            String tmp = this.url.substring(7).split("/")[0];
            if (tmp.contains(":")) {
                this.host = tmp.split(":")[0];
                this.port = Integer.valueOf(tmp.split(":")[1]);
            } else {
                this.host = tmp;
                this.port = 80;
            }
            this.uri = this.url.substring(7 + tmp.length());
            if (this.host != null && this.url != null) {
                this.urlAnalysis = true;
                return true;
            }
        }catch (Exception e) {
            return false;
        }
        return false;
    }

    private boolean connect() {
        if (this.connected) {
            return true;
        }
        try {
            this.socket = new Socket(this.host, this.port);
            this.socketInputStream = socket.getInputStream();
            this.socketOutputStream = socket.getOutputStream();
        } catch (IOException e) {
            return false;
        }
        this.connected = true;
        return true;
    }

    private synchronized byte[] readline() {
        byte[] b = new byte[1024000];
        try {
            int i = 0;
            byte tmp;
            for (;; i++) {
                tmp = (byte) socketInputStream.read();
                if (tmp == -1) {
                    return null;
                }
                b[i] = tmp;
                if (i >= 1) {
                    if (b[i - 1] == 13 && b[i] == 10){
                        break;
                    }
                }
            }
            if (i >= 1) {
                return ByteTools.subByte(b, 0, i - 1);
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private synchronized byte[] read(int len) {
        byte[] b = new byte[len];
        try {
            for (int i = 0; i < len; i++) {
                b[i] = (byte) socketInputStream.read();
            }
            return b;
        } catch (IOException e){
            return null;
        }
    }

    private synchronized boolean write(String data) {
        if (!this.connected) {
            this.init();
        }
        try {
            socketOutputStream.write(data.getBytes());
            return true;
        } catch (IOException e) {
            this.connected = false;
            return false;
        }
    }

    private Response readResponse() {
        int status_code = 0;
        String info;
        Map<String, String> responseHeaders = new HashMap<String, String>();
        String text = "";
        byte[] content = new byte[0];
        byte [] tmpb = this.readline();
        if (tmpb == null) {
            return null;
        }
        String firstLine = new String(tmpb);
        String[] firstLineList = firstLine.split(" ");
        if (firstLineList.length >= 3) {
            status_code = Integer.valueOf(firstLineList[1]);
        } else {
            return null;
        }
        while(true) {
            String tmp = new String(this.readline());
            if (tmp == null) {
                return null;
            }
            if ("".equals(tmp)) {
                break;
            }
            String[] l = tmp.split(":");
            responseHeaders.put(l[0], tmp.substring(l[0].length() + 1).trim());
        }
        if (responseHeaders.containsKey("Content-Type")) {
            String c = responseHeaders.get("Content-Type");
            if (c.contains("charset")) {
                if (c.split("=").length == 2)
                 this.charsetName = c.split("=")[1];
            }
        }
        if (responseHeaders.containsKey("Content-Length")) {
            int contentLength = Integer.valueOf(responseHeaders.get("Content-Length").trim());
            System.out.println("intlen:" + contentLength + "strlen" + responseHeaders.get("Content-Length"));

            content = this.read(contentLength);
            try {
                content = text.getBytes(charsetName);
            } catch (UnsupportedEncodingException e) {
                content = null;
            }
            Response response = new Response(status_code, responseHeaders, this.url, text, content);
            return response;
        }else if (responseHeaders.containsKey("Transfer-Encoding")) {
            if (responseHeaders.get("Transfer-Encoding").contains("chunked")) {
                while(true) {
                    String l = new String(readline());
                    if ("".equals(l)) {
                        continue;
                    }
                    int len = Integer.parseInt(l, 16);
                    if (len == 0) {
                        readline();
                        break;
                    }
                    byte[] tmp = this.read(len);
                    content = ByteTools.addByte(content, tmp);
                }
                try {
                    text = new String(content, this.charsetName);
                } catch (UnsupportedEncodingException e) {
                    text = null;
                }
                Response response = new Response(status_code, responseHeaders, this.url, text, content);
                return response;
            }
        }
        return null;
    }

    private String generateHeadersString() {
        return generateHeadersString(this.headers);
    }

    private String generateHeadersString(Map<String, String> headers) {
        String headerString = "";
        headerString += "Host: " + headers.get("Host") + "\r\n";
        for (Map.Entry<String, String> entry:headers.entrySet()) {
            if ("Host".equals(entry.getKey()))
                continue;
            headerString += entry.getKey() + ": " + entry.getValue() + "\r\n";
        }
        return headerString;
    }

    public Response get() {
        if (!this.init()){
            return null;
        }
        String getCmd = "GET " + this.uri + " " + HttpClient.versionString + "\r\n";
        String reqString = getCmd + generateHeadersString() + "\r\n";
        write(reqString);
        return readResponse();
    }

    public Response post(String data) {
        if (!this.init()){
            return null;
        }
        String getCmd = "POST " + this.uri + " " + HttpClient.versionString + "\r\n";
        Map<String, String> headers = new HashMap<>(this.headers);
        headers.put("Content-Length" , " " + data.getBytes().length);
        String reqString = getCmd + generateHeadersString(headers) + "\r\n" + data;
        write(reqString);
        return readResponse();
    }

    public Response post(Map<String, String> data) {
        if (!this.init()){
            return null;
        }
        String dataString = "";
        for (Map.Entry<String, String> entry: data.entrySet()){
            try {
                dataString += URLEncoder.encode(entry.getKey(),"UTf-8") + "=" + URLEncoder.encode(entry.getValue(),"UTF-8") + "&";
            } catch (UnsupportedEncodingException e) {
            }
        }
        String getCmd = "POST " + this.uri + " " + HttpClient.versionString + "\r\n";
        Map<String, String> headers = new HashMap<>(this.headers);
        headers.put("Content-Length" , " " + dataString.getBytes().length);
        headers.put("Content-Type", " application/x-www-form-urlencoded");
        String reqString = getCmd + generateHeadersString(headers) + "\r\n" + dataString;
        write(reqString);
        return readResponse();
    }

    public void close() {
        try {
            this.socket.close();
        } catch (IOException e) {
        }
    }
}
