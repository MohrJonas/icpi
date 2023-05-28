package mohr.jonas.apex.data;

import org.jetbrains.annotations.Nullable;

public record ContainerTemplate(@Nullable String name, @Nullable String image, @Nullable String install,
                                @Nullable String update, @Nullable String upgrade, @Nullable String remove,
                                @Nullable String search) {
}