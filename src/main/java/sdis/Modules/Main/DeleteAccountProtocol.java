package sdis.Modules.Main;

import sdis.Modules.ProtocolSupplier;
import sdis.Modules.SystemStorage.SystemStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class DeleteAccountProtocol extends ProtocolSupplier<Boolean> {

    private final Main main;
    private final UserMetadata userMetadata;

    public DeleteAccountProtocol(Main main, UserMetadata userMetadata){
        this.main = main;
        this.userMetadata = userMetadata;
    }

    @Override
    public Boolean get() {
        SystemStorage systemStorage = main.getSystemStorage();
        Set<Main.Path> paths = userMetadata.getFiles();

        // Delete user metadata
        try {
            main.deleteFile(userMetadata.asFile());
        } catch (IOException e) {
            throw new CompletionException(e);
        }

        // Delete all files
        List<CompletableFuture<Boolean>> futuresList = new ArrayList<>();
        for(Main.Path p : paths){
            Main.File f = userMetadata.getFile(p);
            DeleteFileProtocol deleteFileProtocol = new DeleteFileProtocol(main, f, 10);
            futuresList.add(CompletableFuture.supplyAsync(deleteFileProtocol, main.getExecutor()));
        }
        boolean success = true;
        for(CompletableFuture<Boolean> f : futuresList){
            try {
                success &= f.get();
            } catch (InterruptedException | ExecutionException e) {
                success = false;
            }
        }

        return success;
    }
}
