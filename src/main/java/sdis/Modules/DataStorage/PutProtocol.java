package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Chord.Messages.HelloMessage;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.DataStorage.Messages.PutMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class PutProtocol extends ProtocolTask<Boolean> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final Chord.Key originalNodeKey;
    private final UUID id;
    private final byte[] data;

    public PutProtocol(Chord chord, DataStorage dataStorage, UUID id, byte[] data){
        this(chord, dataStorage, chord.getNodeInfo().key, id, data);
    }
    public PutProtocol(Chord chord, DataStorage dataStorage, Chord.Key originalNodeKey, UUID id, byte[] data){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.originalNodeKey = originalNodeKey;
        this.id = id;
        this.data = data;
    }

    @Override
    public Boolean compute() {
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStoredLocally = localDataStorage.has(id);
        boolean hasSpaceLocally = localDataStorage.canPut(data.length);
        boolean pointsToSuccessor = dataStorage.successorHasStored(id);

        // If r has stored the datapiece, returns
        if(hasStoredLocally) {
            return true;
        }
        // Everything beyond this point assumes the datapiece is not locally stored

        Chord.NodeConn s = chord.getSuccessor();

        // If r has space
        if(hasSpaceLocally){
            try {
                if(!localDataStorage.put(id, data)) return false;    // Store the datapiece
                if(pointsToSuccessor) {
                    // If it was pointing to its successor, delete it from the successor
                    // so that less steps are required to reach the datapiece
                    DeleteMessage deleteMessage = new DeleteMessage(id);
                    deleteMessage.sendTo(s.socket);
                } else {
                    new HelloMessage().sendTo(chord, s.socket);
                }
                return true;
            } catch (IOException | InterruptedException e) {
                throw new CompletionException(e);
            }
        }
        // Everything beyond this point assumes the node does not have the datapiece locally,
        // nor does it have enough space to store it

        if(s.nodeInfo.key == originalNodeKey){
            try { new HelloMessage().sendTo(chord, s.socket); } catch (IOException | InterruptedException e) { e.printStackTrace(); }
            return false;
        }

        // If it does not yet point to the successor, point to successor
        if(!pointsToSuccessor) dataStorage.registerSuccessorStored(id);
        try {
            // Send a PUT message to the successor; if it does not yet have that datapiece, the successor will store it;
            // if it already has it, this message just serves as a confirmation that the datapiece is in fact stored.
            PutMessage m = new PutMessage(originalNodeKey, id, data);
            boolean response = m.sendTo(s.socket);
            if (!response) {
                dataStorage.unregisterSuccessorStored(id);
            }
            return response;
        } catch (IOException | InterruptedException e) {
            try { s.socket.close(); } catch (IOException ex) { ex.printStackTrace(); }
            throw new CompletionException(e);
        }
    }
}
