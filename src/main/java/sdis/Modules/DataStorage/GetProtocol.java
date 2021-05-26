package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.GetMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CompletionException;

public class GetProtocol extends ProtocolTask<byte[]> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final UUID id;

    public GetProtocol(Chord chord, DataStorage dataStorage, UUID id){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.id = id;
    }

    @Override
    public byte[] compute() {
        Chord.NodeInfo s = chord.getSuccessor();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStored = localDataStorage.has(id);
        if(hasStored){
                return localDataStorage.get(id);
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
