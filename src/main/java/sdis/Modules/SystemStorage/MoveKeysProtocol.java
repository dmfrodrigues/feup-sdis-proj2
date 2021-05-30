package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.Messages.MoveKeysMessage;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class MoveKeysProtocol extends ProtocolTask<Boolean> {

    private final SystemStorage systemStorage;

    public MoveKeysProtocol(SystemStorage systemStorage){
        this.systemStorage = systemStorage;
    }

    @Override
    public Boolean compute() {
        Chord chord = systemStorage.getChord();
        Chord.NodeInfo r = chord.getNodeInfo();

        Chord.NodeInfo s = chord.getSuccessor();
        try{
            MoveKeysMessage moveKeysMessage = new MoveKeysMessage(r);
            return moveKeysMessage.sendTo(s.address);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }
    }
}
