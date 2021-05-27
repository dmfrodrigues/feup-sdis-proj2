package sdis.Modules.Main;

import sdis.Modules.ProtocolTask;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

public abstract class MainProtocolTask<T> extends ProtocolTask<T> {
    protected boolean reduceTasks(Collection<RecursiveTask<Boolean>> tasks){
        return tasks.stream().map((RecursiveTask<Boolean> task) -> {
            try {
                return task.get();
            } catch (InterruptedException | ExecutionException e) {
                return false;
            }
        }).reduce((Boolean a, Boolean b) -> a && b).orElse(true);
    }
    protected boolean invokeAndReduceTasks(Collection<RecursiveTask<Boolean>> tasks) {
        invokeAll(tasks);
        return reduceTasks(tasks);
    }
}
