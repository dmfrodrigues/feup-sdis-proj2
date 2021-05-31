package sdis.Utils;

import javax.net.ssl.SSLEngine;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Utils {

    /**
     * @brief Find n-th occurrence of a string (needle) in another string (haystack).
     *
     * If n=1, this function finds the first occurrence.
     *
     * It returns the index of the first character of the haystack where it matches the needle.
     * If a solution is not found, -1 is returned.
     *
     * @param haystack  String to search in
     * @param needle    String to search for
     * @param n         Number of the occurrence.
     * @return          Index of the n-th occurrence of needle in haystack
     */
    static public int find_nth(byte[] haystack, byte[] needle, int n){
        for(int i = 0; i+needle.length <= haystack.length; ++i){
            boolean occurs = true;
            for(int j = 0; j < needle.length; ++j){
                if(haystack[i+j] != needle[j]){
                    occurs = false;
                    break;
                }
            }
            if(occurs) --n;
            if(n == 0) return i;
        }
        return -1;
    }

    /**
     * Convert array of bytes to string with hexadecimal representation.
     *
     * @param array     Array of bytes to convert
     * @return          String with hexadecimal conversion
     */
    public static String bytesToHexString(byte[] array) {
        StringBuilder hexString = new StringBuilder(2*array.length);
        for (byte b : array) {
            String hex = Integer.toHexString(0xff & b).toUpperCase();
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static long bytesToLong(byte[] array) {
        int sz = Math.min(array.length, 8);
        long l = 0;
        for(int i = 0; i < sz; ++i){
            l <<= 8;
            l ^= (long) array[i] & 0xff;
        }
        return l;
    }

    /**
     * @brief Delete file and, if a directory, all its contents recursively.
     *
     * @param directoryToBeDeleted  Directory to be deleted
     * @return                      True if successful, false otherwise.
     */
    public static boolean deleteRecursive(File directoryToBeDeleted) {
        boolean ret = true;
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                ret &= deleteRecursive(file);
            }
        }
        ret &= directoryToBeDeleted.delete();
        return ret;
    }

    public static SocketChannel createSocket(SSLEngine sslEngine, InetSocketAddress address) throws IOException {
        SocketChannel socket = SocketChannel.open();
        socket.connect(address);
        return socket;
    }

    public static String fromByteBufferToString(ByteBuffer byteBuffer) {
        return new String(byteBuffer.array(), 0, byteBuffer.position());
    }
}
