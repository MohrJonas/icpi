package mohr.jonas.apex;

import com.google.inject.Inject;
import com.hubspot.jinjava.Jinjava;
import lombok.SneakyThrows;
import lombok.val;
import mohr.jonas.apex.data.Container;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
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
		val process = new ProcessBuilder()
				.command(command)
				.start();
		process.waitFor();
		System.err.println(IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8).trim());
		return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim();
	}

	public List<Container> listContainers() throws RuntimeException {
		val output = runCommand("distrobox-list", "--no-color");
		val lines = ArrayUtils.remove(output.split("\n"), 0);
		val pattern = Pattern.compile("^(\\w{12}) \\| (\\w+) +\\| Up (\\d+) (?:hours|minutes) +\\| (\\S+)$");
		return Arrays.stream(lines).map((line) -> {
			val matcher = pattern.matcher(line.trim());
			if (!matcher.find())
				throw new RuntimeException(String.format("Regex pattern '%s' doesn't match line '%s'", pattern.pattern(), line.trim()));
			return new Container(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
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

	public String setupContainer(String name, String image) {
		return runCommand("distrobox-create", "--image", image, "--name", name, "--yes");
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

}
