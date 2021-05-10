package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class DeleteProtocol extends ProtocolSupplier<Boolean> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final UUID id;

    public DeleteProtocol(Chord chord, DataStorage dataStorage, UUID id){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.id = id;
    }

    @Override
    public Boolean get() {
        Chord.NodeInfo r = chord.getNodeInfo();
        Chord.NodeInfo s = chord.getSuccessor();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStored;
        boolean pointsToSuccessor = dataStorage.successorHasStored(id);
        try {
            hasStored = localDataStorage.has(id).get();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }

        // If r has not stored that datapiece and has no pointer saying its successor stored it
        if(!hasStored && !pointsToSuccessor){
            return true;
        }

        // If r has stored that datapiece
        if(hasStored) {
            try {
                dataStorage.delete(id).get(); // Delete the datapiece
                return true;
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            } catch (ExecutionException e) {
                throw new CompletionException(e.getCause());
            }
        }
        // We may now assume the datapiece is not locally stored

        // If r has a pointer to its successor reporting that it might have stored
        try {
            Socket socket = dataStorage.send(s.address, new DeleteMessage(id));
            socket.shutdownOutput();
            byte[] response = socket.getInputStream().readAllBytes();
            socket.close();
            return Boolean.parseBoolean(new String(response));
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
