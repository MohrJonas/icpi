package mohr.jonas.apex.data;

import com.google.common.collect.ImmutableList;
import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DB implements Serializable {

	private static final Path PATH = Path.of(System.getProperty("user.home"), ".local", "apex", "package.db");

	@Getter
	private final Map<String, List<String>> packages = new HashMap<>();

	@SneakyThrows
	public static DB load() {
		if (!Files.exists(PATH)) return new DB();
		@Cleanup val fos = new FileInputStream(PATH.toFile());
		@Cleanup val oos = new ObjectInputStream(fos);
		return (DB) oos.readObject();
	}

	public void addPackage(String name, String packageName) {
		val list = packages.getOrDefault(name, new ArrayList<>());
		list.add(packageName);
		packages.put(name, list);
	}

	public void removePackage(String name, String packageName) {
		val list = packages.getOrDefault(name, new ArrayList<>());
		list.remove(packageName);
		packages.put(name, list);
	}

	public ImmutableList<String> getPackages(String name) {
		return ImmutableList.copyOf(packages.get(name));
	}

	public ImmutableList<String> getAllPackages() {
		return ImmutableList.copyOf(packages.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
	}

	@SneakyThrows
	public void save() {
		@Cleanup val fos = new FileOutputStream(PATH.toFile());
		@Cleanup val oos = new ObjectOutputStream(fos);
		oos.writeObject(this);
	}

}
