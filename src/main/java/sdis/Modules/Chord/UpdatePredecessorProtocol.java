package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.UpdatePredecessorMessage;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;

public class UpdatePredecessorProtocol extends ProtocolSupplier<Void> {

    private final Chord chord;
    private final Chord.NodeInfo nodeInfo;

    public UpdatePredecessorProtocol(Chord chord, Chord.NodeInfo nodeInfo){
        this.chord = chord;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public Void get() {
        Chord.NodeInfo successor = chord.getSuccessor();
        try {
            Socket socket = chord.send(successor, new UpdatePredecessorMessage(nodeInfo));
            socket.shutdownOutput();
            socket.getInputStream().readAllBytes();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
