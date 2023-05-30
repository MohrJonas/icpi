package mohr.jonas.apex.cli.cmd;

import com.google.inject.Inject;
import mohr.jonas.apex.DistroboxAdapter;
import mohr.jonas.apex.cli.Spinner;
import mohr.jonas.apex.cli.Terminal;
import mohr.jonas.apex.data.Config;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Upgrade {

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
    public Upgrade(Logger logger) {
        this.logger = logger;
    }

    public int call(@Nullable String containerName, boolean verbose) {
        if (containerName != null) {
            spinner.spinUntilDone(String.format("Upgrading packages in container '%s'", containerName), CompletableFuture.supplyAsync(() -> {
                adapter.upgradeContainer(containerName);
                return null;
            }));
            terminal.success("Done");
        } else {
            Arrays.stream(config.containers()).forEach((template) -> {
                spinner.spinUntilDone(String.format("Upgrading container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
                    adapter.upgradeContainer(template.name());
                    return null;
                }));
                terminal.success("Done");
            });
        }
        return 0;
    }

}
