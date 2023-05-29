package mohr.jonas.apex.cli.cmd;

import com.google.inject.Inject;
import mohr.jonas.apex.DistroboxAdapter;
import mohr.jonas.apex.cli.Spinner;
import mohr.jonas.apex.data.Config;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
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
	public Upgrade(Logger logger) {
		this.logger = logger;
	}

	public int call(@Nullable String containerName, boolean verbose) {
		if (containerName != null) {
			spinner.spinUntilDone(String.format("Upgrading container '%s'", containerName), CompletableFuture.supplyAsync(() -> {
				logger.log(Level.INFO, adapter.upgradeContainer(containerName));
				return null;
			}));
		} else {
			Arrays.stream(config.containers()).forEach((template) -> spinner.spinUntilDone(String.format("Upgrading container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
				logger.log(Level.INFO, adapter.upgradeContainer(template.name()));
				return null;
			})));
		}
		return 0;
	}

}
