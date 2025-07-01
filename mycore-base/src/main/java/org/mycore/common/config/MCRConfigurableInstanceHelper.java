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

package org.mycore.common.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRRawProperties;
import org.mycore.common.config.annotation.MCRSentinel;

import jakarta.inject.Singleton;

/**
 * Creates Objects which are configured with properties.
 *
 * @author Sebastian Hofmann
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.SingleMethodSingleton",
    "PMD.MCR.Singleton.MethodModifiers", "PMD.MCR.Singleton.MethodReturnType",
    "PMD.MCR.Singleton.ClassModifiers", "PMD.MCR.Singleton.PrivateConstructor",
    "PMD.MCR.Singleton.NonPrivateConstructors",
})
class MCRConfigurableInstanceHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Set<Option> NO_OPTIONS = Collections.emptySet();

    public static final Set<Option> ADD_IMPLICIT_CLASS_PROPERTIES
        = Collections.singleton(Option.ADD_IMPLICIT_CLASS_PROPERTIES);

    private static final ConcurrentMap<Class<?>, ClassInfo<?>> INFOS = new ConcurrentHashMap<>();

    /**
     * Checks if a class is annotated with {@link Singleton}.
     *
     * @param property the configuration property which contains the class
     * @return true if the class in the property is annotated with {@link Singleton}
     */
    public static boolean isSingleton(String property) {
        return MCRConfiguration2.getString(property).stream()
            .anyMatch(propertyVal -> isSingleton(getClass(property, propertyVal)));
    }

    /**
     * Checks if a class is annotated with {@link Singleton}.
     *
     * @param targetClass the class
     * @return true if the class in the property is annotated with {@link Singleton}
     */
    public static boolean isSingleton(Class<?> targetClass) {
        return targetClass.getDeclaredAnnotation(Singleton.class) != null;
    }

    /**
     * Shorthand for {@link #getInstance(Class, String, Set)} that uses no options.
     */
    public static <S> Optional<S> getInstance(Class<S> superClass, String name) throws MCRConfigurationException {
        return getInstance(superClass, name, NO_OPTIONS);
    }

    /**
     * Creates a configured instance of a class.
     *
     * @param superClass the intended super class of the instantiated class
     * @param name       the property which contains the class name
     * @param options    the options to be used
     * @return the configured instance of T
     * @throws MCRConfigurationException if the property is not right configured.
     */
    public static <S> Optional<S> getInstance(Class<S> superClass, String name, Set<Option> options)
        throws MCRConfigurationException {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName(name);
        String className = configuration.className();
        if (isAbsent(className) && !options.contains(Option.ADD_IMPLICIT_CLASS_PROPERTIES)) {
            return Optional.empty();
        }
        return Optional.of(getInstance(superClass, configuration, name, options));
    }

    private static boolean isAbsent(String className) {
        return className == null || className.isBlank();
    }

    /**
     * Shorthand for {@link #getInstance(Class, MCRInstanceConfiguration, Set)} that uses no options.
     */
    public static <S> S getInstance(Class<S> superClass, MCRInstanceConfiguration configuration)
        throws MCRConfigurationException {
        return getInstance(superClass, configuration, NO_OPTIONS);
    }

    /**
     * Creates a configured instance of a class.
     *
     * @param superClass    the intended super class of the instantiated class
     * @param configuration the configuration to be used
     * @param options    the options to be used
     * @return the configured instance of T
     * @throws MCRConfigurationException if the property is not right configured.
     */
    public static <S> S getInstance(Class<S> superClass, MCRInstanceConfiguration configuration,
        Set<Option> options) throws MCRConfigurationException {
        return getInstance(superClass, configuration, null, options);
    }

    private static <S> S getInstance(Class<S> superClass, MCRInstanceConfiguration configuration, String name,
        Set<Option> options) throws MCRConfigurationException {
        String className = configuration.className();
        if (className == null || className.isBlank()) {
            if (options.contains(Option.ADD_IMPLICIT_CLASS_PROPERTIES) && Modifier.isFinal(superClass.getModifiers())) {
                configuration = configuration.fixedClass(superClass);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Adding implicit property {}={}", configuration.name().actual(),
                        configuration.className());
                }
            } else {
                throw new MCRConfigurationException("Missing or empty property: " + configuration.name().actual()
                    + " (and expected class " + superClass.getName() + " is not final)");
            }
        }
        Class<S> targetClass = getClass(configuration.name().actual(), configuration.className());
        Object instance = createInstanceDirectorViaProxy(targetClass, configuration);
        if (superClass.isAssignableFrom(instance.getClass())) {
            return superClass.cast(instance);
        } else {
            throw new MCRConfigurationException("Configured instance of class " + instance.getClass().getName()
                + " is incompatible with intended super class " + superClass.getName() + "'"
                + (name != null ? " in configured class " + name : "")
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getClass(String property, String className) {
        try {
            return (Class<T>) MCRClassTools.forName(className);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException("Missing class (" + className + ") configured in property: "
                + property, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createInstanceDirectorViaProxy(Class<T> targetClass, MCRInstanceConfiguration configuration) {
        MCRConfigurationProxy productAnnotation = targetClass.getDeclaredAnnotation(MCRConfigurationProxy.class);
        if (productAnnotation != null) {
            Class<Supplier<T>> proxyClass = (Class<Supplier<T>>) productAnnotation.proxyClass();
            return getInfo(proxyClass).createInstance(configuration).get();
        } else {
            return getInfo(targetClass).createInstance(configuration);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ClassInfo<T> getInfo(Class<T> targetClass) {
        return (ClassInfo<T>) INFOS.computeIfAbsent(targetClass, ClassInfo::new);
    }

    private static String methodNames(List<Method> methods) {
        return methods.stream()
            .map(Method::getName)
            .collect(Collectors.joining(", "));
    }

    private static String targetTypeName(Target<?> target) {
        return target.type().name().toLowerCase(Locale.ROOT);
    }

    private static String annotationNames(List<Source<?, ?>> sources) {
        return sources.stream().map(MCRConfigurableInstanceHelper::annotationName).collect(Collectors.joining(", "));
    }

    private static String annotationName(Source<?, ?> source) {
        return source.annotationClass().getName();
    }

    private static void throwIncompatibleAnnotation(Class<?> annotationValueClass, Target<?> target, Object instance) {
        throw new MCRConfigurationException("Configured instance of class " + instance.getClass().getName()
            + " is incompatible with annotation value class " + annotationValueClass.getName()
            + " for target " + targetTypeName(target) + " '" + target.name()
            + "' in configured class " + target.declaringClass().getName());
    }

    /**
     * A {@link ClassInfo} is a helper class that gathers and holds some information about a target class.
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
     *     Each injector binds together a {@link Source} and a {@link Target}.
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
     *         examining all possible fields and methods, generalized as {@link Injectable},
     *         (done in {@link ClassInfo#findInjectors(Class)}),
     *       </li>
     *       <li>
     *         checking, if such an injectable is annotated with a supported annotation
     *         (done in {@link AnnotationMapper#injectableToSource(Injectable)}),
     *       </li>
     *       <li>
     *         creating a corresponding source for that annotation
     *         (using {@link AnnotationMapper#annotationToSource(Annotation)}) and
     *       </li>
     *       <li>
     *         creating a corresponding target for that injectable
     *         (using {@link Injectable#toTarget()}).
     *       </li>
     *     </ol>
     *     An exception is thrown if multiple sources are detected for the same target (for example, because a method is
     *     annotated with {@link MCRProperty} and {@link MCRPostConstruction}), if the target is not allowed for the
     *     detected source (for example, {@link MCRPostConstruction} is only allowed on methods) or if the values
     *     produced by the source are assignment-incompatible with the target (for example, values produced by a
     *     {@link InstanceSource} are not assignment-compatible with {@link String}) (as far as type erasure allows it
     *     to determine this preemptively).
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
    private static final class ClassInfo<T> {

        private final Class<T> targetClass;

        private final Supplier<T> factory;

        private final List<Injector<T, ?>> injectors;

        private ClassInfo(Class<T> targetClass) {
            this.targetClass = targetClass;
            this.factory = getFactory(targetClass);
            this.injectors = findInjectors(targetClass);
        }

        // TODO: remove legacy factory method option after release of 2025.06 LTS and adjust error message
        private Supplier<T> getFactory(Class<T> targetClass) {

            List<Method> declaredFactoryMethods = findFactoryMethods(targetClass, targetClass.getDeclaredMethods());
            Optional<Supplier<T>> factory = Stream.<Supplier<Optional<Supplier<T>>>>of(
                    () -> findSingletonFactoryMethod(declaredFactoryMethods),
                    () -> findAnnotatedFactoryMethod(declaredFactoryMethods),
                    () -> findDefaultConstructor(targetClass),
                    () -> findLegacyFactoryMethod(findFactoryMethods(targetClass, targetClass.getMethods())))
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

            return factory.orElseThrow(() ->
                new MCRConfigurationException("Class " + targetClass.getName() + " has "
                    + " has no singleton factory method (public, static, matching return type, parameterless, name"
                    + " equals 'getInstance'), no annotated factory method (public, static, matching return type,"
                    + " parameterless, annotated with @MCRFactory), no public parameterless constructor and no legacy"
                    + " factory method (public, static, matching return type, parameterless, name containing"
                    + " 'instance')")
            );

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
                .filter(method -> method.getAnnotation(Deprecated.class) == null)
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

        private Optional<Supplier<T>> findLegacyFactoryMethod(List<Method> factoryMethods) {

            List<Method> legacyFactoryMethods = factoryMethods.stream()
                .filter(method -> method.getName().toLowerCase(Locale.ROOT).contains("instance"))
                .toList();

            if (legacyFactoryMethods.size() > 1) {
                throw new MCRConfigurationException("Class " + targetClass.getName()
                    + " has multiple legacy factory methods (public, static, matching return type, parameterless, name"
                    + " contains 'instance'): " + methodNames(legacyFactoryMethods));
            }

            return legacyFactoryMethods.stream().findFirst().map(method -> {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Instantiation of {} relies on legacy factory method (public, static,"
                            + " matching return type, parameterless, name containing 'instance') {}.",
                        targetClass.getName(), method.getName());
                }
                return createFactoryMethodFactory(method);
            });

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

        private List<Injector<T, ?>> findInjectors(Class<T> targetClass) {
            List<Injector<T, ?>> injectors = new LinkedList<>();
            for (Field field : targetClass.getFields()) {
                findInjector(new FieldInjectable(field)).ifPresent(injectors::add);
            }
            for (Method method : targetClass.getMethods()) {
                findInjector(new MethodInjectable(method)).ifPresent(injectors::add);
            }
            Collections.sort(injectors);
            return injectors;
        }

        private Optional<Injector<T, ?>> findInjector(Injectable injectable) {

            List<Source<?, ?>> sources = new LinkedList<>();
            for (SourceType sourceType : SourceType.values()) {
                sourceType.mapper.injectableToSource(injectable).ifPresent(sources::add);
            }

            if (sources.isEmpty()) {
                return Optional.empty();
            }

            Target<T> target = injectable.toTarget();
            if (sources.size() != 1) {
                throw new MCRConfigurationException("Target " + targetTypeName(target) + " '"
                    + target.name() + "' has multiple annotations (" + annotationNames(sources)
                    + ") in configured class " + targetClass.getName());
            }

            Source<?, ?> source = sources.getFirst();

            if (!source.allowedTargetTypes().contains(target.type())) {
                throw new MCRConfigurationException("Target " + targetTypeName(target) + " '"
                    + target.name() + "' is not allowed for annotation (" + annotationName(source)
                    + ") in configured class " + targetClass.getName());
            }

            if (!target.isAssignableFrom(source.valueClass())) {
                throw new MCRConfigurationException("Target " + targetTypeName(target) + " '"
                    + target.name() + "' has incompatible type for annotation (" + annotationName(source)
                    + ") in configured class " + targetClass.getName());
            }

            return Optional.of(new Injector<>(target, source));

        }

        public T createInstance(MCRInstanceConfiguration configuration) {
            T instance = factory.get();
            for (Injector<T, ?> injector : injectors) {
                injector.inject(instance, configuration);
            }
            return instance;
        }

    }

    private static abstract class Injectable {

        public abstract <A extends Annotation> Optional<A> annotation(Class<A> annotationClass);

        public abstract <T> Target<T> toTarget();

    }

    private static final class FieldInjectable extends Injectable {

        private final Field field;

        private FieldInjectable(Field field) {
            this.field = field;
        }

        @Override
        public <A extends Annotation> Optional<A> annotation(Class<A> annotationClass) {
            return Optional.ofNullable(field.getDeclaredAnnotation(annotationClass));
        }

        @Override
        public <T> Target<T> toTarget() {
            return new FieldTarget<>(field);
        }

    }

    private static final class MethodInjectable extends Injectable {

        private final Method method;

        private MethodInjectable(Method method) {
            this.method = method;
        }

        @Override
        public <A extends Annotation> Optional<A> annotation(Class<A> annotationClass) {
            return Optional.ofNullable(method.getDeclaredAnnotation(annotationClass));
        }

        @Override
        public <T> Target<T> toTarget() {
            int numberOfParameters = method.getParameterTypes().length;
            return switch (numberOfParameters) {
                case 0 -> new NoParameterMethodTarget<>(method);
                case 1 -> new SingleParameterMethodTarget<>(method);
                default -> throw new MCRConfigurationException("Target method '" + method.getName() +
                    "' has an unexpected number of parameters (" + numberOfParameters +
                    ") in configured class " + method.getDeclaringClass().getName());
            };
        }

    }

    private enum TargetType {

        FIELD(0),

        METHOD(1);

        public final int order;

        TargetType(int order) {
            this.order = order;
        }

    }

    private static abstract class Target<T> {

        public abstract TargetType type();

        public abstract Class<? super T> declaringClass();

        public abstract String name();

        public abstract boolean isAssignableFrom(Class<?> valueClass);

        public abstract void set(T instance, Object value);

    }

    private static final class FieldTarget<T> extends Target<T> {

        private final Field field;

        private FieldTarget(Field field) {
            this.field = field;
        }

        @Override
        public TargetType type() {
            return TargetType.FIELD;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? super T> declaringClass() {
            return (Class<? super T>) field.getDeclaringClass();
        }

        @Override
        public String name() {
            return field.getName();
        }

        @Override
        public boolean isAssignableFrom(Class<?> valueClass) {
            return valueClass.isAssignableFrom(field.getType());
        }

        @Override
        public void set(T instance, Object value) {
            try {
                field.set(instance, value);
            } catch (Exception e) {
                throw new MCRConfigurationException("Failed to set target field '" + name() + "' to '" + value
                    + "' in configurable class " + field.getDeclaringClass().getName(), e);
            }
        }

    }

    private static abstract class MethodTarget<T> extends Target<T> {

        private final Method method;

        private MethodTarget(Method method) {
            this.method = method;
        }

        @Override
        public final TargetType type() {
            return TargetType.METHOD;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? super T> declaringClass() {
            return (Class<? super T>) method.getDeclaringClass();
        }

        @Override
        public final String name() {
            return method.getName();
        }

    }

    private static final class NoParameterMethodTarget<T> extends MethodTarget<T> {

        private final Method method;

        private NoParameterMethodTarget(Method method) {
            super(method);
            this.method = method;
        }

        @Override
        public boolean isAssignableFrom(Class<?> valueClass) {
            return true;
        }

        @Override
        public void set(T instance, Object value) {
            try {
                method.invoke(instance);
            } catch (Exception e) {
                throw new MCRConfigurationException("Failed to call target method '" + name()
                    + "' in configurable class " + method.getDeclaringClass().getName(), e);
            }
        }

    }

    private static final class SingleParameterMethodTarget<T> extends MethodTarget<T> {

        private final Method method;

        private SingleParameterMethodTarget(Method method) {
            super(method);
            this.method = method;
        }

        @Override
        public boolean isAssignableFrom(Class<?> valueClass) {
            return method.getParameterTypes()[0].isAssignableFrom(valueClass);
        }

        @Override
        public void set(T instance, Object value) {
            try {
                method.invoke(instance, value);
            } catch (Exception e) {
                throw new MCRConfigurationException("Failed to call target method '" + name() + "' with '"
                    + value + "' in configurable class " + method.getDeclaringClass().getName(), e);
            }
        }

    }

    private enum SourceGroup {

        VALUE_INJECTION,

        POST_CONSTRUCTION;

    }

    private enum SourceType {

        PROPERTY(SourceGroup.VALUE_INJECTION, new AnnotationMapper<MCRProperty>() {

            @Override
            public Class<MCRProperty> annotationClass() {
                return MCRProperty.class;
            }

            @Override
            public Source<MCRProperty, ?> annotationToSource(MCRProperty annotation) {
                String annotationName = annotation.name();
                if (annotationName.equals("*")) {
                    warnAboutDeprecatedNamePattern(annotationName);
                    return new AllPropertiesSource(annotation, "");
                } else if (annotationName.endsWith(".*")) {
                    warnAboutDeprecatedNamePattern(annotationName);
                    return new AllPropertiesSource(annotation, annotationName
                        .substring(0, annotationName.length() - 1));
                } else {
                    return new PropertySource(annotation);
                }
            }

            private void warnAboutDeprecatedNamePattern(String annotationName) {
                LOGGER.warn("Injection property map with @MCRProperty amd name pattern {}."
                    + " You should migrate to @MCRRawProperties, as this feature will be removed"
                    + " in a future release of MyCoRe.", annotationName);
            }

        }),

        RAW_PROPERTIES(SourceGroup.VALUE_INJECTION, new AnnotationMapper<MCRRawProperties>() {

            @Override
            public Class<MCRRawProperties> annotationClass() {
                return MCRRawProperties.class;
            }

            @Override
            public Source<MCRRawProperties, ?> annotationToSource(MCRRawProperties annotation) {
              return new RawPropertiesSource(annotation);
            }

        }),

        INSTANCE(SourceGroup.VALUE_INJECTION, new AnnotationMapper<MCRInstance>() {

            @Override
            public Class<MCRInstance> annotationClass() {
                return MCRInstance.class;
            }

            @Override
            public Source<MCRInstance, ?> annotationToSource(MCRInstance annotation) {
                return new InstanceSource(annotation);
            }

        }),

        INSTANCE_MAP(SourceGroup.VALUE_INJECTION, new AnnotationMapper<MCRInstanceMap>() {

            @Override
            public Class<MCRInstanceMap> annotationClass() {
                return MCRInstanceMap.class;
            }

            @Override
            public Source<MCRInstanceMap, ?> annotationToSource(MCRInstanceMap annotation) {
                return new InstanceMapSource(annotation);
            }

        }),

        INSTANCE_LIST(SourceGroup.VALUE_INJECTION, new AnnotationMapper<MCRInstanceList>() {

            @Override
            public Class<MCRInstanceList> annotationClass() {
                return MCRInstanceList.class;
            }

            @Override
            public Source<MCRInstanceList, ?> annotationToSource(MCRInstanceList annotation) {
                return new InstanceListSource(annotation);
            }

        }),

        POST_CONSTRUCTION(SourceGroup.POST_CONSTRUCTION, new AnnotationMapper<MCRPostConstruction>() {

            @Override
            public Class<MCRPostConstruction> annotationClass() {
                return MCRPostConstruction.class;
            }

            @Override
            public Source<MCRPostConstruction, ?> annotationToSource(MCRPostConstruction annotation) {
                return new PostConstructionSource(annotation);
            }

        });

        public final int order;

        public final AnnotationMapper<? extends Annotation> mapper;

        SourceType(SourceGroup group, AnnotationMapper<? extends Annotation> mapper) {
            this.order = group.ordinal();
            this.mapper = mapper;
        }

    }

    private abstract static class AnnotationMapper<A extends Annotation> {

        public abstract Class<A> annotationClass();

        public abstract Source<A, ?> annotationToSource(A annotation);

        public Optional<Source<A, ?>> injectableToSource(Injectable injectable) {
            return injectable.annotation(annotationClass()).map(this::annotationToSource);
        }

    }

    private static abstract class Source<A extends Annotation, V> {

        public abstract SourceType type();

        public abstract Class<A> annotationClass();

        public abstract A annotation();

        public abstract int order();

        public abstract Set<TargetType> allowedTargetTypes();

        public abstract Class<?> valueClass();

        public abstract V get(MCRInstanceConfiguration configuration, Target<?> target);

        public final Set<Option> createOptions(Class<?> valueClass, boolean required) {
            return required && Modifier.isFinal(valueClass.getModifiers())
                ? ADD_IMPLICIT_CLASS_PROPERTIES : NO_OPTIONS;
        }

    }

    private static final class PropertySource extends Source<MCRProperty, String> {

        private final MCRProperty annotation;

        private PropertySource(MCRProperty annotation) {
            this.annotation = annotation;
        }

        @Override
        public SourceType type() {
            return SourceType.PROPERTY;
        }

        @Override
        public Class<MCRProperty> annotationClass() {
            return MCRProperty.class;
        }

        @Override
        public MCRProperty annotation() {
            return annotation;
        }

        @Override
        public int order() {
            return annotation.order();
        }

        @Override
        public Set<TargetType> allowedTargetTypes() {
            return EnumSet.allOf(TargetType.class);
        }

        @Override
        public Class<?> valueClass() {
            return String.class;
        }

        @Override
        public String get(MCRInstanceConfiguration configuration, Target<?> target) {

            String value;

            if (!annotation.absolute()) {
                value = configuration.properties().get(annotation.name());
            } else {
                value = configuration.fullProperties().get(annotation.name());
            }

            if (value == null && !Objects.equals("", annotation.defaultName())) {
                value = configuration.fullProperties().get(annotation.defaultName());
                if (value == null) {
                    throw new MCRConfigurationException("The default property "
                        + annotation.defaultName() + " is missing");
                }
            }

            if (value == null && annotation.required()) {
                throw new MCRConfigurationException("The required property "
                    + configuration.name().canonical() + "."
                    + annotation.name() + " is missing");
            }

            return value;

        }

    }

    private static final class AllPropertiesSource extends Source<MCRProperty, Map<String, String>> {

        private final MCRProperty annotation;

        private final String prefix;

        private AllPropertiesSource(MCRProperty annotation, String prefix) {
            this.annotation = annotation;
            this.prefix = prefix;
        }

        @Override
        public SourceType type() {
            return SourceType.PROPERTY;
        }

        @Override
        public Class<MCRProperty> annotationClass() {
            return MCRProperty.class;
        }

        @Override
        public MCRProperty annotation() {
            return annotation;
        }

        @Override
        public int order() {
            return annotation.order();
        }

        @Override
        public Set<TargetType> allowedTargetTypes() {
            return EnumSet.allOf(TargetType.class);
        }

        @Override
        public Class<?> valueClass() {
            return Map.class;
        }

        @Override
        public Map<String, String> get(MCRInstanceConfiguration configuration, Target<?> target) {

            Map<String, String> properties = annotation.absolute() ?
                configuration.fullProperties() : configuration.properties();

            Map<String, String> filteredProperties = new HashMap<>();
            properties.forEach((key, value) -> {
                if (key.startsWith(prefix)) {
                    filteredProperties.put(key.substring(prefix.length()), value);
                }
            });

            return filteredProperties;

        }

    }

    private static final class RawPropertiesSource extends Source<MCRRawProperties, Map<String, String>> {

        private final MCRRawProperties annotation;

        private final String prefix;

        private RawPropertiesSource(MCRRawProperties annotation) {
            this.annotation = annotation;
            String namePattern = annotation.namePattern();
            if (namePattern.equals("*")) {
                this.prefix = "";
            } else if (namePattern.endsWith(".*")) {
                this.prefix = namePattern.substring(0, namePattern.length() - 1);
            } else {
                throw new MCRConfigurationException("Unsupported name pattern:" + annotation.namePattern());
            }
        }

        @Override
        public SourceType type() {
            return SourceType.RAW_PROPERTIES;
        }

        @Override
        public Class<MCRRawProperties> annotationClass() {
            return MCRRawProperties.class;
        }

        @Override
        public MCRRawProperties annotation() {
            return annotation;
        }

        @Override
        public int order() {
            return annotation.order();
        }

        @Override
        public Set<TargetType> allowedTargetTypes() {
            return EnumSet.allOf(TargetType.class);
        }

        @Override
        public Class<?> valueClass() {
            return Map.class;
        }

        @Override
        public Map<String, String> get(MCRInstanceConfiguration configuration, Target<?> target) {

            Map<String, String> properties = annotation.absolute() ?
                configuration.fullProperties() : configuration.properties();

            Map<String, String> filteredProperties = new HashMap<>();
            properties.forEach((key, value) -> {
                if (key.startsWith(prefix)) {
                    filteredProperties.put(key.substring(prefix.length()), value);
                }
            });

            if (filteredProperties.isEmpty() && annotation.required()) {
                throw new MCRConfigurationException("The required property "
                    + configuration.name().canonical() + "."
                    + annotation.namePattern() + " has no values");
            }

            return filteredProperties;

        }

    }

    private static final class InstanceSource extends Source<MCRInstance, Object> {

        private final MCRInstance annotation;

        private InstanceSource(MCRInstance annotation) {
            this.annotation = annotation;
        }

        @Override
        public SourceType type() {
            return SourceType.INSTANCE;
        }

        @Override
        public Class<MCRInstance> annotationClass() {
            return MCRInstance.class;
        }

        @Override
        public MCRInstance annotation() {
            return annotation;
        }

        @Override
        public int order() {
            return annotation.order();
        }

        @Override
        public Set<TargetType> allowedTargetTypes() {
            return EnumSet.allOf(TargetType.class);
        }

        @Override
        public Class<?> valueClass() {
            return annotation.valueClass();
        }

        @Override
        public Object get(MCRInstanceConfiguration configuration, Target<?> target) {

            MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration(annotation().name());

            MCRSentinel sentinel = annotation.sentinel();
            if (sentinel.enabled()) {
                boolean sentinelValue = sentinel.defaultValue();
                String configurationValue = nestedConfiguration.properties().get(sentinel.name());
                if (configurationValue != null) {
                    sentinelValue = Boolean.parseBoolean(configurationValue);
                }
                if (!sentinelValue) {
                    nestedConfiguration = MCRInstanceConfiguration.ofName("", Collections.emptyMap());
                }
            }

            String nestedClassName = nestedConfiguration.className();
            if (nestedClassName == null || nestedClassName.isBlank()) {
                if (!annotation.required()) {
                    return null;
                }
            }

            Set<Option> options = createOptions(annotation.valueClass(), annotation.required());
            Object instance = getInstance(annotation.valueClass(), nestedConfiguration, options);

            if (!annotation.valueClass().isAssignableFrom(instance.getClass())) {
                throwIncompatibleAnnotation(annotation.valueClass(), target, instance);
            }

            return instance;

        }

    }

    private static final class InstanceMapSource extends Source<MCRInstanceMap, Map<String, Object>> {

        private final MCRInstanceMap annotation;

        private InstanceMapSource(MCRInstanceMap annotation) {
            this.annotation = annotation;
        }

        @Override
        public SourceType type() {
            return SourceType.INSTANCE_MAP;
        }

        @Override
        public Class<MCRInstanceMap> annotationClass() {
            return MCRInstanceMap.class;
        }

        @Override
        public MCRInstanceMap annotation() {
            return annotation;
        }

        @Override
        public int order() {
            return annotation.order();
        }

        @Override
        public Set<TargetType> allowedTargetTypes() {
            return EnumSet.allOf(TargetType.class);
        }

        @Override
        public Class<?> valueClass() {
            return Map.class;
        }

        @Override
        public Map<String, Object> get(MCRInstanceConfiguration configuration, Target<?> target) {

            Map<String, MCRInstanceConfiguration> nestedConfigurationMap = nestedConfigurationMap(configuration);

            MCRSentinel sentinel = annotation.sentinel();
            if (sentinel.enabled()) {
                Set<String> keys = new HashSet<>(nestedConfigurationMap.keySet());
                for (String key : keys) {
                    boolean sentinelValue = sentinel.defaultValue();
                    String configurationValue = nestedConfigurationMap.get(key).properties().get(sentinel.name());
                    if (configurationValue != null) {
                        sentinelValue = Boolean.parseBoolean(configurationValue);
                    }
                    if (!sentinelValue) {
                        nestedConfigurationMap.remove(key);
                    }
                }
            }

            if (nestedConfigurationMap.isEmpty() && annotation.required()) {
                throw new MCRConfigurationException("Missing configuration entries like: "
                    + getExampleName(configuration, "A") + ", "
                    + getExampleName(configuration, "B") + ", ...");
            }

            Set<Option> options = createOptions(annotation.valueClass(), annotation.required());
            Map<String, Object> instanceMap = nestedConfigurationMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                    getInstance(annotation.valueClass(), entry.getValue(), options)));

            instanceMap.values().forEach(instance -> {
                if (!annotation.valueClass().isAssignableFrom(instance.getClass())) {
                    throwIncompatibleAnnotation(annotation.valueClass(), target, instance);
                }
            });

            return instanceMap;

        }

        private Map<String, MCRInstanceConfiguration> nestedConfigurationMap(MCRInstanceConfiguration configuration) {
            if (Objects.equals("", annotation.name())) {
                return configuration.nestedConfigurationMap();
            } else {
                return configuration.nestedConfigurationMap(annotation().name());
            }
        }

        private String getExampleName(MCRInstanceConfiguration configuration, String example) {
            if (Objects.equals("", annotation.name())) {
                return configuration.name().subName(example).actual();
            } else {
                return configuration.name().subName(annotation.name() + "." + example).actual();
            }
        }

    }

    private static final class InstanceListSource extends Source<MCRInstanceList, List<Object>> {

        private final MCRInstanceList annotation;

        private InstanceListSource(MCRInstanceList annotation) {
            this.annotation = annotation;
        }

        @Override
        public SourceType type() {
            return SourceType.INSTANCE_LIST;
        }

        @Override
        public Class<MCRInstanceList> annotationClass() {
            return MCRInstanceList.class;
        }

        @Override
        public MCRInstanceList annotation() {
            return annotation;
        }

        @Override
        public int order() {
            return annotation.order();
        }

        @Override
        public Set<TargetType> allowedTargetTypes() {
            return EnumSet.allOf(TargetType.class);
        }

        @Override
        public Class<?> valueClass() {
            return List.class;
        }

        @Override
        public List<Object> get(MCRInstanceConfiguration configuration, Target<?> target) {

            List<MCRInstanceConfiguration> nestedConfigurationList = nestededConfigurationList(configuration);

            MCRSentinel sentinel = annotation.sentinel();
            if (sentinel.enabled()) {
                for (int i = nestedConfigurationList.size() - 1; i >= 0; i--) {
                    boolean sentinelValue = sentinel.defaultValue();
                    String configurationValue = nestedConfigurationList.get(i).properties().get(sentinel.name());
                    if (configurationValue != null) {
                        sentinelValue = Boolean.parseBoolean(configurationValue);
                    }
                    if (!sentinelValue) {
                        nestedConfigurationList.remove(i);
                    }
                }
            }

            if (nestedConfigurationList.isEmpty() && annotation.required()) {
                throw new MCRConfigurationException("Missing configuration entries like: "
                    + getExampleName(configuration, "1") + ", "
                    + getExampleName(configuration, "2") + ", ...");
            }

            Set<Option> options = createOptions(annotation.valueClass(), annotation.required());
            List<Object> instanceList = nestedConfigurationList.stream().map(c ->
                (Object) getInstance(annotation.valueClass(), c, options)).toList();

            instanceList.forEach(instance -> {
                if (!annotation.valueClass().isAssignableFrom(instance.getClass())) {
                    throwIncompatibleAnnotation(annotation.valueClass(), target, instance);
                }
            });

            return instanceList;

        }

        private List<MCRInstanceConfiguration> nestededConfigurationList(MCRInstanceConfiguration configuration) {
            if (Objects.equals("", annotation.name())) {
                return configuration.nestedConfigurationList();
            } else {
                return configuration.nestedConfigurationList(annotation().name());
            }
        }

        private String getExampleName(MCRInstanceConfiguration configuration, String example) {
            if (Objects.equals("", annotation.name())) {
                return configuration.name().subName(example).actual();
            } else {
                return configuration.name().subName(annotation.name() + "." + example).actual();
            }
        }

    }

    private static final class PostConstructionSource extends Source<MCRPostConstruction, String> {

        private final MCRPostConstruction annotation;

        private PostConstructionSource(MCRPostConstruction annotation) {
            this.annotation = annotation;
        }

        @Override
        public SourceType type() {
            return SourceType.POST_CONSTRUCTION;
        }

        @Override
        public Class<MCRPostConstruction> annotationClass() {
            return MCRPostConstruction.class;
        }

        @Override
        public MCRPostConstruction annotation() {
            return annotation;
        }

        @Override
        public int order() {
            return annotation.order();
        }

        @Override
        public Set<TargetType> allowedTargetTypes() {
            return EnumSet.allOf(TargetType.class);
        }

        @Override
        public Class<?> valueClass() {
            return String.class;
        }

        @Override
        public String get(MCRInstanceConfiguration configuration, Target<?> target) {
            return switch (annotation.value()) {
                case ACTUAL -> configuration.name().actual();
                case CANONICAL -> configuration.name().canonical();
            };
        }

    }

    private static final class Injector<T, A extends Annotation> implements Comparable<Injector<?, ?>> {

        private static final Comparator<Injector<?, ?>> COMPARATOR = Comparator
            .comparingInt((ToIntFunction<Injector<?, ?>>) injector -> injector.target.type().order)
            .thenComparingInt(injector -> injector.source.type().order)
            .thenComparingInt(injector -> injector.source.order());

        private final Target<T> target;

        private final Source<A, ?> source;

        private Injector(Target<T> target, Source<A, ?> source) {
            this.target = target;
            this.source = source;
        }

        public void inject(T instance, MCRInstanceConfiguration configuration) {
            Object value = source.get(configuration, target);
            if (value != null) {
                target.set(instance, value);
            }
        }

        @Override
        public int compareTo(Injector<?, ?> other) {
            return COMPARATOR.compare(this, other);
        }

    }

    public enum Option {

        /**
         * If a class is required to be in the configuration properties (for example, because of usage of
         * {@link MCRConfiguration2#getInstanceOfOrThrow(Class, String)} or because of an annotation with
         * required=true) and the expected super class is a final class (i.e. if the class name that has
         * to be present in the properties could ever be only exactly the class name of that final class),
         * add corresponding entries to the properties during instantiation, if not present anyway.
         */
        ADD_IMPLICIT_CLASS_PROPERTIES;

    }


}
