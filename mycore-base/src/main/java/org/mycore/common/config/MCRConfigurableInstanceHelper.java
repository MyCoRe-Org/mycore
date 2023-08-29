/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.MCRClassTools;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;

import jakarta.inject.Singleton;

/**
 * Creates Objects which are configured with properties.
 *
 * @author Sebastian Hofmann
 */
class MCRConfigurableInstanceHelper {

    private static final ConcurrentMap<Class<?>, ClassInfo<?>> INFOS = new ConcurrentHashMap<>();

    /**
     * Checks if a class is annotated with {@link Singleton}.
     *
     * @param property the configuration property which contains the class
     * @return true if the class in the property is annotated with {@link Singleton}
     */
    public static boolean isSingleton(String property) {
        return MCRConfiguration2.getString(property).stream()
            .anyMatch(propertyVal -> getClass(property, propertyVal).getDeclaredAnnotation(Singleton.class) != null);
    }

    /**
     * Creates a configured instance of a class .
     *
     * @param name the property which contains the class name
     * @return the configured instance of T
     * @throws MCRConfigurationException if the property is not right configured.
     */
    public static <T> Optional<T> getInstance(String name) throws MCRConfigurationException {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName(name);
        if (configuration.className() != null) {
            return Optional.of(getInstance(configuration));
        } else {
            return Optional.empty();
        }
    }

    public static <T> T getInstance(MCRInstanceConfiguration configuration) throws MCRConfigurationException {
        String className = configuration.className();
        if (className == null) {
            throw new MCRConfigurationException("Missing property: " + configuration.name().actual());
        }
        Class<T> targetClass = getClass(configuration.name().actual(), configuration.className());
        return getInstance(targetClass, configuration);
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
    private static <T> T getInstance(Class<T> targetClass, MCRInstanceConfiguration configuration) {
        MCRConfigurationProxy productAnnotation = targetClass.getDeclaredAnnotation(MCRConfigurationProxy.class);
        if (productAnnotation != null) {
            Class<Supplier<T>> proxyClass = (Class<Supplier<T>>) productAnnotation.proxyClass();
            return getInfo(proxyClass).getInstance(configuration).get();
        } else {
            return getInfo(targetClass).getInstance(configuration);
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
     *       <li>{@link MCRInstance}</li>
     *       <li>{@link MCRInstanceMap}</li>
     *       <li>{@link MCRInstanceList}</li>
     *       <li>{@link MCRPostConstruction}</li>
     *     </ol>
     *     Since {@link MCRProperty} produces (based on the value of {@link MCRProperty#name()}) different kinds 
     *     of values ({@link String} or {@link Map}), two different source implementations ({@link PropertySource}, 
     *     {@link AllPropertiesSource}) exists for that annotation.
     *     <br/>
     *     Targets are:
     *     <ul>
     *       <li>public fields,</li>
     *       <li>public methods with no parameter or</li>
     *       <li>public methods with one parameter.</li>
     *     </ul>
     *     The list of injectors is created by
     *     <ol>
     *       <li>
     *         examining all possible fields and methods
     *         (as {@link Injectable}, in {@link ClassInfo#findInjectors(Class)}),
     *       </li>
     *       <li>
     *         checking, if such a target is annotated with a supported annotation
     *         (using {@link AnnotationMapper#injectableToSource(Injectable)}),
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
     *     The list is ordered by the type of target (fields first), the source annotation (see list above)
     *     and lastly the order attribute of the source annotations (i.e. {@link MCRProperty#order()}). 
     *   </li>
     * </ul>
     * <p>
     * It is intended that instances of this class are cached, such that the information about the target class
     * only needs to be gathered once.
     */
    private static class ClassInfo<T> {

        private final Class<T> targetClass;

        private final Supplier<T> factory;

        private final List<Injector<T, ?>> injectors;

        private ClassInfo(Class<T> targetClass) {
            this.targetClass = targetClass;
            this.factory = getFactory(targetClass);
            this.injectors = findInjectors(targetClass);
        }

        private Supplier<T> getFactory(Class<T> targetClass) {
            try {
                return createConstructorFactory(findDefaultConstructor(targetClass));
            } catch (NoSuchMethodException e) {
                return createFactoryMethodFactory(findFactoryMethod(targetClass));
            }
        }

        private Constructor<T> findDefaultConstructor(Class<T> targetClass) throws NoSuchMethodException {
            return targetClass.getConstructor();
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

        private Method findFactoryMethod(Class<T> targetClass) {

            List<Method> factoryMethods = Stream.of(targetClass.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType().isAssignableFrom(targetClass))
                .filter(method -> method.getParameterTypes().length == 0)
                .filter(method -> !method.isVarArgs())
                .filter(method -> method.getName().toLowerCase(Locale.ROOT).contains("instance"))
                .toList();

            if (factoryMethods.isEmpty()) {
                throw new MCRConfigurationException("Class " + targetClass.getName()
                    + " has no public, parameterless constructor and no suitable"
                    + " (public, static, matching return type, parameterless, name containing 'instance')"
                    + " factory method"
                );
            }

            if (factoryMethods.size() != 1) {
                throw new MCRConfigurationException("Class " + targetClass.getName()
                    + " has no public, parameterless constructor but multiple suitable"
                    + " (public, static, matching return type, parameterless, name containing 'instance')"
                    + " factory methods (" + methodNames(factoryMethods) + ")"
                );
            }

            return factoryMethods.get(0);

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

            Source<?, ?> source = sources.get(0);

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

        public T getInstance(MCRInstanceConfiguration configuration) {
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

    private static class NoParameterMethodTarget<T> extends MethodTarget<T> {

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

    private static class SingleParameterMethodTarget<T> extends MethodTarget<T> {

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

    private enum SourceType {

        PROPERTY(0, new AnnotationMapper<MCRProperty>() {

            @Override
            public Class<MCRProperty> annotationClass() {
                return MCRProperty.class;
            }

            @Override
            public Source<MCRProperty, ?> annotationToSource(MCRProperty annotation) {
                if (Objects.equals("*", annotation.name())) {
                    return new AllPropertiesSource(annotation);
                } else {
                    return new PropertySource(annotation);
                }
            }

        }),

        INSTANCE(0, new AnnotationMapper<MCRInstance>() {

            @Override
            public Class<MCRInstance> annotationClass() {
                return MCRInstance.class;
            }

            @Override
            public Source<MCRInstance, ?> annotationToSource(MCRInstance annotation) {
                return new InstanceSource(annotation);
            }

        }),

        INSTANCE_MAP(0, new AnnotationMapper<MCRInstanceMap>() {

            @Override
            public Class<MCRInstanceMap> annotationClass() {
                return MCRInstanceMap.class;
            }

            @Override
            public Source<MCRInstanceMap, ?> annotationToSource(MCRInstanceMap annotation) {
                return new InstanceMapSource(annotation);
            }

        }),


        INSTANCE_LIST(0, new AnnotationMapper<MCRInstanceList>() {

            @Override
            public Class<MCRInstanceList> annotationClass() {
                return MCRInstanceList.class;
            }

            @Override
            public Source<MCRInstanceList, ?> annotationToSource(MCRInstanceList annotation) {
                return new InstanceListSource(annotation);
            }

        }),

        POST_CONSTRUCTION(1, new AnnotationMapper<MCRPostConstruction>() {

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

        SourceType(int order, AnnotationMapper<? extends Annotation> mapper) {
            this.order = order;
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

    }

    private static class PropertySource extends Source<MCRProperty, String> {

        protected final MCRProperty annotation;

        private PropertySource(MCRProperty annotation) {
            this.annotation = annotation;
        }

        @Override
        public final SourceType type() {
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
        public final int order() {
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

    private static class AllPropertiesSource extends Source<MCRProperty, Map<String, String>> {

        protected final MCRProperty annotation;

        private AllPropertiesSource(MCRProperty annotation) {
            this.annotation = annotation;
        }

        @Override
        public final SourceType type() {
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
        public final int order() {
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
            if (!annotation.absolute()) {
                return configuration.properties();
            } else {
                return configuration.fullProperties();
            }
        }

    }

    private static class InstanceSource extends Source<MCRInstance, Object> {

        protected final MCRInstance annotation;

        private InstanceSource(MCRInstance annotation) {
            this.annotation = annotation;
        }

        @Override
        public final SourceType type() {
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
        public final int order() {
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

            if (nestedConfiguration.className() == null) {
                if (annotation.required()) {
                    throw new MCRConfigurationException("Missing property: " + nestedConfiguration.name().actual());
                } else {
                    return null;
                }
            }

            Object instance = getInstance(nestedConfiguration);

            if (!annotation.valueClass().isAssignableFrom(instance.getClass())) {
                throw new MCRConfigurationException("Configured instance of class " + instance.getClass().getName()
                    + " is incompatible with annotation value class " + annotation.valueClass().getName()
                    + " for target " + targetTypeName(target) + " '" + target.name()
                    + "' in configured class " + target.declaringClass().getName());
            }

            return instance;

        }

    }

    private static class InstanceMapSource extends Source<MCRInstanceMap, Map<String, Object>> {

        protected final MCRInstanceMap annotation;

        private InstanceMapSource(MCRInstanceMap annotation) {
            this.annotation = annotation;
        }

        @Override
        public final SourceType type() {
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
        public final int order() {
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

            if (nestedConfigurationMap.isEmpty() && annotation.required()) {
                throw new MCRConfigurationException("Missing configuration entries like: "
                    + getExampleName(configuration, "A") + ", "
                    + getExampleName(configuration, "B") + ", ...");
            }

            Map<String, Object> instanceMap = nestedConfigurationMap.entrySet()
                .stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> getInstance(entry.getValue())));

            instanceMap.values().forEach(instance -> {
                if (!annotation.valueClass().isAssignableFrom(instance.getClass())) {
                    throw new MCRConfigurationException("Configured instance of class " + instance.getClass().getName()
                        + " is incompatible with annotation value class " + annotation.valueClass().getName()
                        + " for target " + targetTypeName(target) + " '" + target.name()
                        + "' in configured class " + target.declaringClass().getName());
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

    private static class InstanceListSource extends Source<MCRInstanceList, List<Object>> {

        protected final MCRInstanceList annotation;

        private InstanceListSource(MCRInstanceList annotation) {
            this.annotation = annotation;
        }

        @Override
        public final SourceType type() {
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
        public final int order() {
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

            if (nestedConfigurationList.isEmpty() && annotation.required()) {
                throw new MCRConfigurationException("Missing configuration entries like: "
                    + getExampleName(configuration, "1") + ", "
                    + getExampleName(configuration, "2") + ", ...");
            }

            List<Object> instanceList = nestedConfigurationList
                .stream().map(MCRConfigurableInstanceHelper::getInstance).toList();

            instanceList.forEach(instance -> {
                if (!annotation.valueClass().isAssignableFrom(instance.getClass())) {
                    throw new MCRConfigurationException("Configured instance of class " + instance.getClass().getName()
                        + " is incompatible with annotation value class " + annotation.valueClass().getName()
                        + " for target " + targetTypeName(target) + " '" + target.name()
                        + "' in configured class " + target.declaringClass().getName());
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

    private static class PostConstructionSource extends Source<MCRPostConstruction, String> {

        protected final MCRPostConstruction annotation;

        private PostConstructionSource(MCRPostConstruction annotation) {
            this.annotation = annotation;
        }

        @Override
        public final SourceType type() {
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
        public final int order() {
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
            return configuration.name().actual();
        }

    }

    private static final class Injector<T, A extends Annotation> implements Comparable<Injector<?, ?>> {

        private static final Comparator<Injector<?, ?>> COMPARATOR = Comparator
            .comparing((Function<Injector<?, ?>, Integer>) injector -> injector.target.type().order)
            .thenComparing(injector -> injector.source.type().order)
            .thenComparing(injector -> injector.source.order());

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

}
