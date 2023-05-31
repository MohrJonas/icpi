package mohr.jonas.icpi.cli.cmd;

import com.google.inject.Inject;
import mohr.jonas.icpi.DistroboxAdapter;
import mohr.jonas.icpi.cli.Spinner;
import mohr.jonas.icpi.cli.TerminalUtils;
import mohr.jonas.icpi.data.Config;
import org.jetbrains.annotations.Nullable;
import org.jline.terminal.Terminal;

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
    private TerminalUtils terminalUtils;
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
            terminalUtils.success(terminal, "Done");
        } else {
            Arrays.stream(config.containers()).forEach((template) -> {
                spinner.spinUntilDone(String.format("Upgrading container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
                    adapter.upgradeContainer(template.name());
                    return null;
                }));
                terminalUtils.success(terminal, "Done");
            });
        }
        return 0;
    }

}
