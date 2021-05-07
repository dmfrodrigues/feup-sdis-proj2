package sdis.Modules.Chord;

import sdis.PeerInfo;
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
        CompletableFuture[] futureList = new CompletableFuture[chord.getKeySize()];
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = chord.getKey().add(1L << i);
            int finalI = i;
            CompletableFuture<PeerInfo> sFuture = CompletableFuture.supplyAsync(
                new GetPredecessorProtocol(chord, k),
                chord.getExecutor()
            );
            CompletableFuture<PeerInfo> r_Future = CompletableFuture.supplyAsync(
                new GetSuccessorProtocol(chord, chord.getKey()),
                chord.getExecutor()
            );
            CompletableFuture<PeerInfo> f = CompletableFuture.allOf(sFuture, r_Future)
            .thenApplyAsync((ignored) -> {
                try {
                    PeerInfo s = sFuture.get();
                    PeerInfo r_ = r_Future.get();

                    Socket socket = chord.send(s, new FingerRemoveMessage(chord.getPeerInfo(), r_, finalI));
                    socket.shutdownOutput();
                    byte[] response = socket.getInputStream().readAllBytes();
                    return new PeerInfo(response);
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
