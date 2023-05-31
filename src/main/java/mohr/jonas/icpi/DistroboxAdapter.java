package mohr.jonas.icpi;

import com.google.inject.Inject;
import com.hubspot.jinjava.Jinjava;
import lombok.SneakyThrows;
import lombok.val;
import mohr.jonas.icpi.data.Container;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DistroboxAdapter {

    private final Logger logger;

    @Inject
    public DistroboxAdapter(Logger logger) {
        this.logger = logger;
    }

    @SneakyThrows
    private static String runCommand(String... command) {
        val process = new ProcessBuilder().command(command).start();
        process.waitFor();
        if (process.exitValue() == 0) return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim();
        System.err.printf("An error occurred while running '%s': %s\nPlease consider filing a bug report if this is unexpected\n", Arrays.toString(command), IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8).trim());
        System.exit(-1);
        return null;
    }

    private String deescalatePrivilege(String command) {
        return command.replace("sudo", "").replace("doas", "").trim();
    }

    public List<Container> listContainers() throws RuntimeException {
        val output = runCommand("distrobox-list", "--no-color");
        val lines = ArrayUtils.remove(output.split("\n"), 0);
        return Arrays.stream(lines).map((line) -> {
            val parts = line.split("\\|");
            return new Container(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
        }).toList();
    }

    public String installPackageInContainer(String name, String installCommandTemplate, String... packages) {
        val jinja = new Jinjava();
        val context = Map.of("packages", packages);
        return runCommand("distrobox-enter", "--name", name, "--no-tty", "--", jinja.render(installCommandTemplate, context));
    }

    public String searchForPackageInContainer(String name, String searchCommandTemplate, String... packages) {
        val jinja = new Jinjava();
        val context = Map.of("packages", packages);
        return runCommand("distrobox-enter", "--name", name, "--no-tty", "--", jinja.render(searchCommandTemplate, context));
    }

    public String setupContainer(String name, String image, String setupCommand) {
        return runCommand("distrobox-create", "--image", image, "--name", name, "--yes", "--pre-init-hooks", setupCommand);
    }

    public String upgradeContainer(String name) {
        return runCommand("distrobox-upgrade", name);
    }

    public String updatePackageInContainer(String name, String updateCommandTemplate) {
        return runCommand("distrobox-enter", "--name", name, "--no-tty", "--", updateCommandTemplate);
    }

    public String upgradePackageInContainer(String name, String upgradeCommandTemplate) {
        return runCommand("distrobox-enter", "--name", name, "--no-tty", "--", upgradeCommandTemplate);
    }

    public String removePackageFromContainer(String name, String removeCommandTemplate, String... packages) {
        val jinja = new Jinjava();
        val context = Map.of("packages", packages);
        return runCommand("distrobox-enter", "--name", name, "--no-tty", "--", jinja.render(removeCommandTemplate, context));
    }

    public String removeContainer(String name) {
        return runCommand("distrobox-rm", "-f", "--rm-home", name);
    }

    public String[] getBinariesInContainer(String name) {
        return runCommand("distrobox-enter", "-n", name, "-T", "--", "bash -c \"compgen -c\"").split("\n");
    }

    public String getAbsoluteBinaryPath(String name, String binary) {
        return runCommand("distrobox-enter", "-n", name, "-T", "--", "which", binary);
    }

    public String[] getDesktopEntriesInContainer(String name) {
        //TODO
        return new String[0];
    }

    @SneakyThrows
    public String exportFromContainer(String name, ExportType type, String toExport) {
        val absoluteBinaryPath = getAbsoluteBinaryPath(name, toExport);
        switch (type) {
            case APP -> {
                return runCommand("distrobox-enter", "-n", name, "-T", "--", "distrobox-export -a " + absoluteBinaryPath + " -ep ~/.local/icpi/bin");
            }
            case BINARY -> {
                return runCommand("distrobox-enter", "-n", name, "-T", "--", "distrobox-export -b " + absoluteBinaryPath + " -ep ~/.local/icpi/bin");
            }
        }
        return null;
    }

    @SneakyThrows
    public String unexportFromContainer(String name, ExportType type, String binary) {
        val parts = binary.split("/");
        val binaryName = parts[parts.length - 1];
        val binarHostPath = Path.of(System.getProperty("user.home"), ".local", "icpi", "bin", binaryName);
        if (Files.exists(binarHostPath)) Files.delete(binarHostPath);
        return "OK";
    }

    private List<Integer> getVersion() {
        val pattern = Pattern.compile("^distrobox:\\s(\\d+(?:\\.\\d+)*)$");
        val output = runCommand("distrobox", "version");
        val matcher = pattern.matcher(output);
        if (!matcher.find())
            throw new RuntimeException(String.format("Regex %s doesn't match string %s", pattern.pattern(), output));
        return Arrays.stream(matcher.group(1).split("\\.")).map(Integer::parseInt).toList();
    }
}

