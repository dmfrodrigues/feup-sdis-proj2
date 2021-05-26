package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.FingerRemoveMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

public class FingersRemoveProtocol extends ProtocolTask<Void> {

    private final Chord chord;

    public FingersRemoveProtocol(Chord chord){
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
                    FingerRemoveMessage m = new FingerRemoveMessage(chord.getNodeInfo(), chord.getSuccessor(), finalI);
                    Socket socket = chord.send(predecessor, m);
                    readAllBytesAndClose(socket);
                    return null;
                } catch (IOException | ExecutionException | InterruptedException e) {
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
