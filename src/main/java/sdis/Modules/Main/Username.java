package sdis.Modules.Main;

import sdis.UUID;

import java.io.Serializable;

public class Username implements Serializable {
    private final String s;

    public Username(String s){
        this.s = s;
    }

    public UUID getId(){
        return new UUID("u/" + s);
    }

    public String toString(){
        return s;
    }
}
