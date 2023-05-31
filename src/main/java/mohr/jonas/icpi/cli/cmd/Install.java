package mohr.jonas.icpi.cli.cmd;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.val;
import mohr.jonas.icpi.DistroboxAdapter;
import mohr.jonas.icpi.ExportType;
import mohr.jonas.icpi.cli.Spinner;
import mohr.jonas.icpi.cli.Terminal;
import mohr.jonas.icpi.data.Config;
import mohr.jonas.icpi.data.DB;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.fusesource.jansi.Ansi.ansi;

public class Install {

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
    public Install(Logger logger) {
        this.logger = logger;
    }

    public int call(@Nullable String containerName, boolean verbose, String packageName) {
        if (containerName != null) {
            val template = Config.getByName(config, containerName).orElseFatal();
            val beforeBinaries = adapter.getBinariesInContainer(containerName);
            spinner.spinUntilDone(String.format("Installing package '%s' in container '%s'", packageName, template.name()), CompletableFuture.supplyAsync(() -> {
                adapter.installPackageInContainer(containerName, template.install(), packageName);
                return null;
            }));
            db.addPackage(containerName, packageName);
            val afterBinaries = adapter.getBinariesInContainer(containerName);
            val deltaBinaries = ImmutableSet.copyOf(ArrayUtils.removeElements(afterBinaries, beforeBinaries));
            deltaBinaries.forEach((binary) -> askForExport(containerName, binary));
        } else {
            val outputs = new String[config.containers().length];
            for (int i = 0; i < config.containers().length; i++) {
                val template = config.containers()[i];
                int finalI = i;
                spinner.spinUntilDone(String.format("Querying package '%s' in container '%s'", packageName, template.name()), CompletableFuture.supplyAsync(() -> {
                    outputs[finalI] = adapter.searchForPackageInContainer(template.name(), template.search(), packageName);
                    return null;
                }));
                terminal.success("Done");
            }
            for (int i = 0; i < config.containers().length; i++) {
                System.out.println(ansi().fgBrightCyan().a(String.format("(%d) %s", i + 1, config.containers()[i].name())).reset());
                System.out.println(outputs[i]);
            }
            val selection = terminal.askForNumber(String.format("Above are the available packages for query '%s' in all containers.\nSelect the index of the container you want to install from:", packageName), 1, config.containers().length);
            val template = config.containers()[selection - 1];
            val beforeBinaries = adapter.getBinariesInContainer(template.name());
            spinner.spinUntilDone(ansi().fgBrightCyan().a(String.format("Installing package '%s'", packageName)).reset().toString(), CompletableFuture.supplyAsync(() -> {
                adapter.installPackageInContainer(template.name(), template.install(), packageName);
                return null;
            }));
            terminal.success("Installed %s", packageName);
            db.addPackage(template.name(), packageName);
            val afterBinaries = adapter.getBinariesInContainer(template.name());
            val deltaBinaries = ImmutableSet.copyOf(ArrayUtils.removeElements(afterBinaries, beforeBinaries));
            deltaBinaries.forEach((binary) -> askForExport(template.name(), binary));
        }
        return 0;
    }

    @SneakyThrows
    private void askForExport(String name, String binary) {
        if (collidesWithHostBinary(binary))
            terminal.warning("Binary %s will conflict with host binary", binary);
        if (terminal.askForBoolean(String.format(">> Export binary '%s'? (y/n)", binary), "y", "n"))
            adapter.exportFromContainer(name, ExportType.BINARY, binary);
    }

    @SneakyThrows
    private boolean collidesWithHostBinary(String binary) {
        val process = new ProcessBuilder()
                .command("bash", "-c", "\"compgen -c\"")
                .start();
        process.waitFor();
        val hostBinaries = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim().split("\n");
        return ArrayUtils.contains(hostBinaries, binary);
    }
}
