package mohr.jonas.apex;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import mohr.jonas.apex.cli.Apex;
import mohr.jonas.apex.cli.Spinner;
import mohr.jonas.apex.data.Config;
import mohr.jonas.apex.util.Failable;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

public class Main {

	@SneakyThrows
	public static void main(String[] args) {
		AnsiConsole.systemInstall();
		final Failable<Config> config = Config.readFromDefaultLocations();
		config.orElseFatal();
		final Injector inject = Guice.createInjector(new Module(config.getResult()));
		final CommandLine commandLine = new CommandLine(inject.getInstance(Apex.class));
		System.exit(commandLine.execute(args));
	}

	@AllArgsConstructor
	private static final class Module extends AbstractModule {

		private final Config config;

		@Override
		protected void configure() {
			bind(Spinner.class).toInstance(new Spinner());
			bind(DistroboxAdapter.class);
			bind(Config.class).toInstance(config);
		}
	}
}