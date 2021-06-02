import sdis.Peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.AlreadyBoundException;
import java.security.*;
import java.security.cert.CertificateException;

public class PeerDriver {
    public static void main(String[] args) throws IOException, AlreadyBoundException {
        if(args.length != 3 && args.length != 4){
            System.out.println("ERROR: wrong number of arguments");
            System.out.print(getUsage());
            return;
        }

        long key = Long.parseLong(args[0]);
        InetAddress ipAddress = InetAddress.getByName(args[2]);
        Peer peer = new Peer(62, key, ipAddress);
        peer.scheduleFixes();

        String serviceAccessPoint = args[1];
        peer.bindAsRemoteObject(serviceAccessPoint);

        if(args.length <= 3){
            peer.join();
        } else {
            String socketAddressString = args[5];
            String[] socketAddressSplit = socketAddressString.split(":");
            InetSocketAddress gateway = new InetSocketAddress(socketAddressSplit[0], Integer.parseInt(socketAddressSplit[1]));
            peer.join(gateway);
        }
    }

    private static String getUsage(){
        return
            "Usage:\n"+
            "    java PeerDriver PEER_ID SERVICE_ACCESS_POINT PEER_IP [IP:PORT]\n"+
            "    PEER_ID                 Peer ID\n"+
            "    SERVICE_ACCESS_POINT    Service RMI access point\n"+
            "    PEER_IP                 IP address that the current device is reachable from\n"+
            "    IP:PORT                 Socket address of a peer that already belongs to a chord\n"+
            "    \n"+
            "    If a socket address is not provided, the peer starts a new chord system.\n"
        ;
    }
}
