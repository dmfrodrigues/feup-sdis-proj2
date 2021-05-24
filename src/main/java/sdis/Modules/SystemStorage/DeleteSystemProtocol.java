package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.Messages.DeleteSystemMessage;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class DeleteSystemProtocol extends ProtocolSupplier<Boolean> {

    private final SystemStorage systemStorage;
    private final UUID id;

    public DeleteSystemProtocol(SystemStorage systemStorage, UUID id){
        this.systemStorage = systemStorage;
        this.id = id;
    }

    @Override
    public Boolean get() {
        Chord chord = systemStorage.getChord();
        try{
            Chord.NodeInfo s = chord.getSuccessor(id.getKey(chord)).get();
            DeleteSystemMessage deleteSystemMessage = new DeleteSystemMessage(id);
            Socket socket = systemStorage.send(s.address, deleteSystemMessage);
            socket.shutdownOutput();
            byte[] response = socket.getInputStream().readAllBytes();
            socket.close();
            return deleteSystemMessage.parseResponse(response);
        } catch (InterruptedException | IOException e) {
            throw new CompletionException(e);
        } catch (ExecutionException | CompletionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
