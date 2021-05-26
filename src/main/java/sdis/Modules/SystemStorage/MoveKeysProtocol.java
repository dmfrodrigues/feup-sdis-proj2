package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.Messages.MoveKeysMessage;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class MoveKeysProtocol extends ProtocolTask<Void> {

    private final SystemStorage systemStorage;

    public MoveKeysProtocol(SystemStorage systemStorage, Chord.NodeInfo nodeInfo){
        this.systemStorage = systemStorage;
    }

    @Override
    public Void compute() {
        Chord chord = systemStorage.getChord();
        Chord.NodeInfo r = chord.getNodeInfo();

        Chord.NodeInfo s = chord.getSuccessor();
        try{
            MoveKeysMessage moveKeysMessage = new MoveKeysMessage(r);
            Socket socket = systemStorage.send(s.address, moveKeysMessage);
            socket.shutdownOutput();
            socket.getInputStream().readAllBytes();
            socket.close();
            return null;
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
