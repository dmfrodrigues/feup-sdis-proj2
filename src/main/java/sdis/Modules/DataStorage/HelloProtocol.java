package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.HelloMessage;
import sdis.Modules.ProtocolSupplier;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class HelloProtocol extends ProtocolSupplier<Void> {

    private final DataStorage dataStorage;
    private final Chord chord;
    private final Chord.NodeInfo nodeInfo;

    public HelloProtocol(Chord chord, DataStorage dataStorage){
        this(chord, dataStorage, chord.getNodeInfo());
    }

    public HelloProtocol(Chord chord, DataStorage dataStorage, Chord.NodeInfo nodeInfo){
        this.dataStorage = dataStorage;
        this.chord = chord;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public Void get() {
        Chord.NodeInfo r = chord.getNodeInfo();
        Chord.NodeInfo s = chord.getSuccessor();

        try {
            HelloMessage m = new HelloMessage(nodeInfo);
            Socket socket = dataStorage.send(s.address, m);
            socket.shutdownOutput();
            socket.getInputStream().readAllBytes();
            socket.close();
        } catch (IOException e) {
            throw new CompletionException(e);
        }

        // if(!r.equals(nodeInfo)) dataStorage.hello(nodeInfo);
        return null;
    }
}
