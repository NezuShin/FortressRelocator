package su.nezushin.fr.hidden;

import java.util.Optional;

public abstract class ClosedEither {
    public abstract Optional<String> left();

    public static ClosedEither of(String value) {
        return new Left(value);
    }

    private static final class Left extends ClosedEither {
        private final String value;

        private Left(String value) {
            this.value = value;
        }

        @Override
        public Optional<String> left() {
            return Optional.of(value);
        }
    }
}
