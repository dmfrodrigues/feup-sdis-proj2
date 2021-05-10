package sdis;

/*
import sdis.Protocols.DataStorage.ReclaimProtocol;
import sdis.Protocols.Main.BackupFileProtocol;
import sdis.Protocols.Main.DeleteFileProtocol;
import sdis.Protocols.Main.RestoreFileProtocol;
*/
import sdis.Modules.Chord.Chord;
import sdis.Modules.DataStorage.DataStorage;
import sdis.Modules.Message;
import sdis.Modules.ProtocolSupplier;
import sdis.Utils.Utils;

import java.io.File;
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
    private final Random random = new Random(System.currentTimeMillis());

    private final Chord.Key id;
    private final InetSocketAddress socketAddress;
    private final ServerSocket serverSocket;
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(100);
    private final Chord chord;
    private final DataStorage dataStorage;

    public Peer(int keySize, long id, InetAddress ipAddress) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(null);
        socketAddress = new InetSocketAddress(ipAddress, serverSocket.getLocalPort());

        System.out.println(
            "Starting peer " + id +
            " with address " + getSocketAddress()
        );

        Path storagePath = Paths.get(id + "/storage/data");
        chord = new Chord(getSocketAddress(), getExecutor(), keySize, id);
        dataStorage = new DataStorage(storagePath, getExecutor(), getChord());

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

        return getChord().join();
    }

    public CompletableFuture<Void> join(InetSocketAddress gateway){
        System.out.println("Peer " + getKey() + " joining a chord");

        return getChord().join(gateway, new ProtocolSupplier<>() {
            @Override
            public Void get() {
                return null;
            }
        });
    }

    public CompletableFuture<Void> leave(){
        System.out.println("Peer " + getKey() + " leaving its chord");

        return getChord().leave(new ProtocolSupplier<>() {
            @Override
            public Void get() {
                return null;
            }
        })
        .thenRun(() -> {
            assert(Utils.deleteRecursive(new File(id.toString())));
        })
        ;
    }

    public Chord getChord() {
        return chord;
    }

    public Random getRandom() {
        return random;
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

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    /**
     * Backup file specified by pathname, with a certain replication degree.
     *
     * @param pathname          Pathname of file to be backed up
     * @param replicationDegree Replication degree (number of copies of each file chunk over all machines in the network)
     */
    public void backup(String pathname, int replicationDegree) throws IOException {
        /*
        File file = new File(pathname);
        FileChunkIterator fileChunkIterator;
        try {
            fileChunkIterator = new FileChunkIterator(file);
        } catch (NoSuchFileException e) {
            System.err.println("File " + pathname + " not found");
            return;
        }
        getFileTable().insert(file.getName(), fileChunkIterator.getFileId(), fileChunkIterator.length());
        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(this, fileChunkIterator, replicationDegree);
        CompletableFuture.runAsync(backupFileProtocol);
         */
    }

    /**
     * Restore file specified by pathname.
     *
     * That file's chunks are retrieved from peers, assembled and then saved to the provided pathname.
     *
     * @param pathname  Pathname of file to be restored
     */
    public void restore(String pathname) throws IOException {
        /*
        RestoreFileProtocol callable = new RestoreFileProtocol(this, pathname);
        executor.submit(callable);
         */
    }

    /**
     * Delete file specified by pathname.
     *
     * @param pathname  Pathname of file to be deleted over all peers
     */
    public void delete(String pathname) {
        /*
        DeleteFileProtocol callable = new DeleteFileProtocol(this, pathname);
        executor.submit(callable);
         */
    }

    /**
     * Set space the peer may use to backup chunks from other machines.
     *
     * @param space_kb  Amount of space, in kilobytes (KB, K=1000)
     */
    public void reclaim(int space_kb) {
        /*
        ReclaimProtocol callable = new ReclaimProtocol(this, space_kb);
        executor.submit(callable);
         */
    }

    public static class ServerSocketHandler implements Runnable {
        private static final int BUFFER_LENGTH = 80000;

        private final Peer peer;
        private final ServerSocket serverSocket;

        private final MessageFactory messageFactory;

        public ServerSocketHandler(Peer peer, ServerSocket serverSocket) {
            this.peer = peer;
            this.serverSocket = serverSocket;

            messageFactory = new MessageFactory(peer);
        }

        public ServerSocket getServerSocket() {
            return serverSocket;
        }

        public Peer getPeer() {
            return peer;
        }

        @Override
        public void run() {
            byte[] buf = new byte[BUFFER_LENGTH];
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
