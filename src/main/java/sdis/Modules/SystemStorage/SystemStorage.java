package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.Message;
import sdis.UUID;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;

public class SystemStorage {
    private final Chord chord;
    private final DataStorage dataStorage;

    public SystemStorage(Chord chord, DataStorage dataStorage, Executor executor){
        this.chord = chord;
        this.dataStorage = dataStorage;
    }

    public Chord getChord() {
        return chord;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public Socket send(InetSocketAddress to, Message m) throws IOException {
        Socket socket = new Socket(to.getAddress(), to.getPort());
        OutputStream os = socket.getOutputStream();
        os.write(m.asByteArray());
        os.flush();
        return socket;
    }

    public Socket sendAny(Message message) throws IOException {
        Chord.NodeInfo to = chord.getSuccessor();
        return send(to.address, message);
    }

    public Boolean put(UUID id, byte[] data) {
        return new PutSystemProtocol(this, id, data).invoke();
    }

    public byte[] get(UUID id) {
        return new GetSystemProtocol(this, id).invoke();
    }

    public Boolean delete(UUID id) {
        return new DeleteSystemProtocol(this, id).invoke();
    }
}
