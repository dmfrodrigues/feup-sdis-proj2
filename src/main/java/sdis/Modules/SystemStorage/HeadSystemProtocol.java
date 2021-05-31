package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.Messages.GetSystemMessage;
import sdis.Modules.SystemStorage.Messages.HeadSystemMessage;
import sdis.UUID;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class HeadSystemProtocol extends ProtocolTask<Boolean> {

    private final SystemStorage systemStorage;
    private final UUID id;

    public HeadSystemProtocol(SystemStorage systemStorage, UUID id){
        this.systemStorage = systemStorage;
        this.id = id;
    }

    @Override
    public Boolean compute() {
        Chord chord = systemStorage.getChord();
        try{
            Chord.NodeInfo s = chord.findSuccessor(id.getKey(chord));
            HeadSystemMessage headSystemMessage = new HeadSystemMessage(id);
            return headSystemMessage.sendTo(s.address);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        } catch (CompletionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
