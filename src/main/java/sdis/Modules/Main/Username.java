package sdis.Modules.Main;

import java.io.Serializable;

public class Username implements Serializable {
    private final String s;

    public Username(String s){
        if(s.contains("/")) throw new IllegalArgumentException(s);
        this.s = s;
    }

    public Main.File asFile(long numberOfChunks){
        return new Main.UserMetadataFile(this, numberOfChunks);
    }

    public Main.File asFile(){
        return asFile(0);
    }

    @Override
    public String toString(){
        return s;
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
