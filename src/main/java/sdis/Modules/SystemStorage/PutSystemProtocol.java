package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.DataStorage.Messages.PutMessage;
import sdis.Modules.DataStorage.PutProtocol;
import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.Messages.PutSystemMessage;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class PutSystemProtocol extends ProtocolSupplier<Boolean> {

    private SystemStorage systemStorage;
    private final UUID id;
    private final byte[] data;

    public PutSystemProtocol(SystemStorage systemStorage, UUID id, byte[] data){
        this.systemStorage = systemStorage;
        this.id = id;
        this.data = data;
    }

    @Override
    public Boolean get() {
        Chord chord = systemStorage.getChord();
        try{
            Chord.NodeInfo s = chord.getSuccessor(id.getKey(chord)).get();
            PutSystemMessage putSystemMessage = new PutSystemMessage(s.key, id, data);
            Socket socket = systemStorage.send(s.address, putSystemMessage);
            socket.shutdownOutput();
            byte[] response = socket.getInputStream().readAllBytes();
            socket.close();
            return putSystemMessage.parseResponse(response);
        } catch (InterruptedException | IOException e) {
            throw new CompletionException(e);
        } catch (ExecutionException | CompletionException e) {
            throw new CompletionException(e.getCause());
        }
    }
}
