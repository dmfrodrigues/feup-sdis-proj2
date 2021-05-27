package sdis.Modules.Main.Messages;

import sdis.Modules.Main.Main;
import sdis.Modules.Main.RestoreUserFileProtocol;
import sdis.Modules.Message;
import sdis.Peer;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;

import java.net.Socket;

public abstract class MainMessage extends Message {
    public static abstract class Processor extends Message.Processor {
        private final Main main;
        private final Socket socket;

        public Processor(Main main, Socket socket){
            this.main = main;
            this.socket = socket;
        }

        public Main getMain(){
            return main;
        }

        public Socket getSocket(){
            return socket;
        }
    }

    public abstract MainMessage.Processor getProcessor(Peer peer, Socket socket);
}
