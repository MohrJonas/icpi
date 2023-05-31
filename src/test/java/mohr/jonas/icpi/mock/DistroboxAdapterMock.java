package mohr.jonas.icpi.mock;

import com.google.inject.Inject;
import mohr.jonas.icpi.DistroboxAdapter;
import mohr.jonas.icpi.data.Container;

import java.util.List;
import java.util.logging.Logger;

public class DistroboxAdapterMock extends DistroboxAdapter {

	@Inject
	public DistroboxAdapterMock(Logger logger) {
		super(logger);
	}

	@Override
	public List<Container> listContainers() throws RuntimeException {
		return List.of(new Container("000000000000", "MockedName", "4", "mocked.image/mocked"));
	}

	@Override
	public String updatePackageInContainer(String name, String updateCommandTemplate) {
		return "Done mock-updating package in container";
	}

	@Override
	public String upgradeContainer(String name) {
		return "Done mock-upgrading container";
	}

	@Override
	public String upgradePackageInContainer(String name, String upgradeCommandTemplate) {
		return "Done mock-upgrading package in container";
	}

	@Override
	public String installPackageInContainer(String name, String installCommandTemplate, String... packages) {
		return "Done mock-installing package in container";
	}

	@Override
	public String searchForPackageInContainer(String name, String searchCommandTemplate, String... packages) {
		return "Done mock-searching package in container";
	}

	@Override
	public String setupContainer(String name, String image, String installCommandTemplate) {
		return "Done mock-setting-up in container";
	}
}
