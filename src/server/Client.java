package server;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

/**
 * Created by renmingxu on 2017/3/6.
 */
public class Client {

    public static final int WAITING_NEW_REQ = 0;
    public static final int UNFINISHED_GET = 1;
    public static final int UNFINISHED_POST = 1;

    private SocketChannel socketChannel;
    private HttpServer httpServer;
    private SelectionKey selectionKey;
    private Selector selector;

    private ByteBuffer bufferUnFinished;

    private boolean needToSend;
    private ArrayList<ByteBuffer> bufferToSend;

    public Client(SocketChannel socketChannel, SelectionKey selectionKey,HttpServer httpServer) {
        this.socketChannel = socketChannel;
        this.selectionKey = selectionKey;
        this.selector = selectionKey.selector();
        this.httpServer = httpServer;
        this.bufferToSend = new ArrayList<>();
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
        byte[] b = new byte[buf.remaining()];
        buf.get(b, 0, b.length);
        send(b);
        return true;
    }

    private boolean toSend() {
        ByteBuffer[] byteBuffers = this.bufferToSend.toArray(new ByteBuffer[1]);
        long len = 0;
        try {
            len = this.socketChannel.write(byteBuffers);
        } catch (IOException e) {
            return false;
        }
        if (len < 0) {
            return false;
        }
        this.bufferToSend.removeIf(b -> !b.hasRemaining());
        System.gc();
        System.out.println("Need to send: " + bufferToSend.size());
        if (this.bufferToSend.size() == 0) {
            this.needToSend = false;
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
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length);
        byteBuffer.put(data);
        byteBuffer.flip();
        this.bufferToSend.add(byteBuffer);
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
