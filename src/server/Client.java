package server;



import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.logging.Level;

/**
 * Created by renmingxu on 2017/3/6.
 */
public class Client {

    public static final int WAITING_NEW_REQ = 0;
    public static final int UNFINISHED = 1;
    public static final int GET = 2;
    public static final int POST = 4;
    public static final int REQUEST = 16;
    public static final int RESPONSE = 32;
    public static final int UNFINISHED_GET_REQUEST =    UNFINISHED | GET | REQUEST;
    public static final int UNFINISHED_GET_RESPONSE =   UNFINISHED | GET | RESPONSE;
    public static final int UNFINISHED_POST_REQUEST =   UNFINISHED | POST| REQUEST;
    public static final int UNFINISHED_POST_RESPONSE =  UNFINISHED | POST| RESPONSE;

    private SocketChannel socketChannel;
    private SocketAddress remoteAddress;
    private HttpServer httpServer;
    private SelectionKey selectionKey;
    private Selector selector;
    private int status;

    private long totalSend;
    private long totalRecv;
    private ByteBuffer bufferUnFinished;
    private byte[] byteUnFininshed;

    private boolean needToSend;
    private ByteBuffer bufferToSend;
    private Response responseToSend;

    public Client(SocketChannel socketChannel, SelectionKey selectionKey,HttpServer httpServer) {
        this.socketChannel = socketChannel;
        try {
            this.remoteAddress = socketChannel.getRemoteAddress();
        } catch (IOException ignored) {
            this.remoteAddress = null;
        }
        this.selectionKey = selectionKey;
        this.selector = selectionKey.selector();
        this.httpServer = httpServer;
        this.bufferToSend = ByteBuffer.allocate(0);
        this.status = Client.WAITING_NEW_REQ;
    }

    public boolean eventHandler(SelectionKey key) {
        if (this.selectionKey.isWritable() && this.needToSend) {
            return this.toSend();
        } else if (this.selectionKey.isReadable()) {
            return this.toRecv();
        }
        return true;
    }

    private boolean toRecv() {
        ByteBuffer buf = ByteBuffer.allocate(102400);
        int len;
        try {
            len = this.socketChannel.read(buf);
        } catch (IOException e) {
            return false;
        }
        if (len < 0) {
            return false;
        }
        buf.flip();
        if (this.status == WAITING_NEW_REQ) {
            byte[] b = new byte[buf.remaining()];
            buf.get(b, 0, b.length);
            this.status = requestHandler(b);
            if ((this.status & UNFINISHED & REQUEST) != 0) {
                this.byteUnFininshed = b;
            }
        } else if ((this.status & UNFINISHED & REQUEST ) != 0) {
            byte[] b = new byte[this.byteUnFininshed.length + buf.remaining()];
            System.arraycopy(this.byteUnFininshed, 0, b, 0, this.byteUnFininshed.length);
            buf.get(b, this.byteUnFininshed.length, b.length - this.byteUnFininshed.length);
            this.status = requestHandler(b);
            if ((this.status & UNFINISHED & REQUEST) != 0) {
                this.byteUnFininshed = b;
            } else {
                this.byteUnFininshed = null;
            }
        } else if ((this.status & UNFINISHED & RESPONSE) != 0) {

        }
        return true;
    }

    private int requestHandler(byte[] data) {
        String dataStr = new String(data);
        if (!dataStr.contains(" ")) {
            return UNFINISHED | REQUEST;
        }
        if (!dataStr.contains("\r\n\r\n")) {
            if (dataStr.startsWith("GET")) {
                return UNFINISHED | GET | REQUEST;
            }
            if (dataStr.startsWith("POST")) {
                return UNFINISHED | POST | REQUEST;
            }
        }
        Request request = new Request();
        boolean parseResult = request.parse(data);
        httpServer.logger.log(Level.INFO, remoteAddress + " : " +request.toString());
        if (parseResult) {
            if (request.getType() == RequestType.GET_REQUEST) {
                return getRequestHandler(request);
            } else if (request.getType() == RequestType.POST_REQUEST) {
                return postRequestHandler(request);
            } else {
                return unsupportedRequestHandler(request);
            }
        } else {
            return badRequestHandler();
        }
    }

    private int badRequestHandler() {

        return UNFINISHED | RESPONSE;
    }

    private int unsupportedRequestHandler(Request request) {

        return UNFINISHED| RESPONSE;
    }

    private int postRequestHandler(Request request) {
        if (!request.isFinished()) {
            return UNFINISHED | POST | REQUEST;
        }

        return UNFINISHED | POST | RESPONSE;
    }

    private int getRequestHandler(Request request) {
        if (!request.getHeaders().containsKey("Host")) {
            badRequestHandler();
        }
        File file = new File(httpServer.rootDirectory + request.getFilename());
        Response response = null;
        if (!file.isDirectory()  && file.getAbsolutePath().startsWith(httpServer.rootDirectory)) {
            if (!file.exists()) {
                response = new Response(404, ResponseType.StringContent);
                response.addHeader("Content-Type", "text/html; charset=UTF-8");
                response.setContent("File Not Found\r\n");
            } else {
                response = new Response(200, ResponseType.FileContent);
                String contentType = getContentType(request.getFilename());
                if (contentType != null) {
                    response.addHeader("Content-Type", contentType);
                }
                response.setContent(file);
            }
        } else {
            if (!request.getFilename().endsWith("/")) {
                response = new Response(302, ResponseType.StringContent);
                response.addHeader("Location", request.getFilename() + "/");
                response.setContent("");
            } else {
                File indexFile = new File(httpServer.rootDirectory + request.getFilename() + "/" + httpServer.indexFilename);
                if (indexFile.exists() && !indexFile.isDirectory()) {
                    response = new Response(200, ResponseType.FileContent);
                    String contentType = getContentType(httpServer.indexFilename);
                    if (contentType != null) {
                        response.addHeader("Content-Type", contentType);
                    }
                    response.setContent(indexFile);
                } else if (indexFile.isDirectory()){
                    response = new Response(403, ResponseType.StringContent);
                    response.addHeader("Content-Type", "text/html; charset=UTF-8");
                    response.setContent("Forbidden\r\n");
                } else {
                    response = new Response(200, ResponseType.StringContent);
                    response.addHeader("Content-Type", "text/html; charset=UTF-8");
                    StringBuffer buf = new StringBuffer();
                    buf.append("<html>\n")
                            .append("<head><title>Index of ")
                            .append(request.getFilename())
                            .append(" </title></head>\n")
                            .append("<body bgcolor=\"white\">\n")
                            .append("<h1>Index of ")
                            .append(request.getFilename())
                            .append("</h1><hr><pre><a href=\"../\">../</a>\n");
                    File[] list = file.listFiles();

                    int longest = 0;
                    for (File f: list) {
                        if (f.getName().length() >= longest) {
                            longest = f.getName().length();
                        }
                    }
                    String padding = "                                                                                                                                                                                 ";

                    for (File f: list) {
                        if (f.isDirectory()){
                            buf.append("<a href=\"");
                            try {
                                buf.append(URLEncoder.encode(f.getName(), "UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                            }
                            buf.append("/\">");
                            buf.append(f.getName());
                            buf.append("/</a>");
                            buf.append(padding.substring(0, longest + 29 - f.getName().length()));
                            buf.append(new Date(f.lastModified()));
                            buf.append("         ");
                            buf.append(f.length());
                            buf.append("\n");
                        }
                    }
                    for (File f: list) {
                        if (!f.isDirectory()) {
                            buf.append("<a href=\"");
                            try {
                                buf.append(URLEncoder.encode(f.getName(), "UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                            }
                            buf.append("\">");
                            buf.append(f.getName());
                            buf.append("</a>");
                            buf.append(padding.substring(0, longest + 30 - f.getName().length()));
                            buf.append(new Date(f.lastModified()));
                            buf.append("         ");
                            buf.append(f.length());
                            buf.append("\n");
                        }
                    }
                    buf.append("</pre><hr></body>\n</html>\n");
                    response.setContent(buf.toString());
                }
            }
        }
        send(response);
        return UNFINISHED | GET | RESPONSE;
    }

    private String getContentType(String filename) {
        String[] filenameTmp = filename.split("\\.");
        String filenameEnd = filenameTmp[filenameTmp.length - 1];
        if (httpServer.mimeMap.containsKey(filenameEnd)) {
            return httpServer.mimeMap.get(filenameEnd) + "; charset=UTF-8";
        }
        return null;
    }

    private boolean send(Response response) {
        this.responseToSend = response;
        byte[] data = this.responseToSend.getContent(1024000);
        if (data != null && data.length != 0) {
            send(data);
            return true;
        } else {
            this.responseToSend = null;
            this.status = WAITING_NEW_REQ;
            return false;
        }
    }

    private boolean toSend() {
        int len = 0;
        try {
            len = this.socketChannel.write(this.bufferToSend);
        } catch (IOException e) {
            return false;
        }
        if (len < 0) {
            return false;
        }
        if (responseToSend != null) {
            send(this.responseToSend);
        }
        if (this.bufferToSend.remaining() == 0) {
            this.needToSend = false;
            this.bufferToSend = ByteBuffer.allocate(0);
            try {
                this.selectionKey = this.socketChannel.register(this.selector,
                        SelectionKey.OP_READ);
            } catch (ClosedChannelException e) {
                return false;
            }
        }
        return true;
    }

    private void send(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length + this.bufferToSend.remaining());
        byteBuffer.put(this.bufferToSend);
        byteBuffer.put(data);
        byteBuffer.flip();
        this.bufferToSend = byteBuffer;
        this.needToSend = true;
        try {
            this.selectionKey = this.socketChannel.register(this.selector,
                    SelectionKey.OP_READ| SelectionKey.OP_WRITE);
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }

    public void close() {

    }
}
