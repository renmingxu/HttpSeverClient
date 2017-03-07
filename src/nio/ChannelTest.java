package nio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by renmingxu on 2017/3/6.
 */
public class ChannelTest {
    public static void main(String[] args){
        Selector selector = null;
        ServerSocketChannel serverSocketChannel = null;
        SelectionKey serverKey;
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(8088));
            serverSocketChannel.configureBlocking(false);
            serverKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Set<SocketChannel> clients = new HashSet<>();

        while(true) {
            int n = 0;
            try {
                n = selector.select(1000);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("size:" + selector.keys().size());
            if (n == 0) {
                System.out.println(System.currentTimeMillis() + " waiting!");
                continue;
            }
            System.out.println("n = " + n);
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keys.iterator();
            while(keyIterator.hasNext()){
                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    System.out.println("Accepting");
                    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                    try {
                        SocketChannel client = serverSocketChannel.accept();
                        if (client == null) {
                            continue;
                        }
                        System.out.println(client);
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (key.isReadable()) {
                    System.out.println("Reading");
                    SocketChannel channel = (SocketChannel) key.channel();
                    ByteBuffer buf = ByteBuffer.allocate(10240);
                    try {
                        int len = channel.read(buf);
                        if (len <= 0) {
                            key.cancel();
                            channel.close();
                        }
                        buf.flip();
                        while(buf.hasRemaining()) {
                            channel.write(buf);
                        }
                    } catch (IOException e) {
                        key.cancel();
                        try {
                            channel.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                keyIterator.remove();
            }

        }
    }

}
