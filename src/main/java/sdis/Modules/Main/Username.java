package sdis.Modules.Main;

import java.io.Serializable;
import java.util.regex.Pattern;

public class Username implements Serializable {
    public static final String REGEX = "[a-zA-Z0-9@.+_]{4,28}";
    private static final Pattern pattern = Pattern.compile(REGEX);

    private final String s;

    public Username(String s){
        if(!pattern.matcher(s).matches()) throw new IllegalArgumentException(s);
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
