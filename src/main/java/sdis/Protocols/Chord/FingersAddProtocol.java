package sdis.Protocols.Chord;

import sdis.PeerInfo;
import sdis.Protocols.Chord.Messages.FingerAddMessage;
import sdis.Protocols.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class FingersAddProtocol extends ProtocolSupplier<Void> {

    private final Chord chord;

    public FingersAddProtocol(Chord chord){
        this.chord = chord;
    }

    @Override
    public Void get() {
        CompletableFuture[] futureList = new CompletableFuture[chord.getKeySize()];
        for(int i = 0; i < chord.getKeySize(); ++i){
            long k = chord.getKey() - (1L << i);
            int finalK = i;
            CompletableFuture<PeerInfo> f = CompletableFuture.supplyAsync(
                new GetPredecessorProtocol(chord, k),
                Chord.getExecutor()
            )
            .thenApplyAsync(predecessor -> {
                try {
                    Socket socket = chord.send(predecessor, new FingerAddMessage(chord.getPeerInfo(), finalK));
                    socket.shutdownOutput();
                    byte[] response = socket.getInputStream().readAllBytes();
                    return new PeerInfo(response);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            });
            futureList[i] = f;
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
