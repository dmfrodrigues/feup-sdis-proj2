package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.Messages.GetSystemMessage;
import sdis.UUID;

import java.io.IOException;
import java.util.concurrent.CompletionException;

public class GetSystemProtocol extends ProtocolTask<byte[]> {

    private final SystemStorage systemStorage;
    private final UUID id;

    public GetSystemProtocol(SystemStorage systemStorage, UUID id){
        this.systemStorage = systemStorage;
        this.id = id;
    }

    @Override
    public byte[] compute() {
        Chord chord = systemStorage.getChord();
        try{
            Chord.NodeInfo s = chord.findSuccessor(id.getKey(chord));
            GetSystemMessage getSystemMessage = new GetSystemMessage(id);
            return getSystemMessage.sendTo(s.address);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        } catch (CompletionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
