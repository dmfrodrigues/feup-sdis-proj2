package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.Messages.DeleteSystemMessage;
import sdis.UUID;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class DeleteSystemProtocol extends ProtocolTask<Boolean> {

    private final SystemStorage systemStorage;
    private final UUID id;

    public DeleteSystemProtocol(SystemStorage systemStorage, UUID id){
        this.systemStorage = systemStorage;
        this.id = id;
    }

    @Override
    public Boolean compute() {
        Chord chord = systemStorage.getChord();
        try{
            Chord.NodeInfo s = chord.findSuccessor(id.getKey(chord));
            DeleteSystemMessage deleteSystemMessage = new DeleteSystemMessage(id);
            return deleteSystemMessage.sendTo(s.address);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        } catch (CompletionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
