package mohr.jonas.apex.cli;

import lombok.SneakyThrows;

import java.util.concurrent.CompletableFuture;

public class Spinner {

	private static final String[] SYMBOLS = {"|", "/", "-", "\\"};
	private int state;
	private String text = "";

	public void tick() {
		System.out.printf("%s %s\r", SYMBOLS[state], text);
		if (state == SYMBOLS.length - 1)
			state = 0;
		else
			state++;
	}

	public void reset(String text) {
		this.text = text;
		state = 0;
	}

	@SneakyThrows
	public <T> T spinUntilDone(String text, CompletableFuture<T> future) {
		reset(text);
		while (!future.isDone()) {
			tick();
			//noinspection BusyWait
			Thread.sleep(250L);
		}
		System.out.println();
		return future.get();
	}

}
