package mohr.jonas.icpi;

import lombok.SneakyThrows;
import lombok.val;
import mohr.jonas.icpi.cli.Icpi;
import mohr.jonas.icpi.data.Config;
import mohr.jonas.icpi.data.DB;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        Files.createDirectories(Path.of(System.getProperty("user.home"), ".local", "icpi", "bin"));
        AnsiConsole.systemInstall();
        val config = Config.readFromDefaultLocations().orElseFatal();
        val db = DB.load();
        Runtime.getRuntime().addShutdownHook(new Thread(db::save));
        val icpi = new Icpi(config, db);
        val commandLine = new CommandLine(icpi);
        System.exit(commandLine.execute(args));
    }
}