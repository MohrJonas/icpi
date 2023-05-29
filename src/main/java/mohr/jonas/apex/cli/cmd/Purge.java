package mohr.jonas.apex.cli.cmd;

import com.google.inject.Inject;
import mohr.jonas.apex.DistroboxAdapter;
import mohr.jonas.apex.cli.Spinner;
import mohr.jonas.apex.cli.Terminal;
import mohr.jonas.apex.data.Config;
import org.jetbrains.annotations.Nullable;

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
			terminal.error("!!! Running purge without a container name will delete all containers and applications installed within !!!");
			terminal.askForConfirmation("If that's really what you want, write 'YES, I REALLY WANT TO'", "YES, I REALLY WANT TO");
			Arrays.stream(config.containers()).forEach((template) -> adapter.removeContainer(template.name()));
			return 0;
		}
	}
}
