package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.Messages.MoveKeysMessage;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class MoveKeysProtocol extends ProtocolTask<Boolean> {

    private final SystemStorage systemStorage;

    public MoveKeysProtocol(SystemStorage systemStorage){
        this.systemStorage = systemStorage;
    }

    @Override
    public Boolean compute() {
        Chord chord = systemStorage.getChord();
        Chord.NodeInfo n = chord.getNodeInfo();

        Chord.NodeConn s = chord.getSuccessor();
        try{
            MoveKeysMessage moveKeysMessage = new MoveKeysMessage(n);
            return moveKeysMessage.sendTo(s.socket);
        } catch (IOException | InterruptedException e) {
            try { s.socket.close(); } catch (IOException ex) { ex.printStackTrace(); }
            throw new CompletionException(e);
        }
    }
}
