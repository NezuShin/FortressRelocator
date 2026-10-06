package su.nezushin.fr;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

final class FortressShift {
    private static volatile IntSupplier yOffset = () -> 0;

    private FortressShift() {
    }

    static void setOffset(IntSupplier yOffset) {
        FortressShift.yOffset = yOffset;
    }

    @RuntimeType
    public static Object findGenerationPoint(@SuperCall Callable<Object> superCall) throws Exception {
        Object result = superCall.call();
        if (!(result instanceof Optional<?> optional) || optional.isEmpty()) {
            return result;
        }
        return Optional.of(shift(optional.get()));
    }

    static Object shift(Object stub) throws ReflectiveOperationException {
        Object either = invoke(stub, "generator");
        Optional<?> left = asOptional(invoke(either, "left"));
        if (left.isPresent()) {
            Consumer<Object> original = castConsumer(left.get());
            int offset = yOffset.getAsInt();
            Consumer<Object> wrapped = builder -> {
                original.accept(builder);
                if (offset == 0) {
                    return;
                }
                try {
                    move(builder, offset);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Could not move nether fortress pieces", exception);
                }
            };
            Object position = invoke(stub, "position");
            if (offset != 0) {
                position = position.getClass()
                        .getMethod("offset", int.class, int.class, int.class)
                        .invoke(position, 0, offset, 0);
            }
            return newStub(stub, position, wrapped);
        }

        Optional<?> right = asOptional(invoke(either, "right"));
        if (right.isPresent()) {
            int offset = yOffset.getAsInt();
            if (offset != 0) {
                move(right.get(), offset);
            }
        }
        return stub;
    }

    private static Object newStub(Object stub, Object position, Consumer<Object> generator) throws ReflectiveOperationException {
        for (Constructor<?> constructor : stub.getClass().getConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 2 && parameters[1].isAssignableFrom(generator.getClass())) {
                try {
                    return constructor.newInstance(position, generator);
                } catch (InvocationTargetException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof ReflectiveOperationException reflective) {
                        throw reflective;
                    }
                    throw new IllegalStateException("Could not rebuild fortress generation stub", cause);
                }
            }
        }
        throw new NoSuchMethodException("No piece-consumer constructor on " + stub.getClass().getName());
    }

    private static void move(Object builder, int offset) throws ReflectiveOperationException {
        builder.getClass().getMethod("offsetPiecesVertically", int.class).invoke(builder, offset);
    }

    private static Object invoke(Object target, String method) throws ReflectiveOperationException {
        try {
            return accessible(target.getClass(), method).invoke(target);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ReflectiveOperationException reflective) {
                throw reflective;
            }
            throw new IllegalStateException("Could not call " + method + " on " + target.getClass().getName(), cause);
        }
    }

    private static Method accessible(Class<?> type, String name) throws NoSuchMethodException {
        Class<?> cursor = type;
        while (cursor != null) {
            Method found = declaredPublic(cursor, name);
            if (found != null) {
                return found;
            }
            for (Class<?> face : cursor.getInterfaces()) {
                found = declaredPublic(face, name);
                if (found != null) {
                    return found;
                }
            }
            cursor = cursor.getSuperclass();
        }
        throw new NoSuchMethodException(name + " on a public type in " + type.getName());
    }

    private static Method declaredPublic(Class<?> type, String name) {
        if (!Modifier.isPublic(type.getModifiers())) {
            return null;
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)
                    && method.getParameterCount() == 0
                    && Modifier.isPublic(method.getModifiers())) {
                return method;
            }
        }
        return null;
    }

    private static Optional<?> asOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional;
        }
        throw new IllegalStateException("Expected Optional, got " + (value == null ? "null" : value.getClass().getName()));
    }

    @SuppressWarnings("unchecked")
    private static Consumer<Object> castConsumer(Object value) {
        return (Consumer<Object>) value;
    }
}
