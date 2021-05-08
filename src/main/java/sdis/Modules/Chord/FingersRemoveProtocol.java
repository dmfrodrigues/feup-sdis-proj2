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
            Chord.Key k = chord.getKey().add(1L << i);
            int finalI = i;
            CompletableFuture<Chord.NodeInfo> sFuture = CompletableFuture.supplyAsync(
                new GetPredecessorProtocol(chord, k),
                chord.getExecutor()
            );
            CompletableFuture<Chord.NodeInfo> r_Future = CompletableFuture.supplyAsync(
                new GetSuccessorProtocol(chord, chord.getKey()),
                chord.getExecutor()
            );
            CompletableFuture<Chord.NodeInfo> f = CompletableFuture.allOf(sFuture, r_Future)
            .thenApplyAsync((ignored) -> {
                try {
                    Chord.NodeInfo s = sFuture.get();
                    Chord.NodeInfo r_ = r_Future.get();

                    Socket socket = chord.send(s, new FingerRemoveMessage(chord.getNodeInfo(), r_, finalI));
                    socket.shutdownOutput();
                    byte[] response = socket.getInputStream().readAllBytes();
                    return chord.newNodeInfo(response);
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                } catch (ExecutionException e) {
                    throw new CompletionException(e.getCause());
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
