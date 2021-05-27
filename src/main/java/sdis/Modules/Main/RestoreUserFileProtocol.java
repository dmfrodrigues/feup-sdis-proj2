package sdis.Modules.Main;

import sdis.Storage.ChunkOutput;

public class RestoreUserFileProtocol extends RestoreFileProtocol {
    public RestoreUserFileProtocol(Main main, Username username, ChunkOutput destination, int maxNumberFutures){
        super(main, username.asFile(), destination, maxNumberFutures);
    }

    @Override
    public Boolean compute() {
        long i;
        for (i = 0; ; ++i) {
            byte[] data = getChunk(i);
            if(data == null) break;
            if(!getDestination().set(i, data)) return false;
        }
        return (i != 0);
    }
}
