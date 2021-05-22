package sdis.Modules.Main;

import sdis.UUID;

import java.io.Serializable;

public class Username implements Serializable {
    private final String s;

    public Username(String s){
        if(s.contains("/")) throw new IllegalArgumentException(s);
        this.s = s;
    }

    public Main.Path getPath(){
        return new Main.Path(s);
    }

    public String toString(){
        return s;
    }

    public UUID toUUID() {
        return new UUID(s);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        Username username = (Username) obj;
        return s.equals(username.s);
    }
}
