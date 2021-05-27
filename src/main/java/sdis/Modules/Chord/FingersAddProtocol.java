package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.FingerAddMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public class FingersAddProtocol extends ProtocolTask<Void> {

    private final Chord chord;

    public FingersAddProtocol(Chord chord){
        this.chord = chord;
    }

    @Override
    public Void compute() {
        RecursiveTask<?>[] tasks = new RecursiveTask[chord.getKeySize()];
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = chord.getKey().subtract(1L << i);
            int finalI = i;
            RecursiveTask<Void> task = new ProtocolTask<>() {
                @Override
                protected Void compute() {
                GetPredecessorProtocol getPredecessorProtocol = new GetPredecessorProtocol(chord, k);
                Chord.NodeInfo predecessor = getPredecessorProtocol.compute();
                try {
                    FingerAddMessage m = new FingerAddMessage(chord.getNodeInfo(), finalI);
                    Socket socket = chord.send(predecessor, m);
                    readAllBytesAndClose(socket);
                    return null;
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
                }
            };
            tasks[i] = task;
        }
        invokeAll(tasks);
        return null;
    }
}
