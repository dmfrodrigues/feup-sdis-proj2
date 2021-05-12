package sdis.Modules.Main;

import sdis.Modules.SystemStorage.SystemStorage;

public class Main {
    private final SystemStorage systemStorage;

    public Main(SystemStorage systemStorage){
        this.systemStorage = systemStorage;
    }

    public SystemStorage getSystemStorage(){
        return systemStorage;
    }
}
