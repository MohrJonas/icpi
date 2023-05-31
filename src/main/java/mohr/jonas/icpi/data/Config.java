package mohr.jonas.icpi.data;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import lombok.val;
import mohr.jonas.icpi.util.Failable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public record Config(@Nullable ContainerTemplate[] containers, @Nullable List<Integer>[] compatibleVersions) {

    public static Failable<Config> readFromDefaultLocations() {
        val locations = new Path[]{Path.of("/etc/acpi.config"), Path.of(System.getProperty("user.home"), ".config", "acpi.config")};
        val location = Arrays.stream(locations).filter(Files::exists).findFirst();
        if (location.isEmpty())
            return Failable.fail(String.format("No config file was found in the following locations: %s", Arrays.toString(locations)));
        try {
            val fileContent = Files.readString(location.get());
            val gson = new Gson();
            return Failable.success(gson.fromJson(fileContent, Config.class));
        } catch (IOException | JsonSyntaxException e) {
            return Failable.fail(String.format("Error parsing json file %s: %s", location.get(), e.getLocalizedMessage()));
        }
    }

    public static Failable<ContainerTemplate> getByName(Config config, String name) {
        val template = Arrays.stream(config.containers).filter((c) -> c.name().equals(name)).findAny();
        return template.map(Failable::success).orElseGet(() -> Failable.fail(String.format("Unable to find container with name %s in config", name)));
    }
}