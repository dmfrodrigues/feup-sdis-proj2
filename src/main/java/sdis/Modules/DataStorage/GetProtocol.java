package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.GetMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class GetProtocol extends ProtocolSupplier<byte[]> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final UUID id;

    public GetProtocol(Chord chord, DataStorage dataStorage, UUID id){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.id = id;
    }

    @Override
    public byte[] get() {
        Chord.NodeInfo s = chord.getSuccessor();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStored = localDataStorage.has(id);
        if(hasStored){
            try {
                return localDataStorage.get(id).get();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            } catch (ExecutionException e) {
                throw new CompletionException(e.getCause());
            }
        }

        boolean pointsToSuccessor = dataStorage.successorHasStored(id);
        if(pointsToSuccessor){
            try {
                GetMessage m = new GetMessage(id);
                Socket socket = dataStorage.send(s.address, m);
                socket.shutdownOutput();
                byte[] response = socket.getInputStream().readAllBytes();
                socket.close();
                return m.parseResponse(response);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }

        return null;
    }
}
