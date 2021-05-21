package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.UUID;

import java.io.Serializable;

public class Username implements Serializable {
    private final String s;

    public Username(String s){
        this.s = s;
    }

    public String getId(){
        return s;
    }

    public String toString(){
        return s;
    }

    public Chord.Key toKey(Chord chord) {
        return chord.newKey("u/" + toString());
    }
}
