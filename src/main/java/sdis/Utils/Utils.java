package sdis.Utils;

public class Utils {

    /**
     * @brief Find n-th occurence of a string (needle) in another string (haystack).
     *
     * If n=1, this function finds the first occurence.
     *
     * It returns the index of the first character of the haystack where it matches the needle.
     * If a solution is not found, -1 is returned.
     *
     * @param haystack  String to search in
     * @param needle    String to search for
     * @param n         Number of the occurence.
     * @return
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

    public static int log2(long n) {
        return 63 - Long.numberOfLeadingZeros(n);
    }
}
