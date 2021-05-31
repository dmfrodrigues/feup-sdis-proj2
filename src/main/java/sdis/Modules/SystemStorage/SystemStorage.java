package sdis.Modules.SystemStorage;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.Message;
import sdis.UUID;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

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

    /*
    public Socket send(Socket socket, Message m) throws IOException {
        socket.getOutputStream().write(m.asByteArray());
        socket.getOutputStream().flush();
        return socket;
    }
     */

    /*
    public Socket send(InetSocketAddress to, Message m) throws IOException {
        return send(new Socket(to.getAddress(), to.getPort()), m);
    }
     */

    public SocketChannel sendAny(Message m) throws IOException {
        Chord.NodeConn to = chord.getSuccessor();
        to.socket.write(m.asByteBuffer());
        return to.socket;
    }

    public boolean put(UUID id, byte[] data) {
        return new PutSystemProtocol(this, id, data).invoke();
    }

    public boolean head(UUID id) {
        return new HeadSystemProtocol(this, id).invoke();
    }

    public byte[] get(UUID id) {
        return new GetSystemProtocol(this, id).invoke();
    }

    public boolean delete(UUID id) {
        return new DeleteSystemProtocol(this, id).invoke();
    }
}
