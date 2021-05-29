package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.GetRedirectsMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CompletionException;

public class GetRedirectsProtocol extends ProtocolTask<Set<UUID>> {

    private final DataStorage dataStorage;
    private final InetSocketAddress address;

    public GetRedirectsProtocol(DataStorage dataStorage, Chord chord){
        this(dataStorage, chord.getPredecessor().address);
    }

    public GetRedirectsProtocol(DataStorage dataStorage, InetSocketAddress address){
        this.dataStorage = dataStorage;
        this.address = address;
    }

    @Override
    public Set<UUID> compute() {
        try {
            GetRedirectsMessage m = new GetRedirectsMessage();
            Socket socket = dataStorage.send(address, m);
            byte[] response = readAllBytesAndClose(socket);
            return m.parseResponse(response);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }
    }
}
