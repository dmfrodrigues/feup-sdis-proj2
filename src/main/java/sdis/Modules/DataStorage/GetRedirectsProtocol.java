package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.GetRedirectsMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.CompletionException;

public class GetRedirectsProtocol extends ProtocolTask<Set<UUID>> {

    private final InetSocketAddress address;

    public GetRedirectsProtocol(Chord chord){
        this(chord.getPredecessor().address);
    }

    public GetRedirectsProtocol(InetSocketAddress address){
        this.address = address;
    }

    @Override
    public Set<UUID> compute() {
        try {
            GetRedirectsMessage m = new GetRedirectsMessage();
            return m.sendTo(address);
        } catch (IOException | InterruptedException e) {
            throw new CompletionException(e);
        }
    }
}
