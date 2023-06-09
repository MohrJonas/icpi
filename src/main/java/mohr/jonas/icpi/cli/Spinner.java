package mohr.jonas.icpi.cli;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.CompletableFuture;

public class Spinner {

    private static final String[] SYMBOLS = {"━", "┅", "┉", "┃", "┇", "┋"};
    private int state;
    private String text = "";

    private void tick() {
        System.out.printf("%s %s\r", SYMBOLS[state], text);
        if (state == SYMBOLS.length - 1) state = 0;
        else state++;
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
            Thread.sleep(300L);
        }
        System.out.print(StringUtils.repeat(" ", text.length() + 2) + "\r");
        return future.get();
    }

}
