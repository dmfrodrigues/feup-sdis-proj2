package sdis.Modules.Main;

import sdis.Modules.ProtocolTask;
import sdis.Modules.SystemStorage.SystemStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

public class DeleteAccountProtocol extends ProtocolTask<Boolean> {

    private final Main main;
    private final UserMetadata userMetadata;

    public DeleteAccountProtocol(Main main, UserMetadata userMetadata){
        this.main = main;
        this.userMetadata = userMetadata;
    }

    @Override
    public Boolean compute() {
        SystemStorage systemStorage = main.getSystemStorage();
        Set<Main.Path> paths = userMetadata.getFiles();

        // Delete user metadata
        try {
            main.deleteFile(userMetadata.asFile());
        } catch (IOException e) {
            throw new CompletionException(e);
        }

        // Delete all files
        List<ProtocolTask<Boolean>> tasks = new ArrayList<>();
        for(Main.Path p : paths){
            Main.File f = userMetadata.getFile(p);
            tasks.add(new DeleteFileProtocol(main, f, 10));
        }

        invokeAll(tasks);
        boolean success = true;
        for(RecursiveTask<Boolean> task: tasks) {
            try {
                success &= task.get();
            } catch (InterruptedException | ExecutionException e) {
                success = false;
            }
        }

        return success;
    }
}
