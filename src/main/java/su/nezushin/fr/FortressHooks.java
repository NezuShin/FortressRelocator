package su.nezushin.fr;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.invoke.MethodHandles;

final class FortressHooks {
    private static Class<?> shiftedFortress;

    private FortressHooks() {
    }

    static synchronized Class<?> shiftedFortress(Class<?> fortressType) {
        if (shiftedFortress == null) {
            shiftedFortress = define(fortressType, "su.nezushin.fr.ShiftedNetherFortress");
        }
        return shiftedFortress;
    }

    static Class<?> define(Class<?> superType, String name) {
        return new ByteBuddy()
                .subclass(superType)
                .name(name)
                .method(ElementMatchers.named("findGenerationPoint"))
                .intercept(MethodDelegation.to(FortressShift.class))
                .make()
                .load(FortressHooks.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup()))
                .getLoaded();
    }
}
