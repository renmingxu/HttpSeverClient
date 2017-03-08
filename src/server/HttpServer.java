package server;

import tool.Mime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by renmingxu on 2017/3/6.
 */
public class HttpServer {
    private String serverAddress;
    private Map<String, String> config;
    private int port;
    String rootDirectory;
    String indexFilename;
    Logger logger;
    Map<String, String> mimeMap;

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private Map<SocketChannel, Client> channelClientMap;

    public HttpServer(String configFilename) {
        this.getConfig(configFilename);
        this.port = Integer.parseInt(config.get("port"));
        this.serverAddress = config.get("address");
        this.rootDirectory = config.get("root");
        this.indexFilename = config.get("index");
        this.channelClientMap = new HashMap<>();
        this.mimeMap = Mime.getMimeMap(config.get("mime"));
        this.logger = Logger.getLogger("HttpServer");
    }

    private boolean getConfig(String configFilename) {
        config = new HashMap<>();
        File file = new File(configFilename);
            if (file.exists()) {
            try {
                int len = (int) file.length();
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] data = new byte[len];
                int pos = 0;
                while ( pos < len) {
                    int l = fileInputStream.read(data, pos, len - pos);
                    if (l <= 0) {
                        return false;
                    }
                    pos += l;
                }
                String dataStr = new String(data);
                for (String str: dataStr.split("(\r)?\n")) {
                    str = str.trim();
                    if (str.endsWith(";")) {
                        str = str.split(";")[0];
                        String key = str.split(":")[0].trim();
                        String value = str.substring(str.split(" ")[0].length()).trim();
                        config.put(key, value);
                    }
                }
                fileInputStream.close();
            } catch (Exception ignored) {
                return false;
            }
        }
        return true;
    }

    public void start() {
        try {
            this.selector = Selector.open();
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.bind(new InetSocketAddress(this.serverAddress, port));
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        listen();
    }

    private void listen() {
        while (true) {
            long start = System.nanoTime();
            int n = 0;
            try {
                n = this.selector.select(5000);
            } catch (IOException e) {
                continue;
            }
            long end = System.nanoTime();
            if (n == 0) {
                logger.log(Level.INFO,"Client num:" + this.channelClientMap.size());
                System.gc();
                continue;
            }
            Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
            Iterator<SelectionKey> keysIterator = selectedKeys.iterator();
            while (keysIterator.hasNext()) {
                SelectionKey key = keysIterator.next();
                keysIterator.remove();
                if (key.isAcceptable()) {
                    handlerAccept(key);
                } else if (key.isReadable()) {
                    handlerReadWrite(key);
                } else if (key.isWritable()) {
                    handlerReadWrite(key);
                }
            }
        }
    }

    private void handlerReadWrite(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Client client = this.channelClientMap.get(socketChannel);
        if (!client.eventHandler(key)) {
            this.channelClientMap.remove(socketChannel);
            client.close();
            key.cancel();;
            try {
                socketChannel.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handlerAccept(SelectionKey key) {
        try {
            SocketChannel clientSocketChannel = this.serverSocketChannel.accept();
            if (clientSocketChannel == null) {
                return;
            }
            this.logger.log(Level.INFO ,"New Client :" + clientSocketChannel.getRemoteAddress());
            clientSocketChannel.configureBlocking(false);
            SelectionKey clientKey = clientSocketChannel.register(this.selector, SelectionKey.OP_READ);
            Client client = new Client(clientSocketChannel, clientKey, this);
            this.channelClientMap.put(clientSocketChannel, client);
        } catch (IOException ignored) {
        }
    }
}

