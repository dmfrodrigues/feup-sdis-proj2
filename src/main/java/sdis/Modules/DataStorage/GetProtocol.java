package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.DataStorage.Messages.GetMessage;
import sdis.Modules.DataStorage.Messages.PutMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.Peer;
import sdis.PeerInfo;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class GetProtocol extends ProtocolSupplier<byte[]> {

    private final Chord chord;
    private final Chord.Key originalNodeKey;
    private final UUID id;

    public GetProtocol(Chord chord, UUID id){
        this(chord, chord.getKey(), id);
    }
    public GetProtocol(Chord chord, Chord.Key originalNodeKey, UUID id){
        this.chord = chord;
        this.originalNodeKey = originalNodeKey;
        this.id = id;
    }

    @Override
    public byte[] get() {
        PeerInfo s = chord.getSuccessor();
        Peer peer = chord.getPeer();
        DataStorage dataStorage = peer.getDataStorage();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStored;
        try {
            hasStored = localDataStorage.has(id).get();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } catch (ExecutionException e) {
            throw new CompletionException(e.getCause());
        }
        if(hasStored){
            try {
                return localDataStorage.get(id).get();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            } catch (ExecutionException e) {
                throw new CompletionException(e.getCause());
            }
        }

        boolean pointsToSuccessor = peer.getDataStorage().successorHasStored(id);
        if(pointsToSuccessor){
            try {
                Socket socket = chord.send(s, new GetMessage(chord.getKey(), id));
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }

        return null;
    }
}
