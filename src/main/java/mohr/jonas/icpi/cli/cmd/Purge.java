package mohr.jonas.icpi.cli.cmd;

import com.google.inject.Inject;
import mohr.jonas.icpi.DistroboxAdapter;
import mohr.jonas.icpi.cli.Spinner;
import mohr.jonas.icpi.cli.TerminalUtils;
import mohr.jonas.icpi.data.Config;
import org.jetbrains.annotations.Nullable;
import org.jline.terminal.Terminal;

import java.util.Arrays;
import java.util.logging.Logger;

public class Purge {

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
    public Purge(Logger logger) {
        this.logger = logger;
    }

    public int call(@Nullable String name, boolean verbose) {
        if (name != null) {
            adapter.removeContainer(name);
            return 0;
        } else {
            terminalUtils.error(terminal, "⚠ Running purge without a container name will delete all containers and applications installed within ⚠");
            terminalUtils.askForConfirmation("If that's really what you want, write 'YES, I REALLY WANT TO'", "YES, I REALLY WANT TO: ", terminal);
            Arrays.stream(config.containers()).forEach((template) -> adapter.removeContainer(template.name()));
            return 0;
        }
    }
}
