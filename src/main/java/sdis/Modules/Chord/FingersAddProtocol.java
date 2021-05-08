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
        CompletableFuture<?>[] futureList = new CompletableFuture[chord.getKeySize()];
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = chord.getKey().subtract(1L << i);
            int finalI = i;
            // System.out.println("Peer " + chord.getKey() + " updating i=" + i);
            CompletableFuture<Void> f = CompletableFuture.supplyAsync(
                new GetPredecessorProtocol(chord, k),
                chord.getExecutor()
            )
            .thenApplyAsync((Chord.NodeInfo predecessor) -> {
                if(predecessor.equals(chord.getNodeInfo())) {
                    return null;
                }
                try {
                    FingerAddMessage m = new FingerAddMessage(chord.getNodeInfo(), finalI);
                    Socket socket = chord.send(predecessor, m);
                    socket.shutdownOutput();
                    socket.getInputStream().readAllBytes();
                    socket.close();
                    // System.out.println("    Peer " + chord.getKey() + "\t sent " + new String(m.asByteArray()) + " to " + predecessor);
                    return null;
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            });
            futureList[i] = f;
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            // System.out.println("Peer " + chord.getKey() + " updated i=" + i);
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
