package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.GetMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class GetProtocol extends ProtocolTask<byte[]> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final UUID id;

    public GetProtocol(Chord chord, DataStorage dataStorage, UUID id){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.id = id;
    }

    @Override
    public byte[] compute() {
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStored = localDataStorage.has(id);
        if(hasStored){
                return localDataStorage.get(id);
        }

        Chord.NodeConn s = chord.getSuccessor();
        boolean pointsToSuccessor = dataStorage.successorHasStored(id);
        if(pointsToSuccessor){
            try {
                GetMessage m = new GetMessage(id);
                return m.sendTo(s.socket);
            } catch (IOException | InterruptedException e) {
                try { readAllBytesAndClose(s.socket); } catch (InterruptedException ex) { ex.printStackTrace(); }
                throw new CompletionException(e);
            }
        }

        return null;
    }
}
