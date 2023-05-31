package mohr.jonas.icpi.cli.cmd;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.val;
import mohr.jonas.icpi.DistroboxAdapter;
import mohr.jonas.icpi.ExportType;
import mohr.jonas.icpi.cli.Spinner;
import mohr.jonas.icpi.cli.TerminalUtils;
import mohr.jonas.icpi.data.Config;
import mohr.jonas.icpi.data.DB;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Nullable;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStyle;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Install {

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
				terminalUtils.success(terminal, "Done");
			}
			for (int i = 0; i < config.containers().length; i++) {
				System.out.println(terminalUtils.coloredString(terminal, AttributedStyle.CYAN, "(%d) %s", i + 1, config.containers()[i].name()));
				System.out.println(outputs[i]);
			}
			val selection = terminalUtils.askForNumber(String.format("Above are the available packages for query '%s' in all containers.\nSelect the index of the container you want to install from: ", packageName), 1, config.containers().length, terminal);
			val template = config.containers()[selection - 1];
			val beforeBinaries = adapter.getBinariesInContainer(template.name());
			spinner.spinUntilDone(terminalUtils.coloredString(terminal, AttributedStyle.CYAN, "Installing package '%s'", packageName), CompletableFuture.supplyAsync(() -> {
				adapter.installPackageInContainer(template.name(), template.install(), packageName);
				return null;
			}));
			terminalUtils.success(terminal, "Installed %s", packageName);
			db.addPackage(template.name(), packageName);
			val afterBinaries = adapter.getBinariesInContainer(template.name());
			val deltaBinaries = ImmutableSet.copyOf(ArrayUtils.removeElements(afterBinaries, beforeBinaries));
			if (!deltaBinaries.isEmpty()) {
				terminal.writer().println(terminalUtils.coloredString(terminal, AttributedStyle.CYAN, "Select binaries to export:"));
				val toExport = terminalUtils.askForMultiSelection(terminal, deltaBinaries.stream().map((s) -> new ImmutablePair<>(s, s.equals(packageName))).toList(), AttributedStyle.GREEN);
				toExport.forEach((binary) -> adapter.exportFromContainer(template.name(), ExportType.BINARY, binary));
			}
		}
		return 0;
	}

	@SneakyThrows
	private void askForExport(String name, String binary) {
		if (collidesWithHostBinary(binary))
			terminalUtils.warning(terminal, "Binary %s will conflict with host binary", binary);
		if (terminalUtils.askForBoolean(String.format(">> Export binary '%s'? (y/n): ", binary), "y", "n", terminal))
			adapter.exportFromContainer(name, ExportType.BINARY, binary);
	}

	@SneakyThrows
	private boolean collidesWithHostBinary(String binary) {
		val process = new ProcessBuilder().command("bash", "-c", "\"compgen -c\"").start();
		process.waitFor();
		val hostBinaries = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim().split("\n");
		return ArrayUtils.contains(hostBinaries, binary);
	}
}
