package sdis.Protocols.DataStorage;

import sdis.Chord;
import sdis.PeerInfo;
import sdis.Peer;
import sdis.Protocols.DataStorage.Messages.DeleteMessage;
import sdis.Protocols.DataStorage.Messages.PutMessage;
import sdis.Protocols.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class PutProtocol extends ProtocolSupplier<Integer> {

    private final Chord chord;
    private long originalNodeKey;
    private long key;
    private byte[] data;

    public PutProtocol(Chord chord, long key, byte[] data){
        this(chord, chord.getKey(), key, data);
    }
    public PutProtocol(Chord chord, long originalNodeKey, long key, byte[] data){
        this.chord = chord;
        this.originalNodeKey = originalNodeKey;
        this.key = key;
        this.data = data;
    }

    public void store(long id, byte[] data){

    }

    @Override
    public Integer get() {
        PeerInfo r = chord.getPeerInfo();
        PeerInfo s = chord.getSuccessor();
        Peer peer = chord.getPeer();
        String fileID = peer.getFileTable().getFileID(key);

        if(s.key == originalNodeKey) return 1;

        boolean hasStored = peer.getStorageManager().hasDataPiece(fileID);
        boolean hasSpace = peer.getStorageManager().getCapacity()
                >= (peer.getStorageManager().getMemoryUsed() + data.length);


        // If r has not stored that datapiece and does not have space for another piece
        if(!hasStored && !hasSpace){
            peer.getFileTable().registerSuccessorStored(key,s);
            try {
                Socket socket = chord.send(s, new PutMessage(originalNodeKey, key, data));
                socket.shutdownOutput();
                int response = Integer.parseInt(new String(socket.getInputStream().readAllBytes()));
                if(response != 0) {
                    peer.getFileTable().unregisterSuccessorStored(key);
                }
                return response;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
        // If r has already stored that datapiece
        if(hasStored){
            return 0;
        }
        boolean pointsToSuccessor = peer.getFileTable().successorHasStored(key);
        // If r has not stored that chunk but has a pointer to its successor reporting that it might have stored
        if(!hasStored && pointsToSuccessor){
            // If it has space for that chunk
            if(hasSpace){
                try {
                    peer.getStorageManager().saveDataPiece(fileID, data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Socket socket = chord.send(s, new DeleteMessage(key));
                    socket.close();
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
                return 0;
            } else { // If it does not have space for that chunk
                try {
                    Socket socket = chord.send(s, new PutMessage(originalNodeKey, key, data));
                    socket.shutdownOutput();
                    int response = Integer.parseInt(new String(socket.getInputStream().readAllBytes()));
                    if (response != 0) {
                        peer.getFileTable().unregisterSuccessorStored(key);
                    }
                    return response;
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }
        }

        return 0;
    }
}
