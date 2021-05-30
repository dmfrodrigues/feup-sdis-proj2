package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.Message;
import sdis.UUID;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SystemStorage {
    private final Chord chord;
    private final DataStorage dataStorage;

    public SystemStorage(Chord chord, DataStorage dataStorage){
        this.chord = chord;
        this.dataStorage = dataStorage;
    }

    public Chord getChord() {
        return chord;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public Socket send(Socket socket, Message m) throws IOException {
        socket.getOutputStream().write(m.asByteArray());
        socket.getOutputStream().flush();
        return socket;
    }

    public Socket send(InetSocketAddress to, Message m) throws IOException {
        return send(new Socket(to.getAddress(), to.getPort()), m);
    }

    public Socket sendAny(Message message) throws IOException {
        Chord.NodeConn to = chord.getSuccessor();
        return send(to.nodeInfo.address, message);
    }

    public boolean put(UUID id, byte[] data) {
        return new PutSystemProtocol(this, id, data).invoke();
    }

    public byte[] get(UUID id) {
        return new GetSystemProtocol(this, id).invoke();
    }

    public boolean delete(UUID id) {
        return new DeleteSystemProtocol(this, id).invoke();
    }
}
