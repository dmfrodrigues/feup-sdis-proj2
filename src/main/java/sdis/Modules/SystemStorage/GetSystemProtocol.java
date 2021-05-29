package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.Messages.GetSystemMessage;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
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
            Chord.NodeInfo s = chord.getSuccessor(id.getKey(chord));
            GetSystemMessage getSystemMessage = new GetSystemMessage(id);
            Socket socket = systemStorage.send(s.address, getSystemMessage);
            byte[] response = readAllBytesAndClose(socket);
            return getSystemMessage.parseResponse(response);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        } catch (CompletionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
