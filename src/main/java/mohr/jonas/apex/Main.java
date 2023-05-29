package mohr.jonas.apex;

import lombok.SneakyThrows;
import lombok.val;
import mohr.jonas.apex.cli.Apex;
import mohr.jonas.apex.data.Config;
import mohr.jonas.apex.data.DB;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

	@SneakyThrows
	public static void main(String[] args) {
		Files.createDirectories(Path.of(System.getProperty("user.home"), ".local", "apex", "bin"));
		AnsiConsole.systemInstall();
		val config = Config.readFromDefaultLocations().orElseFatal();
		val db = DB.load();
		Runtime.getRuntime().addShutdownHook(new Thread(db::save));
		val apex = new Apex(config, db);
		val commandLine = new CommandLine(apex);
		System.exit(commandLine.execute(args));
	}
}