package sdis;

import sdis.Modules.Chord.Chord;
import sdis.Modules.Chord.FixChordProtocol;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.GetRedirectsProtocol;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.Main.*;
import sdis.Modules.Message;
import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.MoveKeysProtocol;
import sdis.Modules.SystemStorage.ReclaimProtocol;
import sdis.Modules.SystemStorage.RemoveKeysProtocol;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ChunkIterator;
import sdis.Storage.ChunkOutput;
import sdis.Utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static sdis.Modules.Main.Main.CHUNK_SIZE;
import static sdis.Modules.Main.Main.MAX_HEADER_SIZE;

public class Peer implements PeerInterface {

    private final ServerSocketChannel serverSocket;
    private final Chord.Key id;
    private final Path baseStoragePath;
    private final InetSocketAddress socketAddress;
    private final Chord chord;
    private final DataStorage dataStorage;
    private final SystemStorage systemStorage;
    private final Main main;
    private final Thread serverSocketHandlerThread;

    public Peer(int keySize, long id, InetAddress ipAddress) throws IOException {
        this(keySize, id, ipAddress, Paths.get("."));
    }

    public Peer(int keySize, long id, InetAddress ipAddress, Path baseStoragePath) throws IOException {
        serverSocket = ServerSocketChannel.open();
        serverSocket.configureBlocking(true);
        serverSocket.socket().bind(null);
        socketAddress = new InetSocketAddress(ipAddress, serverSocket.socket().getLocalPort());

        System.out.println(
            "Starting peer " + id +
            " with address " + socketAddress
        );

        this.baseStoragePath = Paths.get(baseStoragePath.toString(), Long.toString(id));
        chord = new Chord(socketAddress, keySize, id);
        dataStorage = new DataStorage(Paths.get(this.baseStoragePath.toString(), "storage/data"), getChord());
        systemStorage = new SystemStorage(chord, dataStorage);
        main = new Main(systemStorage);

        this.id = chord.newKey(id);

        chord.scheduleFixes();
        main.scheduleFixes();

        ServerSocketHandler serverSocketHandler = new ServerSocketHandler(this, serverSocket);
        serverSocketHandlerThread = new Thread(serverSocketHandler);
        serverSocketHandlerThread.start();
    }

    public void bindAsRemoteObject(String remoteObjName) throws RemoteException, AlreadyBoundException {
        PeerInterface stub = (PeerInterface) UnicastRemoteObject.exportObject(this, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.bind(remoteObjName, stub);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                registry.unbind(remoteObjName);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }));
    }

    public Chord.Key getKey() {
        return id;
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public Chord getChord() {
        return chord;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public SystemStorage getSystemStorage() {
        return systemStorage;
    }

    public Main getMain() {
        return main;
    }

    public boolean join(){
        System.out.println("Peer " + id + " creating a new chord");

        return chord.join();
    }

    public boolean join(InetSocketAddress gateway){
        System.out.println("Peer " + id + " joining a chord");

        return chord.join(gateway, new ProtocolTask<>() {
            @Override
            public Boolean compute() {
                // Get redirects
                GetRedirectsProtocol getRedirectsProtocol = new GetRedirectsProtocol(chord);
                Set<UUID> redirects = getRedirectsProtocol.invoke();
                for(UUID id : redirects)
                    dataStorage.registerSuccessorStored(id);

                // Move keys
                MoveKeysProtocol moveKeysProtocol = new MoveKeysProtocol(systemStorage);
                return moveKeysProtocol.invoke();
            }
        });
    }

    public boolean leave(){
        System.out.println("Peer " + id + " leaving its chord");

        boolean ret = chord.leave(new ProtocolTask<>() {
            @Override
            public Boolean compute() {
                // Remove keys
                RemoveKeysProtocol removeKeysProtocol = new RemoveKeysProtocol(systemStorage);
                if (!removeKeysProtocol.invoke()) return false;

                // Delete local storage
                return Utils.deleteRecursive(baseStoragePath.toFile());
            }
        });

        ret &= kill();

        System.out.println("Peer " + chord.getNodeInfo().key + " done leaving");

        return ret;
    }

    public boolean kill(){
        chord.killFixes();
        main.killFixes();

        try {
            serverSocket.close();
            serverSocketHandlerThread.join();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public UserMetadata authenticate(Username username, Password password) {
        return main.authenticate(username, password);
    }

    /**
     * Backup file.
     */
    public boolean backup(Username username, Password password, Main.Path path, int replicationDegree, ChunkIterator chunkIterator) {
        try {
            UserMetadata userMetadata = authenticate(username, password);
            if(userMetadata == null){
                System.err.println("Failed to authenticate");
                return false;
            }

            if(userMetadata.getFile(path) != null){
                System.err.println("Failed to backup: file already exists");
            }

            Main.File file = new Main.File(username, path, chunkIterator.length(), replicationDegree);

            return main.backupFile(file, chunkIterator);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Restore file.
     */
    public boolean restore(Username username, Password password, Main.Path path, ChunkOutput chunkOutput) {
        Main.File file = getFile(username, password, path);
        if (file == null) return false;

        return main.restoreFile(file, chunkOutput);
    }

    /**
     * Delete file.
     */
    public boolean delete(Username username, Password password, Main.Path path) {
        Main.File file = getFile(username, password, path);
        if (file == null) return false;

        return main.deleteFile(file);
    }

    private Main.File getFile(Username username, Password password, Main.Path path) {
        UserMetadata userMetadata = authenticate(username, password);
        if(userMetadata == null){
            System.err.println("Failed to authenticate");
            return null;
        }

        Main.File file = userMetadata.getFile(path);
        if(file == null){
            System.err.println("No such file with path " + path);
            Set<Main.Path> files = userMetadata.getFiles();
            System.err.println("Available files (" + files.size() + "):");
            for(Main.Path f: files){
                System.err.println("    " + f);
            }
            return null;
        }
        return file;
    }

    @Override
    public boolean deleteAccount(Username username, Password password) {
        return new DeleteAccountProtocol(main, username, password).invoke();
    }

    /**
     * Set space the peer may use to backup chunks from other machines.
     *
     * @param space_bytes   Amount of space, in bytes
     */
    public boolean reclaim(int space_bytes) {
            LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();
            localDataStorage.setCapacity(space_bytes);
            if(localDataStorage.getMemoryUsed() > localDataStorage.getCapacity()) {
                ReclaimProtocol reclaimProtocol = new ReclaimProtocol(systemStorage);
                reclaimProtocol.invoke();
            }
            return true;
    }

    public boolean fix() {
        boolean ret = chord.fix();
        ret &= main.fix();
        return ret;
    }

    public static class ServerSocketHandler implements Runnable {
        private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(20);
        private final Peer peer;
        private final ServerSocketChannel serverSocket;

        private final MessageFactory messageFactory;

        public ServerSocketHandler(Peer peer, ServerSocketChannel serverSocket) {
            this.peer = peer;
            this.serverSocket = serverSocket;

            messageFactory = new MessageFactory(peer);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    SocketChannel socket = serverSocket.accept();
                    ByteBuffer buffer = ByteBuffer.allocate(CHUNK_SIZE + MAX_HEADER_SIZE);
                    int size = socket.read(buffer);
                    byte[] data = new byte[size];
                    System.arraycopy(buffer.array(), 0, data, 0, size);
                    Message message = messageFactory.factoryMethod(data);
                    Message.Processor processor = message.getProcessor(peer, socket);
                    executor.execute(processor::invoke);
                } catch(AsynchronousCloseException e) {
                    System.out.println("Peer " + peer.id + ": Asynchronous close exception, exiting server socket handler");
                    return;
                } catch (Exception e) {
                    System.err.println("Peer " + peer.id + ": Exception in ServerSocketHandler cycle");
                    e.printStackTrace();
                }
            }
        }
    }
}
