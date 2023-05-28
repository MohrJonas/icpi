package mohr.jonas.apex.cli;

import com.google.inject.Inject;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import mohr.jonas.apex.DistroboxAdapter;
import mohr.jonas.apex.data.Config;
import org.apache.commons.lang3.math.NumberUtils;
import org.fusesource.jansi.Ansi;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.fusesource.jansi.Ansi.ansi;

//TODO move color printing into own function
//TODO add containerName validation
@CommandLine.Command(name = "apex", mixinStandardHelpOptions = true, version = "apex 1.0.0", description = "Alternative package manager to apx")
public class Apex implements Callable<Integer> {

	private final Logger logger;
	@SuppressWarnings("unused")
	@Inject
	private Config config;
	@SuppressWarnings("unused")
	@Inject
	private Spinner spinner;
	@SuppressWarnings("unused")
	@Inject
	private DistroboxAdapter adapter;
	@CommandLine.Option(names = {"-v", "--verbose"}, description = "Enable verbose output for debugging", defaultValue = "false")
	@SuppressWarnings("unused")
	private boolean verbose;

	@Inject
	public Apex(Logger logger) {
		this.logger = logger;
		logger.setLevel(Level.ALL);
	}

	@SuppressWarnings("unused")
	@SneakyThrows
	@CommandLine.Command(name = "init", description = "Initialize all the containers described in the config", mixinStandardHelpOptions = true)
	public Integer init(@CommandLine.Option(names = {"-v", "--verbose"}, description = "Enable verbose output", defaultValue = "false") boolean verbose) {
		Arrays.stream(config.containers()).forEach((template) -> {
			logger.log(Level.INFO, "Setting up container {0}", template.name());
			spinner.spinUntilDone(String.format("Setting up container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
				logger.log(Level.INFO, adapter.setupContainer(template.name(), template.image()));
				return null;
			}));
		});
		return 0;
	}

	@SuppressWarnings("unused")
	@CommandLine.Command(name = "install", description = "Install a package", mixinStandardHelpOptions = true)
	public Integer install(@CommandLine.Parameters(index = "0", description = "Name of the package to install") String packageName, @CommandLine.Option(names = {"-c", "--container"}, description = "Specify a container explicitly") String containerName) {
		if (containerName != null) {
			val template = Config.getByName(config, containerName).orElseFatal();
			spinner.spinUntilDone(String.format("Installing package '%s' in container '%s'", packageName, template.name()), CompletableFuture.supplyAsync(() -> {
				logger.log(Level.INFO, adapter.installPackageInContainer(containerName, template.install(), packageName));
				return null;
			}));
		} else {
			Arrays.stream(config.containers()).forEach((template) -> {
				System.out.println(ansi().fg(Ansi.Color.GREEN).a(String.format(">> Querying '%s'", template.name())).reset());
				spinner.spinUntilDone(String.format("Searching package '%s' in container '%s'", packageName, template.name()), CompletableFuture.supplyAsync(() -> {
					System.out.println(adapter.searchForPackageInContainer(template.name(), template.search(), packageName));
					return null;
				}));
			});
			System.out.println(ansi().fg(Ansi.Color.GREEN).a(String.format("Above are the available packages for query '%s' in all containers.\nSelect the index of the container you want to install from:", packageName)).reset());
			@Cleanup final Scanner scanner = new Scanner(System.in);
			int input;
			while (true) {
				System.out.print(">> ");
				val s = scanner.next();
				if (NumberUtils.isParsable(s)) {
					input = Integer.parseInt(s);
					if (input > 0 && input <= config.containers().length) break;
				}
			}
			val template = config.containers()[input - 1];
			spinner.spinUntilDone(String.format("Installing package '%s' in container '%s'", packageName, template.name()), CompletableFuture.supplyAsync(() -> {
				logger.log(Level.INFO, adapter.installPackageInContainer(template.name(), template.install(), packageName));
				return null;
			}));
		}
		return 0;
	}

	@SuppressWarnings("unused")
	@CommandLine.Command(name = "update", description = "Update package repositories", mixinStandardHelpOptions = true)
	public Integer update(@CommandLine.Option(names = {"-c", "--container"}, description = "Specify a container explicitly") String containerName) {
		if (containerName != null) {
			val template = Config.getByName(config, containerName).orElseFatal();
			spinner.spinUntilDone(String.format("Updating packages in container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
				logger.log(Level.INFO, adapter.updatePackageInContainer(containerName, template.update()));
				return null;
			}));
		} else {
			Arrays.stream(config.containers()).forEach((template) -> spinner.spinUntilDone(String.format("Updating packages in container '%s'", template.name()), CompletableFuture.supplyAsync(() -> {
				logger.log(Level.INFO, adapter.updatePackageInContainer(template.name(), template.update()));
				return null;
			})));
		}
		return 0;
	}

	@SuppressWarnings("unused")
	@CommandLine.Command(name = "upgrade", description = "Upgrade packages", mixinStandardHelpOptions = true)
	public Integer containerUpgrade(@CommandLine.Option(names = {"-c", "--container"}, description = "Specify a container explicitly") String containerName) {
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

	@Override
	public Integer call() {
		System.out.println("Apex 1.0.0\nUse -h for a list of available commands");
		return 0;
	}
}
