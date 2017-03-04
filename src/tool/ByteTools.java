package tool;

/**
 * Created by renmingxu on 17-3-3.
 */
public class ByteTools {
    public static byte[] subByte(byte[] b, int start, int len) {
        byte[] result = new byte[len];
        System.arraycopy(b, start, result, 0, len);
        return result;
    }
    public static byte[] addByte(byte[] a ,byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
