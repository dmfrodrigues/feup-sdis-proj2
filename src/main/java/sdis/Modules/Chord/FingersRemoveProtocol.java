package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.FingerRemoveMessage;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class FingersRemoveProtocol extends ProtocolSupplier<Void> {

    private final Chord chord;

    public FingersRemoveProtocol(Chord chord){
        this.chord = chord;
    }

    @Override
    public Void get() {
        CompletableFuture<?>[] futureList = new CompletableFuture[chord.getKeySize()];
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = chord.getKey().subtract(1L << i);
            int finalI = i;
            CompletableFuture<Void> f = CompletableFuture.supplyAsync(
                new GetPredecessorProtocol(chord, k),
                chord.getExecutor()
            )
            .thenApplyAsync((Chord.NodeInfo predecessor) -> {
                try {
                    FingerRemoveMessage m = new FingerRemoveMessage(chord.getNodeInfo(), chord.getSuccessor(), finalI);
                    Socket socket = chord.send(predecessor, m);
                    socket.shutdownOutput();
                    socket.getInputStream().readAllBytes();
                    socket.close();
                    return null;
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
