package client;

import java.util.Map;

/**
 * Created by renmingxu on 17-3-3.
 */
public class Response {
    private int status_code;
    private byte[] content;
    private String text;
    private String url;
    private Map<String, String> headers;
    public Response(int status_code, Map<String, String> headers, String url, String text, byte[] content) {
        this.status_code = status_code;
        this.headers = headers;
        this.url = url;
        this.text = text;
        this.content = content;
    }

    public String toString() {
        return "[" + status_code + "]" + " [" + url +"]";
    }

    public int getStatus_code() {
        return status_code;
    }

    public byte[] getContent() {
        return content;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
