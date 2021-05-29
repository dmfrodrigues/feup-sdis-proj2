package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.SetPredecessorMessage;
import sdis.Modules.ProtocolTask;

import java.io.IOException;
import java.net.Socket;

public class SetPredecessorProtocol extends ProtocolTask<Boolean> {

    private final Chord chord;
    private final Chord.NodeInfo nodeInfo;

    public SetPredecessorProtocol(Chord chord, Chord.NodeInfo nodeInfo){
        this.chord = chord;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public Boolean compute() {
        Chord.NodeInfo s = chord.getSuccessor();
        try {
            Socket socket = chord.send(s, new SetPredecessorMessage(nodeInfo));
            readAllBytesAndClose(socket);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
