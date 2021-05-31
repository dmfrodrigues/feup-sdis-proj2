package sdis.Modules.SystemStorage.Messages;

import sdis.Modules.Message;
import sdis.Modules.SystemStorage.SystemStorage;
import sdis.Peer;

import java.net.Socket;
import java.nio.channels.SocketChannel;

public abstract class SystemStorageMessage extends Message {
    public static abstract class Processor extends Message.Processor {
        private final SystemStorage systemStorage;
        private final Socket socket;

        public Processor(SystemStorage systemStorage, Socket socket){
            this.systemStorage = systemStorage;
            this.socket = socket;
        }

        public SystemStorage getSystemStorage(){
            return systemStorage;
        }

        public Socket getSocket(){
            return socket;
        }
    }

    public abstract SystemStorageMessage.Processor getProcessor(Peer peer, SocketChannel socket);
}
