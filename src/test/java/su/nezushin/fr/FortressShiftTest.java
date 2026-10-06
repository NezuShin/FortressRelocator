package su.nezushin.fr;

import org.junit.jupiter.api.Test;
import su.nezushin.fr.hidden.ClosedEither;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FortressShiftTest {
    @Test
    void subclassOffsetsPiecesAfterVanillaGeneration() throws Exception {
        ProbeStub shifted = generate("su.nezushin.fr.ShiftedFortressProbe");
        PieceBuilder builder = new PieceBuilder();
        shifted.generator().left().orElseThrow().accept(builder);

        assertEquals(64 + 15, shifted.position().y());
        assertEquals(List.of("vanilla", "offset:15"), builder.trace);
    }

    @Test
    void readsPublicMethodDeclaredAboveAPrivateSubclass() throws Exception {
        Method invoke = FortressShift.class.getDeclaredMethod("invoke", Object.class, String.class);
        invoke.setAccessible(true);

        Object left = invoke.invoke(null, ClosedEither.of("piece"), "left");

        assertEquals(Optional.of("piece"), left);
    }

    @Test
    void shiftMovesAnAlreadyBuiltPieceList() throws Exception {
        FortressShift.setOffset(() -> 8);
        PieceBuilder builder = new PieceBuilder();
        ProbeStub stub = new ProbeStub(builder);

        assertSame(stub, FortressShift.shift(stub));
        assertEquals(List.of("offset:8"), builder.trace);
    }

    public static final class PieceBuilder {
        private final List<String> trace = new ArrayList<>();

        public void offsetPiecesVertically(int offset) {
            trace.add("offset:" + offset);
        }
    }

    public static final class GeneratorBox {
        private final Consumer<PieceBuilder> left;
        private final PieceBuilder right;

        GeneratorBox(Consumer<PieceBuilder> left) {
            this.left = left;
            this.right = null;
        }

        GeneratorBox(PieceBuilder right) {
            this.left = null;
            this.right = right;
        }

        public Optional<Consumer<PieceBuilder>> left() {
            return Optional.ofNullable(left);
        }

        public Optional<PieceBuilder> right() {
            return Optional.ofNullable(right);
        }
    }

    public static final class ProbePos {
        private final int y;

        ProbePos(int y) {
            this.y = y;
        }

        public ProbePos offset(int x, int y, int z) {
            return new ProbePos(this.y + y);
        }

        public int y() {
            return y;
        }
    }

    public static final class ProbeStub {
        private final ProbePos position;
        private final GeneratorBox generator;

        public ProbeStub(ProbePos position, Consumer<PieceBuilder> generator) {
            this.position = position;
            this.generator = new GeneratorBox(generator);
        }

        ProbeStub(PieceBuilder builder) {
            this.position = new ProbePos(0);
            this.generator = new GeneratorBox(builder);
        }

        public ProbePos position() {
            return position;
        }

        public GeneratorBox generator() {
            return generator;
        }
    }

    public static class FakeFortress {
        public Optional<ProbeStub> findGenerationPoint(Object context) {
            return Optional.of(new ProbeStub(new ProbePos(64), builder -> builder.trace.add("vanilla")));
        }
    }

    private static ProbeStub generate(String typeName) throws Exception {
        FortressShift.setOffset(() -> 15);
        Class<?> type = FortressHooks.define(FakeFortress.class, typeName);
        Object fortress = type.getDeclaredConstructor().newInstance();
        Method findGenerationPoint = type.getMethod("findGenerationPoint", Object.class);
        Optional<?> stub = (Optional<?>) findGenerationPoint.invoke(fortress, new Object());
        return (ProbeStub) stub.orElseThrow();
    }
}
