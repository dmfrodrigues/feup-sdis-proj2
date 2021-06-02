package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.Messages.PutSystemMessage;
import sdis.UUID;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class PutSystemProtocol extends ProtocolTask<Boolean> {

    private final SystemStorage systemStorage;
    private final UUID id;
    private final byte[] data;

    public PutSystemProtocol(SystemStorage systemStorage, UUID id, byte[] data){
        this.systemStorage = systemStorage;
        this.id = id;
        this.data = data;
    }

    @Override
    public Boolean compute() {
        Chord chord = systemStorage.getChord();
        try{
            Chord.NodeInfo s = chord.findSuccessor(id.getKey(chord));
            PutSystemMessage putSystemMessage = new PutSystemMessage(id, data);
            return putSystemMessage.sendTo(systemStorage, s.address);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        } catch (CompletionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
