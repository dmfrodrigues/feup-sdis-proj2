package sdis.Modules.Main;

import sdis.Modules.Chord.Chord;
import sdis.UUID;

import java.io.Serializable;

public class Username implements Serializable {
    private final String s;

    public Username(String s){
        this.s = s;
    }

    public Main.Path getPath(){
        return new Main.Path("u/" + s);
    }

    public String toString(){
        return s;
    }
}
