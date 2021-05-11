package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.DataStorage.Messages.PutMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class PutProtocol extends ProtocolSupplier<Boolean> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final Chord.Key originalNodeKey;
    private final UUID id;
    private final byte[] data;

    public PutProtocol(Chord chord, DataStorage dataStorage, UUID id, byte[] data){
        this(chord, dataStorage, chord.getKey(), id, data);
    }
    public PutProtocol(Chord chord, DataStorage dataStorage, Chord.Key originalNodeKey, UUID id, byte[] data){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.originalNodeKey = originalNodeKey;
        this.id = id;
        this.data = data;
    }

    @Override
    public Boolean get() {
        Chord.NodeInfo r = chord.getNodeInfo();
        Chord.NodeInfo s = chord.getSuccessor();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStoredLocally = localDataStorage.has(id);
        boolean hasSpaceLocally;
        boolean pointsToSuccessor = dataStorage.successorHasStored(id);
        boolean isBase = dataStorage.has(id);
        try {
            hasSpaceLocally = localDataStorage.canPut(data.length).get();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }

        // If r has stored the datapiece, returns
        if(hasStoredLocally) {
            return true;
        }
        // Everything beyond this point assumes the datapiece is not locally stored

        // If r has space
        if(hasSpaceLocally){
            try {
                localDataStorage.put(id, data).get();    // Store the datapiece
                if(r.key.equals(originalNodeKey)) dataStorage.storeBase(id);
                if(pointsToSuccessor) {
                    // If it was pointing to its successor, delete it from the successor
                    // so that less steps are required to reach the datapiece
                    Socket socket = dataStorage.send(s.address, new DeleteMessage(id));
                    socket.shutdownOutput();
                    socket.getInputStream().readAllBytes();
                    socket.close();
                }
                return true;
            } catch (InterruptedException | IOException e) {
                throw new CompletionException(e);
            } catch (ExecutionException e) {
                throw new CompletionException(e.getCause());
            }
        }
        // Everything beyond this point assumes the node does not have the datapiece locally,
        // nor does it have enough space to store it

        if(s.key == originalNodeKey) return false;

        // If it does not yet point to the successor, point to successor
        if(!pointsToSuccessor) dataStorage.registerSuccessorStored(id);
        if(r.key.equals(originalNodeKey)) dataStorage.storeBase(id);
        try {
            // Send a PUT message to the successor; if it does not yet have that datapiece, the successor will store it;
            // if it already has it, this message just serves as a confirmation that the datapiece is in fact stored.
            PutMessage m = new PutMessage(originalNodeKey, id, data);
            Socket socket = dataStorage.send(s.address, m);
            socket.shutdownOutput();
            byte[] responseByte = socket.getInputStream().readAllBytes();
            socket.close();
            boolean response = m.parseResponse(responseByte);
            if (!response) {
                dataStorage.unregisterSuccessorStored(id);
                dataStorage.unstoreBase(id);
            }
            return response;
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
