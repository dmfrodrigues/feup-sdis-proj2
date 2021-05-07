package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.FingerAddMessage;
import sdis.Modules.ProtocolSupplier;

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
        CompletableFuture<?>[] futureList = new CompletableFuture[Chord.getKeySize()];
        for(int i = 0; i < Chord.getKeySize(); ++i){
            Chord.Key k = chord.getKey().subtract(1L << i);
            int finalK = i;
            CompletableFuture<Chord.NodeInfo> f = CompletableFuture.supplyAsync(
                new GetPredecessorProtocol(chord, k),
                chord.getExecutor()
            )
            .thenApplyAsync(predecessor -> {
                try {
                    Socket socket = chord.send(predecessor, new FingerAddMessage(chord.getPeerInfo(), finalK));
                    socket.shutdownOutput();
                    byte[] response = socket.getInputStream().readAllBytes();
                    return new Chord.NodeInfo(response);
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
