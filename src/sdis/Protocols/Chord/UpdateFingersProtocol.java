package sdis.Protocols.Chord;

import sdis.Chord;
import sdis.PeerInfo;
import sdis.Protocols.Chord.Messages.GetPredecessorMessage;
import sdis.Protocols.Chord.Messages.UpdateFingerMessage;
import sdis.Protocols.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class UpdateFingersProtocol extends ProtocolSupplier<Void> {

    private final Chord chord;

    public UpdateFingersProtocol(Chord chord){
        this.chord = chord;
    }

    @Override
    public Void get() {
        CompletableFuture[] futureList = new CompletableFuture[chord.getKeySize()];
        for(int k = 0; k < chord.getKeySize(); ++k){
            long key = chord.getKey() - (1L << k);
            int finalK = k;
            CompletableFuture<PeerInfo> f = CompletableFuture.supplyAsync(
                new GetSuccessorProtocol(chord, key),
                Chord.getExecutor()
            )
            .thenApplyAsync(peer -> {
                try {
                    Socket socket = chord.send(peer, new GetPredecessorMessage());
                    socket.shutdownOutput();
                    byte[] response = socket.getInputStream().readAllBytes();
                    return new PeerInfo(response);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            })
            .thenApplyAsync(predecessor -> {
                try {
                    Socket socket = chord.send(predecessor, new UpdateFingerMessage(chord.getPeerInfo(), finalK));
                    socket.shutdownOutput();
                    byte[] response = socket.getInputStream().readAllBytes();
                    return new PeerInfo(response);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            });
            futureList[k] = f;
        }
        CompletableFuture<Void> future = CompletableFuture.allOf(futureList);
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } catch (ExecutionException e){
            throw new CompletionException(e.getCause());
        }
        return null;
    }
}
