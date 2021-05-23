import sdis.Modules.Main.Main;
import sdis.Modules.Main.Password;
import sdis.Modules.Main.Username;
import sdis.PeerInterface;
import sdis.Storage.ChunkIterator;
import sdis.Storage.ChunkOutput;
import sdis.Storage.FileChunkIterator;
import sdis.Storage.FileChunkOutput;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String[] args) {
        try {
            String peerAccessPoint = args[0];

            Registry registry = LocateRegistry.getRegistry();
            PeerInterface stub = (PeerInterface) registry.lookup(peerAccessPoint);

            Username username = new Username(args[1]);
            Password password = new Password(args[2]);
            String operation = args[3];

            switch (operation) {
                case "BACKUP": {
                    Path origin = Paths.get(args[4]);
                    ChunkIterator chunkIterator = new FileChunkIterator(origin.toFile(), Main.CHUNK_SIZE);
                    Main.Path destination = new Main.Path(args[5]);
                    int replicationDegree = Integer.parseInt(args[5]);
                    stub.backup(username, password, destination, replicationDegree, chunkIterator);
                    break;
                }
                case "RESTORE": {
                    Main.Path origin = new Main.Path(args[4]);
                    Path destination = Paths.get(args[5]);
                    ChunkOutput chunkOutput = new FileChunkOutput(destination.toFile());
                    stub.restore(username, password, origin, chunkOutput);
                    break;
                }
                case "DELETE": {
                    Main.Path origin = new Main.Path(args[4]);
                    stub.delete(username, password, origin);
                    break;
                }
                case "RECLAIM": {
                    int max_size = Integer.parseInt(args[4]);
                    stub.reclaim(max_size);
                    break;
                }
            }
        } catch(Throwable e){
            System.out.println(getUsage());
        }
    }

    private static String getUsage(){
        return
            "Usage:\n"+
            "    java TestApp <PEER_ACCESS_POINT> <USERNAME> <PASSWORD> <OPERATION>\n"+
            "    <PEER_ACCESS_POINT>     Name of the remote object providing the service\n"+
            "    <USERNAME>              User name\n"+
            "    <PASSWORD>              Password (in plain text)\n"+
            "    <OPERATION>             The operation to be executed: BACKUP, RESTORE, DELETE, RECLAIM or STATE\n"+
            "<OPERATION>: BACKUP <ORIGIN> <DESTINATION> <REPLICATION_DEGREE>\n"+
            "    <ORIGIN>                Local path of file to be backed-up\n"+
            "    <DESTINATION>           Path the file will have in the system\n"+
            "    <REPLICATION_DEGREE>    Replication degree of BACKUP\n"+
            "<OPERATION>: RESTORE <ORIGIN> <DESTINATION>\n"+
            "    <ORIGIN>                System path of the file we want to restore\n"+
            "    <DESTINATION>           Local path where the file will be saved to\n"+
            "<OPERATION>: DELETE <ORIGIN>\n"+
            "    <ORIGIN>                System path of the file we want to delete\n"+
            "<OPERATION>: RECLAIM <SPACE>\n"+
            "    <SPACE>                 The maximum amount of disk space (in byte) that the service can use.\n"
        ;
    }
}
