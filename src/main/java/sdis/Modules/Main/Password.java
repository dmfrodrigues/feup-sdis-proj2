package sdis.Modules.Main;

import sdis.Utils.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Password {
    private static final String HASH_ALGORITHM = "SHA-256";
    private String hashed;
    private transient String plain;

    public Password(String s) {
        set(s);
    }

    public void set(String s) {
        this.plain = s;
        this.hashed = hashPassword(s);
    }

    private String hashPassword(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(s.getBytes());
            return Utils.bytesToHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            return s;
        }
    }

    public boolean authenticate(String password) {
        return (this.hashed.equals(hashPassword(password)));
    }

    public String getHashed(){
        return hashed;
    }

    public String getPlain(){
        return plain;
    }
}
