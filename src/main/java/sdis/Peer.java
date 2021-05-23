package sdis;

import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.DataStorage.GetRedirectsProtocol;
import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.Main.*;
import sdis.Modules.Message;
import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.ReclaimProtocol;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Storage.ChunkIterator;
import sdis.Storage.ChunkOutput;
import sdis.Utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

public class Peer implements PeerInterface {

    private final Chord.Key id;
    private final Path baseStoragePath;
    private final InetSocketAddress socketAddress;
    private final ServerSocket serverSocket;
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(100);
    private final Chord chord;
    private final DataStorage dataStorage;
    private final SystemStorage systemStorage;
    private final Main main;

    public Peer(int keySize, long id, InetAddress ipAddress) throws IOException {
        this(keySize, id, ipAddress, Paths.get("."));
    }

    public Peer(int keySize, long id, InetAddress ipAddress, Path baseStoragePath) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(null);
        socketAddress = new InetSocketAddress(ipAddress, serverSocket.getLocalPort());

        System.out.println(
            "Starting peer " + id +
            " with address " + getSocketAddress()
        );

        this.baseStoragePath = Paths.get(baseStoragePath.toString(), Long.toString(id));
        chord = new Chord(getSocketAddress(), executor, keySize, id);
        dataStorage = new DataStorage(Paths.get(this.baseStoragePath.toString(), "storage/data"), executor, getChord());
        systemStorage = new SystemStorage(chord, dataStorage, executor);
        main = new Main(systemStorage, executor);

        this.id = chord.newKey(id);

        ServerSocketHandler serverSocketHandler = new ServerSocketHandler(this, serverSocket);
        Thread serverSocketHandlerThread = new Thread(serverSocketHandler);
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

    public CompletableFuture<Void> join(){
        System.out.println("Peer " + getKey() + " creating a new chord");

        return chord.join();
    }

    public CompletableFuture<Void> join(InetSocketAddress gateway){
        System.out.println("Peer " + getKey() + " joining a chord");

        return chord.join(gateway, new ProtocolSupplier<>() {
            @Override
            public Void get() {
                // Get redirects
                GetRedirectsProtocol getRedirectsProtocol = new GetRedirectsProtocol(chord, dataStorage);
                Set<UUID> redirects = getRedirectsProtocol.get();
                for(UUID id : redirects)
                    dataStorage.registerSuccessorStored(id);

                // Move keys

                return null;
            }
        });
    }

    public CompletableFuture<Void> leave(){
        System.out.println("Peer " + getKey() + " leaving its chord");

        return chord.leave(new ProtocolSupplier<>() {
            @Override
            public Void get() {
                return null;
            }
        })
        .thenRun(() -> {
            assert(Utils.deleteRecursive(baseStoragePath.toFile()));
        });
    }

    public static ScheduledExecutorService getExecutor(){
        return executor;
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

    /**
     * Backup file.
     */
    public void backup(Username username, Password password, Main.Path path, int replicationDegree, ChunkIterator chunkIterator) {
        try {
            UserMetadata userMetadata = authenticate(username, password);
            if(userMetadata == null){
                System.err.println("Failed to authenticate");
                return;
            }

            Main.File file = new Main.File(username, path, chunkIterator.length(), replicationDegree);

            main.backupFile(file, chunkIterator).get();

        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restore file.
     */
    public void restore(Username username, Password password, Main.Path path, ChunkOutput chunkOutput) {
        try {
            UserMetadata userMetadata = authenticate(username, password);
            if(userMetadata == null){
                System.err.println("Failed to authenticate");
                return;
            }

            Main.File file = userMetadata.getFile(path);
            if(file == null){
                System.err.println("No such file with path " + path);
                Set<Main.Path> files = userMetadata.getFiles();
                System.err.println("Available files (" + files.size() + "):");
                for(Main.Path f: files){
                    System.err.println("    " + f);
                }
                return;
            }

            main.restoreFile(file, chunkOutput).get();

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete file.
     */
    public void delete(Username username, Password password, Main.Path path) {
        try {
            UserMetadata userMetadata = authenticate(username, password);
            if(userMetadata == null){
                System.err.println("Failed to authenticate");
                return;
            }

            Main.File file = userMetadata.getFile(path);
            if(file == null){
                System.err.println("No such file with path " + path);
                Set<Main.Path> files = userMetadata.getFiles();
                System.err.println("Available files (" + files.size() + "):");
                for(Main.Path f: files){
                    System.err.println("    " + f);
                }
                return;
            }

            main.deleteFile(file).get();

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set space the peer may use to backup chunks from other machines.
     *
     * @param space_bytes   Amount of space, in bytes
     */
    public void reclaim(int space_bytes) {
        try {
            LocalDataStorage localDataStorage = dataStorage.getLocalDataStorage();
            localDataStorage.setCapacity(space_bytes);
            if(localDataStorage.getMemoryUsed().get() > localDataStorage.getCapacity()) {
                ReclaimProtocol reclaimProtocol = new ReclaimProtocol(systemStorage);
                reclaimProtocol.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public UserMetadata authenticate(Username username, Password password) {
        AuthenticationProtocol authenticationProtocol = new AuthenticationProtocol(main, username, password);
        return authenticationProtocol.get();
    }

    public static class ServerSocketHandler implements Runnable {
        private final Peer peer;
        private final ServerSocket serverSocket;

        private final MessageFactory messageFactory;

        public ServerSocketHandler(Peer peer, ServerSocket serverSocket) {
            this.peer = peer;
            this.serverSocket = serverSocket;

            messageFactory = new MessageFactory(peer);
        }

        public Peer getPeer() {
            return peer;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    InputStream is = socket.getInputStream();
                    byte[] data = is.readAllBytes();
                    Message message = messageFactory.factoryMethod(data);
                    Message.Processor processor = message.getProcessor(peer, socket);
                    CompletableFuture.supplyAsync(processor, executor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
