package su.nezushin.fr;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

final class FortressInstaller {
    private static final Object LOCK = new Object();

    private FortressInstaller() {
    }

    static void install(Logger logger) {
        int offset = FortressRelocator.yOffset();
        if (offset == 0) {
            logger.info("y-offset is 0; nether fortresses stay at the vanilla height");
            return;
        }

        synchronized (LOCK) {
            try {
                swap(logger, offset);
            } catch (Exception exception) {
                logger.log(Level.SEVERE, "Could not install the nether fortress Y offset", exception);
            }
        }
    }

    private static void swap(Logger logger, int offset) throws ReflectiveOperationException {
        Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
        Object registryAccess = server.getClass().getMethod("registryAccess").invoke(server);
        Class<?> resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
        Object registryKey = Class.forName("net.minecraft.core.registries.Registries").getField("STRUCTURE").get(null);
        Object location = Class.forName("net.minecraft.resources.Identifier")
                .getMethod("parse", String.class)
                .invoke(null, "minecraft:fortress");
        Object structureKey = createKey(resourceKeyClass, registryKey, location);
        Object registry = method(registryAccess.getClass(), "lookupOrThrow", 1).invoke(registryAccess, registryKey);
        Object holder = holder(registry, structureKey);
        Object current = holder.getClass().getMethod("value").invoke(holder);

        Class<?> fortressType = Class.forName("net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure");
        if (!fortressType.isInstance(current)) {
            logger.warning("minecraft:fortress is " + current.getClass().getName() + "; leaving it unchanged");
            return;
        }

        Class<?> shiftedType = FortressHooks.shiftedFortress(fortressType);
        if (shiftedType.isInstance(current)) {
            return;
        }

        Object settings = field(current.getClass(), "settings").get(current);
        Constructor<?> constructor = shiftedType.getDeclaredConstructor(settings.getClass());
        constructor.setAccessible(true);
        Object shifted = constructor.newInstance(settings);
        retarget(registry, current, shifted, holder);
        logger.info("Nether fortresses will generate " + offset + " blocks above the vanilla Y 48-70 band");
    }

    private static Object createKey(Class<?> resourceKeyClass, Object registryKey, Object location) throws ReflectiveOperationException {
        for (Method method : resourceKeyClass.getMethods()) {
            if (method.getName().equals("create") && method.getParameterCount() == 2) {
                return method.invoke(null, registryKey, location);
            }
        }
        throw new NoSuchMethodException("ResourceKey.create");
    }

    private static Object holder(Object registry, Object structureKey) throws ReflectiveOperationException {
        for (Method method : registry.getClass().getMethods()) {
            if (method.getName().equals("get")
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].getName().equals("net.minecraft.resources.ResourceKey")
                    && Optional.class.isAssignableFrom(method.getReturnType())) {
                Object result = method.invoke(registry, structureKey);
                if (result instanceof Optional<?> optional && optional.isPresent()) {
                    return optional.get();
                }
            }
        }
        throw new IllegalStateException("minecraft:fortress is not in the structure registry");
    }

    @SuppressWarnings("unchecked")
    private static void retarget(Object registry, Object current, Object shifted, Object holder) throws ReflectiveOperationException {
        Map<Object, Object> byValue = (Map<Object, Object>) field(registry.getClass(), "byValue").get(registry);
        Object toId = field(registry.getClass(), "toId").get(registry);
        Method getInt = toId.getClass().getMethod("getInt", Object.class);
        Method putId = toId.getClass().getMethod("put", Object.class, int.class);
        Method removeInt = toId.getClass().getMethod("removeInt", Object.class);

        int id = (Integer) getInt.invoke(toId, current);
        if (id < 0) {
            throw new IllegalStateException("minecraft:fortress has no registry id");
        }
        byValue.put(shifted, holder);
        putId.invoke(toId, shifted, id);
        field(holder.getClass(), "value").set(holder, shifted);
        byValue.remove(current);
        removeInt.invoke(toId, current);

        Object bound = holder.getClass().getMethod("value").invoke(holder);
        if (bound != shifted || byValue.get(shifted) != holder) {
            throw new IllegalStateException("Fortress registry entry did not point at the shifted structure");
        }
    }

    private static Method method(Class<?> type, String name, int parameters) throws NoSuchMethodException {
        Class<?> cursor = type;
        while (cursor != null) {
            for (Method method : cursor.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameters) {
                    return method;
                }
            }
            cursor = cursor.getSuperclass();
        }
        throw new NoSuchMethodException(name + "/" + parameters + " on " + type.getName());
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                Field found = cursor.getDeclaredField(name);
                found.setAccessible(true);
                return found;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " on " + type.getName());
    }
}
