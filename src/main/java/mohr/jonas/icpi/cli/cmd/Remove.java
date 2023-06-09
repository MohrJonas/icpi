package mohr.jonas.icpi.cli.cmd;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import lombok.val;
import mohr.jonas.icpi.DistroboxAdapter;
import mohr.jonas.icpi.ExportType;
import mohr.jonas.icpi.cli.Spinner;
import mohr.jonas.icpi.cli.TerminalUtils;
import mohr.jonas.icpi.data.Config;
import mohr.jonas.icpi.data.DB;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Nullable;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStyle;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class Remove {

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
	private DB db;
	@Inject
	private Terminal terminal;

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
					terminalUtils.error(terminal, "Unable to find package automatically. Consider defining the container explicitly with the -c option");
					return -1;
				}
				case 1 -> {
					return call(packageName, possibleContainerNames.get(0).getKey());
				}
				default -> {
					for (int i = 0; i < possibleContainerNames.size(); i++) {
						System.out.println(terminalUtils.coloredString(terminal, AttributedStyle.CYAN, "(%d) %s", i, possibleContainerNames.get(i).getKey()));
					}
					val selection = terminalUtils.askForNumber("The package appears to be installed in multiple containers. Select the container you want to remove from: ", 1, possibleContainerNames.size(), terminal);
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
		terminalUtils.success(terminal, "Removed %s", packageName);
		val afterBinaries = adapter.getBinariesInContainer(name);
		val deltaBinaries = ImmutableSet.copyOf(ArrayUtils.removeElements(beforeBinaries, afterBinaries));
		if (!deltaBinaries.isEmpty()) {
			terminal.writer().println(terminalUtils.coloredString(terminal, AttributedStyle.CYAN, "Select binaries to unexport:"));
			val toUnexport = terminalUtils.askForMultiSelection(terminal, deltaBinaries.stream().map((s) -> new ImmutablePair<>(s, true)).toList(), AttributedStyle.RED);
			toUnexport.forEach((binary) -> adapter.unexportFromContainer(template.name(), ExportType.BINARY, binary));
		}
		//deltaBinaries.forEach((binary) -> askForRemoval(name, binary));
		return 0;
	}

	private void askForRemoval(String name, String binary) {
		if (terminalUtils.askForBoolean(String.format("Binary '%s' is no longer available in container '%s'. Remove from host?: ", binary, name), "y", "n", terminal))
			adapter.unexportFromContainer(name, ExportType.BINARY, binary);
	}

}
