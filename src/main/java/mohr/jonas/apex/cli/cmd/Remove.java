package mohr.jonas.apex.cli.cmd;

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

import java.util.Arrays;
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
			val assumedContainerName = db
					.getPackages().
					entrySet()
					.stream()
					.filter((entry) -> entry.getValue().contains(packageName))
					.findAny()
					.get()
					.getKey();
			return call(packageName, assumedContainerName);
		}
	}

	private int call(String packageName, String assumedContainerName) {
		val template = Config.getByName(config, assumedContainerName).orElseFatal();
		val beforeBinaries = adapter.getBinariesInContainer(assumedContainerName);
		spinner.spinUntilDone(String.format("Removing package '%s' in container '%s'", packageName, assumedContainerName), CompletableFuture.supplyAsync(() -> {
			adapter.removePackageFromContainer(assumedContainerName, template.remove(), packageName);
			return null;
		}));
		db.removePackage(assumedContainerName, packageName);
		val afterBinaries = adapter.getBinariesInContainer(assumedContainerName);
		val deltaBinaries = ArrayUtils.removeElements(beforeBinaries, afterBinaries);
		Arrays.stream(deltaBinaries).forEach((binary) -> askForRemoval(assumedContainerName, binary));
		return 0;
	}

	private void askForRemoval(String name, String binary) {
		if (terminal.askForBoolean(String.format("Binary '%s' is no longer available in container '%s'. Remove from host?", binary, name), "y", "n"))
			adapter.unexportFromContainer(name, ExportType.BINARY, binary);
	}

}
