package sdis.Modules.Chord;

import sdis.Modules.Chord.Messages.SetPredecessorMessage;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;

public class SetPredecessorProtocol extends ProtocolSupplier<Void> {

    private final Chord chord;
    private final Chord.NodeInfo nodeInfo;

    public SetPredecessorProtocol(Chord chord, Chord.NodeInfo nodeInfo){
        this.chord = chord;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public Void get() {
        Chord.NodeInfo s = chord.getSuccessor();
        try {
            Socket socket = chord.send(s, new SetPredecessorMessage(nodeInfo));
            socket.shutdownOutput();
            socket.getInputStream().readAllBytes();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
