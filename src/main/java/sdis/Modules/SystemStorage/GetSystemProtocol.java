package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.DataStorage.Messages.GetMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.Messages.GetSystemMessage;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class GetSystemProtocol extends ProtocolSupplier<byte[]> {

    private final SystemStorage systemStorage;
    private final UUID id;

    public GetSystemProtocol(SystemStorage systemStorage, UUID id){
        this.systemStorage = systemStorage;
        this.id = id;
    }

    @Override
    public byte[] get() {
        Chord chord = systemStorage.getChord();
        try{
            Chord.NodeInfo s = chord.getSuccessor(id.getKey(chord)).get();
            GetSystemMessage getSystemMessage = new GetSystemMessage(id);
            Socket socket = systemStorage.send(s.address, getSystemMessage);
            socket.shutdownOutput();
            byte[] response = socket.getInputStream().readAllBytes();
            socket.close();
            return getSystemMessage.parseResponse(response);
        } catch (InterruptedException | IOException e) {
            throw new CompletionException(e);
        } catch (ExecutionException | CompletionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
