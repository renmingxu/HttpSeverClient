package server;

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

/**
 * Created by renmingxu on 2017/3/6.
 */
public class HttpServer {
    private int port;
    private String rootDirectory;

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private Map<SocketChannel, Client> channelClientMap;

    public HttpServer(int port, String rootDirectory) {
        this.port = port;
        this.rootDirectory = rootDirectory;
        this.channelClientMap = new HashMap<>();
    }

    public void start() {
        try {
            this.selector = Selector.open();
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.bind(new InetSocketAddress(port));
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        listen();
    }

    private void listen() {
        while (true) {
            int n = 0;
            try {
                n = this.selector.select(1000);
            } catch (IOException e) {
                continue;
            }
            if (n == 0) {
                continue;
            }
            Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
            Iterator<SelectionKey> keysIterator = selectedKeys.iterator();
            while (keysIterator.hasNext()) {
                SelectionKey key = keysIterator.next();
                keysIterator.remove();
                if (key.isAcceptable()) {
                    System.out.println("Accepting");
                    handlerAccept(key);
                } else if (key.isReadable()) {
                    System.out.println("Reading");
                    handlerReadWrite(key);
                } else if (key.isWritable()) {
                    System.out.println("Writing");
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
            clientSocketChannel.configureBlocking(false);
            SelectionKey clientKey = clientSocketChannel.register(this.selector, SelectionKey.OP_READ);
            Client client = new Client(clientSocketChannel, clientKey, this);
            this.channelClientMap.put(clientSocketChannel, client);
        } catch (IOException ignored) {
        }
    }
}

