package sdis.Modules.Main;

import sdis.Modules.SystemStorage.SystemStorage;

public class Main {
    public static final int CHUNK_SIZE = 64000;

    private final SystemStorage systemStorage;

    public Main(SystemStorage systemStorage){
        this.systemStorage = systemStorage;
    }

    public SystemStorage getSystemStorage(){
        return systemStorage;
    }
}
