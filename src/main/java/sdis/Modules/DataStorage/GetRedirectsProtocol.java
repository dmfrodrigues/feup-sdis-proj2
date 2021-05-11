package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.GetMessage;
import sdis.Modules.DataStorage.Messages.GetRedirectsMessage;
import sdis.Modules.ProtocolSupplier;
import sdis.UUID;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class GetRedirectsProtocol extends ProtocolSupplier<Set<UUID>> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final InetSocketAddress address;

    public GetRedirectsProtocol(Chord chord, DataStorage dataStorage){
        this(chord, dataStorage, chord.getPredecessor().address);
    }

    public GetRedirectsProtocol(Chord chord, DataStorage dataStorage, InetSocketAddress address){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.address = address;
    }

    @Override
    public Set<UUID> get() {
        try {
            GetRedirectsMessage m = new GetRedirectsMessage();
            Socket socket = dataStorage.send(address, m);
            socket.shutdownOutput();
            byte[] response = socket.getInputStream().readAllBytes();
            socket.close();
            return m.parseResponse(response);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
