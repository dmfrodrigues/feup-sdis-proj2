package sdis.Modules.Main;

import sdis.Utils.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Password {
    private static final String HASH_ALGORITHM = "SHA-256";
    private String s;

    public Password(String s) throws NoSuchAlgorithmException {
        set(s);
    }

    public void set(String s) throws NoSuchAlgorithmException {
        this.s = hashPassword(s);
    }

    private String hashPassword(String s) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hash = digest.digest(s.getBytes());
        return Utils.bytesToHexString(hash);
    }

    public boolean authenticate(String password) throws NoSuchAlgorithmException {
        return (this.s.equals(hashPassword(password)));
    }
}
