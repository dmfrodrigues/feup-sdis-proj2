package sdis.Modules.DataStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.Messages.DeleteMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.CompletionException;

public class DeleteProtocol extends ProtocolTask<Boolean> {

    private final Chord chord;
    private final DataStorage dataStorage;
    private final UUID id;

    public DeleteProtocol(Chord chord, DataStorage dataStorage, UUID id){
        this.chord = chord;
        this.dataStorage = dataStorage;
        this.id = id;
    }

    @Override
    public Boolean compute() {
        Chord.NodeInfo s = chord.getSuccessor();
        LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();

        boolean hasStored = localDataStorage.has(id);
        boolean pointsToSuccessor = dataStorage.successorHasStored(id);

        // If r has not stored that datapiece and has no pointer saying its successor stored it
        if(!hasStored && !pointsToSuccessor){
            return true;
        }

        // If r has stored that datapiece
        if(hasStored) {
                localDataStorage.delete(id); // Delete the datapiece
                dataStorage.unstoreBase(id);
                return true;
        }
        // We may now assume the datapiece is not locally stored

        // If r has a pointer to its successor reporting that it might have stored
        try {
            DeleteMessage m = new DeleteMessage(id);
            SocketChannel socket = dataStorage.send(s.address, m);
            socket.shutdownOutput();
            byte[] responseByte = socket.getInputStream().readAllBytes();
            socket.close();
            boolean response = m.parseResponse(responseByte);
            if(response){
                dataStorage.unregisterSuccessorStored(id);
            }
            return response;
        } catch (IOException | UnrecoverableKeyException | CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new CompletionException(e);
        }
    }
}
