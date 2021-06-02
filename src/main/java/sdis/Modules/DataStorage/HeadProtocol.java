package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.HeadMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class HeadProtocol extends ProtocolTask<Boolean> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final UUID id;

    public HeadProtocol(Chord chord, DataStorage dataStorage, UUID id){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.id = id;
    }

    @Override
    public Boolean compute() {
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStored = localDataStorage.has(id);
        if(hasStored){
            return true;
        }

        Chord.NodeConn s = chord.getSuccessor();
        boolean pointsToSuccessor = dataStorage.successorHasStored(id);
        if(pointsToSuccessor){
            try {
                HeadMessage m = new HeadMessage(id);
                return m.sendTo(s.socket);
            } catch (IOException | InterruptedException e) {
                try { s.socket.close(); } catch (IOException ex) { ex.printStackTrace(); }
                throw new CompletionException(e);
            }
        }

        return null;
    }
}
