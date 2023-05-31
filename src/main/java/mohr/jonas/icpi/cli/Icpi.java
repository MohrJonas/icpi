package mohr.jonas.icpi.cli;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import mohr.jonas.icpi.DistroboxAdapter;
import mohr.jonas.icpi.cli.cmd.*;
import mohr.jonas.icpi.data.Config;
import mohr.jonas.icpi.data.DB;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;

import java.util.concurrent.Callable;

//TODO move color printing into own function
//TODO add containerName validation
@CommandLine.Command(name = "icpi", mixinStandardHelpOptions = true, version = "1.0.0", description = "Alternative package manager to apx")
public class Icpi implements Callable<Integer> {

	private final Injector injector;
	@CommandLine.Option(names = {"-v", "--verbose"}, description = "Enable verbose output for debugging.", defaultValue = "false")
	@SuppressWarnings("unused")
	private boolean verbose;

	public Icpi(Config config, DB db) {
		this.injector = Guice.createInjector(new InjectorModule(config, db));
	}

	@CommandLine.Command(name = "init", description = "Initialize all the containers described in the config.", mixinStandardHelpOptions = true)
	public int init(@CommandLine.Option(names = {"-v", "--verbose"}, description = "Enable verbose output", defaultValue = "false") boolean verbose) {
		return injector.getInstance(Init.class).call(verbose);
	}

	@CommandLine.Command(name = "install", description = "Install a package.", mixinStandardHelpOptions = true)
	public int install(@CommandLine.Parameters(index = "0", description = "Name of the package to install") String packageName, @CommandLine.Option(names = {"-c", "--container"}, description = "Specify a container explicitly") String containerName) {
		return injector.getInstance(Install.class).call(containerName, verbose, packageName);
	}

	@CommandLine.Command(name = "update", description = "Update package repositories.", mixinStandardHelpOptions = true)
	public int update(@CommandLine.Option(names = {"-c", "--container"}, description = "Specify a container explicitly") String containerName) {
		return injector.getInstance(Update.class).call(containerName, verbose);
	}

	@CommandLine.Command(name = "upgrade", description = "Upgrade packages.", mixinStandardHelpOptions = true)
	public int containerUpgrade(@CommandLine.Option(names = {"-c", "--container"}, description = "Specify a container explicitly") String containerName) {
		return injector.getInstance(Upgrade.class).call(containerName, verbose);
	}

	@CommandLine.Command(name = "remove", description = "Remove packages.", mixinStandardHelpOptions = true)
	public int remove(
			@CommandLine.Parameters(index = "0", description = "Name of the package to install") String packageName,
			@CommandLine.Option(names = {"-c", "--container"}, description = "Specify a container explicitly") String containerName) {
		return injector.getInstance(Remove.class).call(containerName, verbose, packageName);
	}

	@CommandLine.Command(name = "purge", description = "Remove a container.", mixinStandardHelpOptions = true)
	public int purge(
			@CommandLine.Option(names = {"-c", "--container"}, description = "Specify a container explicitly") String containerName) {
		return injector.getInstance(Purge.class).call(containerName, verbose);
	}

	@Override
	public Integer call() {
		System.out.println("Icpi 1.0.0\nUse -h for a list of available commands");
		return 0;
	}

	@AllArgsConstructor
	private static final class InjectorModule extends AbstractModule {

		private final Config config;
		private final DB db;

		@SneakyThrows
		@Override
		protected void configure() {
			bind(Spinner.class).in(Scopes.SINGLETON);
			bind(Config.class).toInstance(config);
			bind(Terminal.class).toInstance(TerminalBuilder.builder().jna(true).build());
			bind(DB.class).toInstance(db);
			bind(DistroboxAdapter.class).in(Scopes.SINGLETON);
		}
	}
}
