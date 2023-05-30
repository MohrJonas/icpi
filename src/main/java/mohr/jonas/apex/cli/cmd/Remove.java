package mohr.jonas.apex.cli.cmd;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import lombok.val;
import mohr.jonas.apex.DistroboxAdapter;
import mohr.jonas.apex.ExportType;
import mohr.jonas.apex.cli.Spinner;
import mohr.jonas.apex.cli.Terminal;
import mohr.jonas.apex.data.Config;
import mohr.jonas.apex.data.DB;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.fusesource.jansi.Ansi.ansi;

public class Remove {

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
    private DB db;

    @Inject
    public Remove(Logger logger) {
        this.logger = logger;
    }

    public int call(@Nullable String containerName, boolean verbose, String packageName) {
        if (containerName != null) {
            return call(packageName, containerName);
        } else {
            val possibleContainerNames = db.getPackages().entrySet().stream().filter((entry) -> entry.getValue().contains(packageName)).toList();
            switch (possibleContainerNames.size()) {
                case 0 -> {
                    terminal.error("Unable to find package automatically. Consider defining the container explicitly with the -c option");
                    return -1;
                }
                case 1 -> {
                    return call(packageName, possibleContainerNames.get(0).getKey());
                }
                default -> {
                    for (int i = 0; i < possibleContainerNames.size(); i++) {
                        System.out.println(ansi().fgBrightCyan().a(String.format("(%d) %s", i, possibleContainerNames.get(i).getKey())));
                    }
                    val selection = terminal.askForNumber("The package appears to be installed in multiple containers. Select the container you want to remove from", 1, possibleContainerNames.size());
                    return call(packageName, possibleContainerNames.get(selection - 1).getKey());
                }
            }
        }
    }

    private int call(String packageName, String name) {
        val template = Config.getByName(config, name).orElseFatal();
        val beforeBinaries = adapter.getBinariesInContainer(name);
        spinner.spinUntilDone(String.format("Removing package '%s' from container '%s'", packageName, name), CompletableFuture.supplyAsync(() -> {
            adapter.removePackageFromContainer(name, template.remove(), packageName);
            return null;
        }));
        db.removePackage(name, packageName);
        terminal.success("Removed %s", packageName);
        val afterBinaries = adapter.getBinariesInContainer(name);
        val deltaBinaries = ImmutableSet.copyOf(ArrayUtils.removeElements(beforeBinaries, afterBinaries));
        deltaBinaries.forEach((binary) -> askForRemoval(name, binary));
        return 0;
    }

    private void askForRemoval(String name, String binary) {
        if (terminal.askForBoolean(String.format("Binary '%s' is no longer available in container '%s'. Remove from host?", binary, name), "y", "n"))
            adapter.unexportFromContainer(name, ExportType.BINARY, binary);
    }

}
