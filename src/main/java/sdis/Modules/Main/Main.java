package sdis.Modules.Main;

import sdis.Modules.SystemStorage.SystemStorage;

public class Main {
    public static final int CHUNK_SIZE = 64000;

    private final Username username;
    private final Password password;
    private final SystemStorage systemStorage;

    public Main(Username username, Password password, SystemStorage systemStorage){
        this.username = username;
        this.password = password;
        this.systemStorage = systemStorage;
    }

    public Username getUsername(){
        return username;
    }

    public SystemStorage getSystemStorage(){
        return systemStorage;
    }
}
