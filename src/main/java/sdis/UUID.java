package sdis;

import sdis.Modules.Chord.Chord;

public class UUID implements Comparable<UUID> {
    private String s;

    public UUID(String s){
        this.s = s;
    }

    public String toString(){
        return s;
    }

    public Chord.Key getKey(){
        return new Chord.Key(s.hashCode());
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
