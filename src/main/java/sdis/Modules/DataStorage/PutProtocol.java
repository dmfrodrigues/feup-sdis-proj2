package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.PeerInfo;
import sdis.Peer;
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
    private final Chord.Key originalNodeKey;
    private final UUID id;
    private final byte[] data;

    public PutProtocol(Chord chord, UUID id, byte[] data){
        this(chord, chord.getKey(), id, data);
    }
    public PutProtocol(Chord chord, Chord.Key originalNodeKey, UUID id, byte[] data){
        this.chord = chord;
        this.originalNodeKey = originalNodeKey;
        this.id = id;
        this.data = data;
    }

    @Override
    public Boolean get() {
        PeerInfo r = chord.getPeerInfo();
        PeerInfo s = chord.getSuccessor();
        Peer peer = chord.getPeer();
        DataStorage dataStorage = peer.getDataStorage();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        if(s.key == originalNodeKey) return false;

        boolean hasStored = false;
        boolean hasSpace = false;
        try {
            hasStored = localDataStorage.has(id).get();
            hasSpace = localDataStorage.canPut(data.length).get();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }


        // If r has not stored that datapiece and does not have space for another piece
        if(!hasStored && !hasSpace){
            peer.getDataStorage().registerSuccessorStored(id);
            try {
                Socket socket = chord.send(s, new PutMessage(originalNodeKey, id, data));
                socket.shutdownOutput();
                boolean response = Boolean.parseBoolean(new String(socket.getInputStream().readAllBytes()));
                if(!response) {
                    peer.getDataStorage().unregisterSuccessorStored(id);
                }
                return response;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
        // If r has already stored that datapiece
        if(hasStored){
            return true;
        }
        boolean pointsToSuccessor = peer.getDataStorage().successorHasStored(id);
        // If r has not stored that chunk but has a pointer to its successor reporting that it might have stored
        if(!hasStored && pointsToSuccessor){
            // If it has space for that chunk
            if(hasSpace){
                try {
                    peer.getDataStorage().put(id, data).get();
                    Socket socket = chord.send(s, new DeleteMessage(id));
                    socket.close();
                } catch (InterruptedException | IOException e) {
                    throw new CompletionException(e);
                } catch (ExecutionException e) {
                    throw new CompletionException(e.getCause());
                }
                return true;
            } else { // If it does not have space for that chunk
                try {
                    Socket socket = chord.send(s, new PutMessage(originalNodeKey, id, data));
                    socket.shutdownOutput();
                    boolean response = Boolean.parseBoolean(new String(socket.getInputStream().readAllBytes()));
                    if (!response) {
                        peer.getDataStorage().unregisterSuccessorStored(id);
                    }
                    return response;
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }
        }

        return true;
    }
}
