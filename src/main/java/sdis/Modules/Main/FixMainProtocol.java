package sdis.Modules.Main;

import sdis.Modules.DataStorage.LocalDataStorage;
import sdis.Modules.Main.Messages.AccountMessage;
import sdis.Modules.ProtocolTask;
import sdis.UUID;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FixMainProtocol extends MainProtocolTask<Boolean> {

    private final Main main;

    public FixMainProtocol(Main main){
        this.main = main;
    }

    private ProtocolTask<Boolean> fixUserFiles(Username username){
        return new ProtocolTask<>() {
            @Override
            protected Boolean compute() {
                try {
                    UserMetadata userMetadata = AccountMessage.getUserMetadata(main, username);
                    if (userMetadata == null) return false;

                    List<ProtocolTask<Boolean>> tasks = userMetadata.getFiles().stream()
                    .map(userMetadata::getFile)
                    .map((Main.File file) -> new ProtocolTask<Boolean>() {
                        @Override
                        protected Boolean compute() {
                            return new FixFileProtocol(main, file).invoke();
                        }
                    }).collect(Collectors.toList());

                    return invokeAndReduceTasks(tasks);
                } catch (IOException | ClassNotFoundException e) {
                    return false;
                }
            }
        };
    }

    @Override
    protected Boolean compute() {
        Pattern userMetadataPattern = Pattern.compile("(" + Username.REGEX + ")-[0-9]+-[0-9]+");
        LocalDataStorage localDataStorage = main.getSystemStorage().getDataStorage().getLocalDataStorage();

        // Get usernames
        Set<UUID> uuids = localDataStorage.getAll();
        Set<Username> usernames = new HashSet<>();
        for(UUID id: uuids){
            Matcher matcher = userMetadataPattern.matcher(id.toString());
            if(matcher.matches()) usernames.add(new Username(matcher.group(1)));
        }

        // Get metadata files
        List<ProtocolTask<Boolean>> tasks = usernames.stream().map(this::fixUserFiles).collect(Collectors.toList());
        return invokeAndReduceTasks(tasks);
    }
}
