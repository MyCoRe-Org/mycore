/**
 *
 */
package org.mycore.saxon;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRSuppressWarning;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.ExtensionFunction;

/**
 * Extends Saxons TransformerFactoryImpl and registers extension functions automatically
 * <dl>
 *  <dt>MCR.Saxon.ExtensionFunctions.Impl</dt>
 *  <dd>List of classes implementing {@link ExtensionFunction}</dd>
 *  <dt>MCR.Saxon.ExtensionFunctions.Definition</dt>
 *  <dd>List of classes extending {@link ExtensionFunctionDefinition}</dd>
 *  <dt>MCR.Saxon.ExtensionFunctions.Static</dt>
 *  <dd>List of classes providing <code>public static</code> methods as extension functions</dd>
 * </dl>
 *
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTransformerFactoryImpl extends TransformerFactoryImpl {

    public static final String EXTENSION_FUNCTION_CLASSES_PROPERTY = "MCR.Saxon.ExtensionFunctions.Impl";

    public static final String EXTENSION_FUNCTION_DEFINITION_PROPERTY = "MCR.Saxon.ExtensionFunctions.Definition";

    public static final String EXTENSION_FUNCTION_STATIC_PROPERTY = "MCR.Saxon.ExtensionFunctions.Static";

    private static LoadingCache<String, StructuredQName> qNameCache = CacheBuilder.newBuilder()
        .build(CacheLoader.from(cacheKey -> {
            String[] cacheKeyValue = cacheKey.split("#");
            return new StructuredQName("", toURI(cacheKeyValue[0]), cacheKeyValue[1]);
        }));

    /**
     *
     */
    public MCRTransformerFactoryImpl() {
        super();
        init();
    }

    /**
     * @param config
     */
    public MCRTransformerFactoryImpl(Configuration config) {
        super(config);
        init();
    }

    private void init() {
        registerExtensionFunctions();
        registerStaticMethods();
    }

    private void registerExtensionFunctions() {
        if (MCRConfiguration2.getString(EXTENSION_FUNCTION_CLASSES_PROPERTY).isPresent()) {
            List<ExtensionFunction> extensionFunction = MCRConfiguration2
                .getInstancesOf(EXTENSION_FUNCTION_CLASSES_PROPERTY);
            extensionFunction.forEach(this::register);
        }
        if (MCRConfiguration2.getString(EXTENSION_FUNCTION_DEFINITION_PROPERTY).isPresent()) {
            List<ExtensionFunctionDefinition> extensionFunction = MCRConfiguration2
                .getInstancesOf(EXTENSION_FUNCTION_DEFINITION_PROPERTY);
            extensionFunction.forEach(this::register);
        }
    }

    private void register(ExtensionFunctionDefinition t) {
        LogManager.getLogger().debug(() -> "Register function: " + t.getFunctionQName().getClarkName());
        getConfiguration().registerExtensionFunction(t);
    }

    private void register(ExtensionFunction t) {
        register(new MCRExtensionFunctionDefinitionWrapper(t));
    }

    private void registerStaticMethods() {
        if (MCRConfiguration2.getString(EXTENSION_FUNCTION_STATIC_PROPERTY).isPresent()) {
            List<String> staticClassNames = MCRConfiguration2.getStrings(EXTENSION_FUNCTION_STATIC_PROPERTY);
            Map<StructuredQName, List<Method>> staticFnMethods = staticClassNames.stream()
                .map(MCRTransformerFactoryImpl::toClass)
                .flatMap(MCRTransformerFactoryImpl::getStaticMethods)
                .filter(this::isCompatible)
                .collect(Collectors.groupingBy(MCRTransformerFactoryImpl::toQName));
            staticFnMethods.entrySet().stream()
                .map(e -> toFnDefinition(e.getKey(), e.getValue()))
                .forEach(this::register);
        }
    }

    private MCRStaticMethodFnDefinition toFnDefinition(StructuredQName qName, List<Method> methodList) {
        Method[] methods = methodList.toArray(new Method[methodList.size()]);
        return new MCRStaticMethodFnDefinition(getConfiguration(), qName, methods);
    }

    private static StructuredQName toQName(Method m) {
        String cacheKey = m.getDeclaringClass().getName() + "#" + m.getName();
        return qNameCache.getUnchecked(cacheKey);
    }

    private static String toURI(String className) {
        return ("mcr:" + className).intern();
    }

    private static Class<?> toClass(String t) {
        try {
            return Class.forName(t);
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException("Extension class not found: " + t);
        }
    }

    private static Stream<Method> getStaticMethods(Class<?> c) {
        return Arrays.stream(c.getMethods())
            .filter(method -> method.getDeclaringClass().equals(c))
            .filter(method -> Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers()));
    }

    private boolean isCompatible(Method m) {
        boolean paramCompatible = Stream.of(m.getParameterTypes())
            .map(MCRSaxonUtils::getSequenceType)
            .map(Optional::isPresent)
            .allMatch(t -> t);
        boolean compatible = paramCompatible
            && MCRSaxonUtils.getObjectConverter(m.getReturnType(), getConfiguration()) != null;
        if (!compatible) {
            if (m.isAnnotationPresent(MCRSuppressWarning.class)) {
                MCRSuppressWarning suppressWarning = m.getAnnotation(MCRSuppressWarning.class);
                if (Stream.of(suppressWarning.value())
                    .filter(MCRSuppressWarning.SAXON::equals)
                    .findAny()
                    .isPresent()) {
                    return false;
                }
            }
            LogManager.getLogger().info(
                "Not a compatible extension function: {}\nUse  '@MCRSuppressWarning(\"saxon\")' to hide this warning",
                m);
        }
        return compatible;
    }

}
