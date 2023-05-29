package mohr.jonas.apex.cli.cmd;

import com.google.inject.Inject;
import mohr.jonas.apex.DistroboxAdapter;
import mohr.jonas.apex.cli.Spinner;
import mohr.jonas.apex.data.Config;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Init {

	@Inject
	private DistroboxAdapter adapter;
	@Inject
	private Config config;
	@Inject
	private Spinner spinner;

	private final Logger logger;

	@Inject
	public Init(Logger logger) {
		this.logger = logger;
	}

	public int call(boolean verbose) {
		Arrays.stream(config.containers()).forEach((template) -> {
			logger.log(Level.INFO, "Setting up container {0}", template.name());
			spinner.spinUntilDone(String.format("Setting up container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
				logger.log(Level.INFO, adapter.setupContainer(template.name(), template.image()));
				return null;
			}));
		});
		return 0;
	}
}
