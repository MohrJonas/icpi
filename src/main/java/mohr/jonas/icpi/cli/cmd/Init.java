package mohr.jonas.icpi.cli.cmd;

import com.google.inject.Inject;
import mohr.jonas.icpi.DistroboxAdapter;
import mohr.jonas.icpi.cli.Spinner;
import mohr.jonas.icpi.cli.TerminalUtils;
import mohr.jonas.icpi.data.Config;
import org.jline.terminal.Terminal;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Init {

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
    public Init(Logger logger) {
        this.logger = logger;
    }

    public int call(boolean verbose) {
        Arrays.stream(config.containers()).forEach((template) -> {
            spinner.spinUntilDone(String.format("Setting up container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
                adapter.setupContainer(template.name(), template.image(), template.setup());
                return null;
            }));
            terminalUtils.success(terminal, "Done");
        });
        return 0;
    }
}
