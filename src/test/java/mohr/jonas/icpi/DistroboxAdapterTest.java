package mohr.jonas.icpi;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import lombok.val;
import mohr.jonas.icpi.mock.DistroboxAdapterMock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DistroboxAdapterTest {

	private static DistroboxAdapter adapter;

	private static class Module extends AbstractModule {

	}

	@BeforeAll
	public static void setupAdapter() {
		val guice = Guice.createInjector(new Module());
		adapter = guice.getInstance(DistroboxAdapterMock.class);
	}

	@Test
	public void testGetContainer() {
		System.out.println(adapter.listContainers());;
	}

}
