/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common.config.instantiator;

import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.annotationClassName;
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.annotationClassNames;
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.methodNames;
import static org.mycore.common.config.instantiator.MCRInstantiatorUtils.targetTypeName;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.MCRInstanceConfiguration;
import org.mycore.common.config.MCRInstanceConfiguration.Option;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRRawProperties;
import org.mycore.common.config.instantiator.injectable.MCRFieldInjectable;
import org.mycore.common.config.instantiator.injectable.MCRInjectable;
import org.mycore.common.config.instantiator.injectable.MCRMethodInjectable;
import org.mycore.common.config.instantiator.source.MCRSource;
import org.mycore.common.config.instantiator.source.MCRSourceType;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRInstantiator} creates configured instances, by interpreting supported annotations on fields and methods,
 * using information from these annotations to select values, from the configuration properties, and injecting thees
 * values into the annotated fields and methods.
 */
@SuppressWarnings({ "PMD.MCR.Singleton.ClassModifiers", "PMD.MCR.Singleton.PrivateConstructor",
    "PMD.MCR.Singleton.NonPrivateConstructors", "PMD.MCR.Singleton.MethodModifiers",
    "PMD.MCR.Singleton.MethodReturnType", "PMD.SingletonClassReturningNewInstance" })
public final class MCRInstantiator {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ConcurrentMap<Class<?>, MCRClassInfo<?>> INFOS = new ConcurrentHashMap<>();

    public static <S> S getInstance(Class<S> superClass, MCRInstanceConfiguration configuration, Set<Option> options)
        throws MCRConfigurationException {

        String className = configuration.className();
        if (className == null) {
            boolean classIsImplicit = Modifier.isFinal(superClass.getModifiers());
            boolean useImplicitClass = options.contains(Option.IMPLICIT);
            if (classIsImplicit && useImplicitClass) {
                configuration = configuration.withClass(superClass);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("[IMPLICIT] Adding implicit class property {}={}",
                        configuration.name().actual(), configuration.className());
                }
            } else {
                throw new MCRConfigurationException("Missing property: " + configuration.name().actual()
                    + " (and instance is not required or expected class " + superClass.getName() + " is not final, " +
                    "therefore, the class name cannot be determined implicitly)");
            }
        } else if (className.isBlank()) {
            throw new MCRConfigurationException("Empty property: " + configuration.name().actual()
                + " (and instance is not required or expected class " + superClass.getName() + " is not final, " +
                "therefore, the class name cannot be determined implicitly)");
        }

        Class<S> targetClass = getClass(configuration.name().actual(), configuration.className());
        Object instance = createInstanceDirectOrViaProxy(targetClass, configuration);

        if (superClass.isAssignableFrom(instance.getClass())) {
            return superClass.cast(instance);
        } else {
            throw new MCRConfigurationException("Instance of class " + instance.getClass().getName()
                + "', configured in " + configuration.name().actual() + ", is incompatible with" +
                " intended super class " + superClass.getName());
        }

    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getClass(String property, String className) {
        try {
            return (Class<T>) MCRClassTools.forName(className);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException("Missing class (" + className + ") configured in: " + property, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createInstanceDirectOrViaProxy(Class<T> targetClass, MCRInstanceConfiguration configuration) {
        MCRConfigurationProxy productAnnotation = targetClass.getDeclaredAnnotation(MCRConfigurationProxy.class);
        if (productAnnotation != null) {
            Class<Supplier<T>> proxyClass = (Class<Supplier<T>>) productAnnotation.proxyClass();
            return getInfo(proxyClass).createInstance(configuration).get();
        } else {
            return getInfo(targetClass).createInstance(configuration);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> MCRClassInfo<T> getInfo(Class<T> targetClass) {
        return (MCRClassInfo<T>) INFOS.computeIfAbsent(targetClass, MCRClassInfo::new);
    }

    /**
     * A {@link MCRClassInfo} is a helper class that gathers and holds some information about a target class.
     * <p>
     * This class gathers the following information about the target class:
     * <ul>
     *   <li>
     *     A {@link Supplier} as a factory for creating instances of the target class.
     *     That factory is
     *     <ol>
     *       <li>
     *         preferably the target classes parameterless public constructor or
     *       </li>
     *       <li>
     *         a parameterless public static factory method whose name case-insensitively contains
     *         the word "instance".
     *       </li>
     *     </ol>
     *     An exception is thrown if neither kind of factory exists or if no suitable constructor but
     *     multiple suitable factory methods exists.
     *   </li>
     *   <li>
     *     A list of {@link Injector} that are executed after an instance of the target class has been created.
     *     Each injector binds together a {@link MCRSource} and a {@link MCRTarget}.
     *     <br/>
     *     Sources implement the varying strategies that create values from configuration properties.
     *     Source implementations exist for all supported annotations:
     *     <ol>
     *       <li>{@link MCRProperty}</li>
     *       <li>{@link MCRRawProperties}</li>
     *       <li>{@link MCRInstance}</li>
     *       <li>{@link MCRInstanceMap}</li>
     *       <li>{@link MCRInstanceList}</li>
     *       <li>{@link MCRPostConstruction}</li>
     *     </ol>
     *     Targets are:
     *     <ul>
     *       <li>public fields,</li>
     *       <li>public methods with no parameter or</li>
     *       <li>public methods with one parameter.</li>
     *     </ul>
     *     The list of injectors is created by
     *     <ol>
     *       <li>
     *         examining all possible fields and methods, generalized as {@link MCRInjectable},
     *         (done in {@link MCRClassInfo#findInjectors(Class)}),
     *       </li>
     *       <li>
     *         creating a corresponding source for supported annotations
     *         (done in {@link MCRInjectable#toSource(MCRSourceType)}),
     *       </li>
     *       <li>
     *         creating a corresponding target for that injectable
     *         (using {@link MCRInjectable#toTarget()}).
     *       </li>
     *     </ol>
     *     An exception is thrown if multiple sources are detected for the same target (for example, because a method is
     *     annotated with {@link MCRProperty} and {@link MCRPostConstruction}), if the target is not allowed for the
     *     detected source (for example, {@link MCRPostConstruction} is only allowed on methods) or if the values
     *     produced by the source are assignment-incompatible with the target (for example, values produced by a
     *     source for {@link MCRProperty} are not assignment-compatible with {@link String}) (as far as type erasure
     *     allows it to determine this preemptively).
     *     <br/>
     *     The list is ordered by the type of target (all fields first, then all methods), the type of source annotation
     *     (first everything other than {@link MCRPostConstruction}, then {@link MCRPostConstruction}) and lastly the
     *     order attribute of the source annotations, if available (for example {@link MCRProperty#order()}).
     *   </li>
     * </ul>
     * <p>
     * It is intended that instances of this class are cached, such that the information about the target class
     * only needs to be gathered once.
     */
    private static final class MCRClassInfo<T> {

        private final Class<T> targetClass;

        private final Supplier<T> factory;

        private final List<Injector> injectors;

        private MCRClassInfo(Class<T> targetClass) {
            this.targetClass = targetClass;
            this.factory = getFactory(targetClass);
            this.injectors = findInjectors(targetClass);
        }

        private Supplier<T> getFactory(Class<T> targetClass) {

            List<Method> declaredFactoryMethods = findFactoryMethods(targetClass, targetClass.getDeclaredMethods());
            Optional<Supplier<T>> factory = Stream.<Supplier<Optional<Supplier<T>>>>of(
                () -> findSingletonFactoryMethod(declaredFactoryMethods),
                () -> findAnnotatedFactoryMethod(declaredFactoryMethods),
                () -> findDefaultConstructor(targetClass))
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

            return factory.orElseThrow(() -> new MCRConfigurationException("Class " + targetClass.getName() + " has "
                + " has no singleton factory method (public, static, matching return type, parameterless, name"
                + " equals 'getInstance'), no annotated factory method (public, static, matching return type,"
                + " parameterless, annotated with @MCRFactory) and no public parameterless constructor"));

        }

        private static <T> List<Method> findFactoryMethods(Class<T> targetClass, Method[] declaredMethods) {
            return Stream.of(declaredMethods)
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType().isAssignableFrom(targetClass))
                .filter(method -> method.getParameterTypes().length == 0)
                .filter(method -> !method.isVarArgs())
                .toList();
        }

        private Optional<Supplier<T>> findSingletonFactoryMethod(List<Method> factoryMethods) {

            List<Method> singletonFactoryMethods = factoryMethods.stream()
                .filter(method -> method.getName().equals("getInstance"))
                .toList();

            if (singletonFactoryMethods.size() > 1) {
                throw new MCRConfigurationException("Class " + targetClass.getName()
                    + " has multiple singleton factory methods (public, static, matching return type, parameterless,"
                    + " name equals 'getInstance'): " + methodNames(singletonFactoryMethods));
            }

            return singletonFactoryMethods.stream().findFirst().map(this::createFactoryMethodFactory);

        }

        private Optional<Supplier<T>> findAnnotatedFactoryMethod(List<Method> factoryMethods) {

            List<Method> annotatedFactoryMethods = factoryMethods.stream()
                .filter(method -> method.getDeclaredAnnotation(MCRFactory.class) != null)
                .toList();

            if (annotatedFactoryMethods.size() > 1) {
                throw new MCRConfigurationException("Class " + targetClass.getName()
                    + " has multiple annotated factory methods (public, static, matching return type, parameterless,"
                    + " annotated with @MCRFactory): " + methodNames(annotatedFactoryMethods));
            }

            return annotatedFactoryMethods.stream().findFirst().map(this::createFactoryMethodFactory);

        }

        private Optional<Supplier<T>> findDefaultConstructor(Class<T> targetClass) {
            try {
                return Optional.of(createConstructorFactory(targetClass.getConstructor()));
            } catch (NoSuchMethodException e) {
                return Optional.empty();
            }
        }

        @SuppressWarnings("unchecked")
        private Supplier<T> createFactoryMethodFactory(Method method) {
            return () -> {
                try {
                    return (T) method.invoke(method.getDeclaringClass(), (Object[]) null);
                } catch (Exception e) {
                    throw new MCRConfigurationException("Unable to create an instance of "
                        + method.getDeclaringClass().getName() + " using public, parameterless constructor", e);
                }
            };
        }

        private Supplier<T> createConstructorFactory(Constructor<T> constructor) {
            return () -> {
                try {
                    return constructor.newInstance();
                } catch (Exception e) {
                    throw new MCRConfigurationException("Unable to create an instance of "
                        + constructor.getDeclaringClass().getName() + " using parameterless public constructor", e);
                }
            };
        }

        private List<Injector> findInjectors(Class<T> targetClass) {
            List<Injector> injectors = new LinkedList<>();
            for (Field field : targetClass.getFields()) {
                findInjector(new MCRFieldInjectable(field)).ifPresent(injectors::add);
            }
            for (Method method : targetClass.getMethods()) {
                findInjector(new MCRMethodInjectable(method)).ifPresent(injectors::add);
            }
            Collections.sort(injectors);
            return injectors;
        }

        private Optional<Injector> findInjector(MCRInjectable injectable) {

            List<MCRSource> sources = new LinkedList<>();
            for (MCRSourceType sourceType : MCRSourceType.values()) {
                injectable.toSource(sourceType).ifPresent(sources::add);
            }

            if (sources.isEmpty()) {
                return Optional.empty();
            }

            MCRTarget target = injectable.toTarget();
            if (sources.size() != 1) {
                throw new MCRConfigurationException("Target " + targetTypeName(target) + " '"
                    + target.name() + "' has multiple annotations (" + annotationClassNames(sources)
                    + ") in configured class " + targetClass.getName());
            }

            MCRSource source = sources.getFirst();

            if (!source.allowedTargetTypes().contains(target.type())) {
                throw new MCRConfigurationException("Target " + targetTypeName(target) + " '"
                    + target.name() + "' is not allowed for annotation (" + annotationClassName(source)
                    + ") in configured class " + targetClass.getName());
            }

            if (!target.isAssignableFrom(source.valueClass())) {
                throw new MCRConfigurationException("Target " + targetTypeName(target) + " '"
                    + target.name() + "' has incompatible type for annotation (" + annotationClassName(source)
                    + ") in configured class " + targetClass.getName());
            }

            return Optional.of(new Injector(source, target));

        }

        public T createInstance(MCRInstanceConfiguration configuration) {
            T instance = factory.get();
            for (Injector injector : injectors) {
                injector.inject(instance, configuration);
            }
            return instance;
        }

    }

    private record Injector(MCRSource source, MCRTarget target) implements Comparable<Injector> {

        private static final Comparator<Injector> COMPARATOR = Comparator
            .comparingInt((ToIntFunction<Injector>) injector -> injector.target.type().order())
            .thenComparingInt(injector -> injector.source.type().order())
            .thenComparingInt(injector -> injector.source.order());

        private void inject(Object instance, MCRInstanceConfiguration configuration) {
            Object value = source.get(configuration, target);
            if (value != null) {
                target.set(instance, value);
            }
        }

        @Override
        public int compareTo(Injector other) {
            return COMPARATOR.compare(this, other);
        }

    }

}
