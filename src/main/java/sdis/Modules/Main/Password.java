package sdis.Modules.Main;

import sdis.Utils.Utils;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Password implements Serializable {
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

    public boolean authenticate(Password password) {
        return authenticate(password.getPlain());
    }

    public String getHashed(){
        return hashed;
    }

    public String getPlain(){
        return plain;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        Password password = (Password) obj;
        return hashed.equals(password.hashed);
    }
}
