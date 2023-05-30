package mohr.jonas.apex.cli.cmd;

import com.google.inject.Inject;
import lombok.val;
import mohr.jonas.apex.DistroboxAdapter;
import mohr.jonas.apex.cli.Spinner;
import mohr.jonas.apex.cli.Terminal;
import mohr.jonas.apex.data.Config;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Update {

    private final Logger logger;
    @Inject
    private DistroboxAdapter adapter;
    @Inject
    private Config config;
    @Inject
    private Spinner spinner;
    @Inject
    private Terminal terminal;

    @Inject
    public Update(Logger logger) {
        this.logger = logger;
    }

    public int call(@Nullable String containerName, boolean verbose) {
        if (containerName != null) {
            val template = Config.getByName(config, containerName).orElseFatal();
            spinner.spinUntilDone(String.format("Updating packages in container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
                adapter.updatePackageInContainer(containerName, template.update());
                return null;
            }));
            terminal.success("Done");
        } else {
            Arrays.stream(config.containers()).forEach((template) -> {
                spinner.spinUntilDone(String.format("Updating packages in container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
                    adapter.updatePackageInContainer(template.name(), template.update());
                    return null;
                }));
                terminal.success("Done");
            });
        }
        return 0;
    }

}
