package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.FingerAddMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

public class FingersAddProtocol extends ProtocolTask<Void> {

    private final Chord chord;

    public FingersAddProtocol(Chord chord){
        this.chord = chord;
    }

    @Override
    public Void compute() {
        Chord.NodeInfo r = chord.getNodeInfo();
        RecursiveTask<?>[] tasks = new RecursiveTask[chord.getKeySize()];
        for(int i = 0; i < chord.getKeySize(); ++i){
            Chord.Key k = chord.getKey().subtract(1L << i).add(1);
            int finalI = i;
            RecursiveTask<Void> task = new ProtocolTask<>() {
                @Override
                protected Void compute() {
                    Chord.NodeInfo predecessor = chord.getPredecessor(k);
                    try {
                        FingerAddMessage m = new FingerAddMessage(chord.getNodeInfo(), finalI);
                        m.sendTo(chord, predecessor.address);
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
