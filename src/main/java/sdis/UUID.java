package sdis;

import sdis.Modules.Chord.Chord;
import sdis.Utils.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UUID implements Comparable<UUID> {
    private static final String HASH_ALGORITHM = "SHA-256";

    private final String s;

    public UUID(String s){
        this.s = s;
    }

    public String toString(){
        return s;
    }

    public Chord.Key getKey(Chord chord){
        long l = 0;
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(s.getBytes());
            l = Utils.bytesToLong(hash);
        } catch (NoSuchAlgorithmException e) {
            l = s.hashCode();
        }

        return chord.newKey(l);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof UUID))
            return false;
        if (o == this)
            return true;
        return this.s.equals(((UUID) o).s);
    }

    @Override
    public int compareTo(UUID u) {
        if (u == null) throw new NullPointerException();
        return s.compareTo(u.s);
    }

    @Override
    public int hashCode(){
        return s.hashCode();
    }
}
