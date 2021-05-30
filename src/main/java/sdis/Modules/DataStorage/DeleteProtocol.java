package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class DeleteProtocol extends ProtocolTask<Boolean> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final UUID id;

    public DeleteProtocol(Chord chord, DataStorage dataStorage, UUID id){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.id = id;
    }

    @Override
    public Boolean compute() {
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStored = localDataStorage.has(id);
        boolean pointsToSuccessor = dataStorage.successorHasStored(id);

        // If r has not stored that datapiece and has no pointer saying its successor stored it
        if(!hasStored && !pointsToSuccessor){
            return true;
        }

        // If r has stored that datapiece
        if(hasStored) {
                localDataStorage.delete(id); // Delete the datapiece
                dataStorage.unstoreBase(id);
                return true;
        }
        // We may now assume the datapiece is not locally stored

        Chord.NodeConn s = chord.getSuccessor();

        // If r has a pointer to its successor reporting that it might have stored
        try {
            DeleteMessage m = new DeleteMessage(id);
            boolean response = m.sendTo(s.socket);
            if(response){
                dataStorage.unregisterSuccessorStored(id);
            }
            return response;
        } catch (IOException | InterruptedException e) {
            try { readAllBytesAndClose(s.socket); } catch (InterruptedException ex) { ex.printStackTrace(); }
            throw new CompletionException(e);
        }
    }
}
