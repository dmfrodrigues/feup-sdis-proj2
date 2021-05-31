package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.GetRedirectsMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.CompletionException;

public class GetRedirectsProtocol extends ProtocolTask<Set<UUID>> {

    private final SocketChannel socket;

    public GetRedirectsProtocol(Chord chord){
        this(chord.getPredecessor().socket);
    }

    public GetRedirectsProtocol(SocketChannel socket){
        this.socket = socket;
    }

    @Override
    public Set<UUID> compute() {
        try {
            GetRedirectsMessage m = new GetRedirectsMessage();
            return m.sendTo(socket);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }
    }
}
