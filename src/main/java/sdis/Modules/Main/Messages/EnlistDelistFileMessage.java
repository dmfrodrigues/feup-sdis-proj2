package sdis.Modules.Main.Messages;

import sdis.Modules.Main.*;
import sdis.Modules.Message;
import sdis.Peer;
import sdis.Storage.DataBuilderChunkOutput;
import sdis.Utils.DataBuilder;

import java.io.IOException;
import java.net.Socket;

public abstract class EnlistDelistFileMessage extends MainMessage {
    protected static UserMetadata getUserMetadata(Main main, Username owner) throws IOException, ClassNotFoundException {
        // Get user metadata
        DataBuilder builder = new DataBuilder();
        DataBuilderChunkOutput chunkOutput = new DataBuilderChunkOutput(builder, 10);
        RestoreUserFileProtocol restoreUserFileProtocol = new RestoreUserFileProtocol(main, owner, chunkOutput, 10);
        if(!restoreUserFileProtocol.invoke()) return null;

        // Parse user metadata
        byte[] data = builder.get();
        return UserMetadata.deserialize(data);
    }

    protected static boolean replaceUserMetadata(Main main, UserMetadata userMetadata) throws IOException {
        Main.File userMetadataFile = userMetadata.asFile();

        // Delete old user metadata
        DeleteFileProtocol deleteFileProtocol = new DeleteFileProtocol(main, userMetadataFile, 10, false);
        if(!deleteFileProtocol.invoke()) return false;

        // Save new user metadata
        byte[] data = userMetadata.serialize();
        BackupFileProtocol backupFileProtocol = new BackupFileProtocol(main, userMetadataFile, data, false);
        if(!backupFileProtocol.invoke()) return false;

        return true;
    }
}
