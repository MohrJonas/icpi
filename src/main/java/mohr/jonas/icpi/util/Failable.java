package mohr.jonas.icpi.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Failable<T> {

    private final T result;
    private final String message;
    private final boolean failed;

    @Contract("_ -> new")
    public static <T> @NotNull Failable<T> fail(String message) {
        return new Failable<>(null, message, true);
    }

    @Contract("_ -> new")
    public static <T> @NotNull Failable<T> success(T t) {
        return new Failable<>(t, null, false);
    }

    public boolean hasFailed() {
        return failed;
    }

    public boolean hasSucceeded() {
        return !failed;
    }

    public T orElseFatal() {
        if (!failed) return result;
        System.err.println(message);
        System.exit(-1);
        return null;
    }

}
