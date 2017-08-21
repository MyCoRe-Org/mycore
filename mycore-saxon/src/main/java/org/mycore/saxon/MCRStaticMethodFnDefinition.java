/**
 *
 */
package org.mycore.saxon;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRStaticMethodFnDefinition extends ExtensionFunctionDefinition {

    private int minArgs;

    private int maxArgs;

    private Configuration config;

    private StructuredQName qname;

    private Method[] methods;

    private SequenceType[] argumentTypes;

    private Optional<JPConverter> objectConverter;

    private Optional<SequenceType> returnType;

    /**
     *
     */
    public MCRStaticMethodFnDefinition(Configuration config, StructuredQName qname, Method... methods) {
        IntSummaryStatistics paramCountStats = Stream.of(methods).mapToInt(Method::getParameterCount)
            .summaryStatistics();
        this.minArgs = paramCountStats.getMin();
        this.maxArgs = paramCountStats.getMax();
        this.config = config;
        this.qname = qname;
        this.methods = methods;
        Method firstMethod = methods[0];
        try {
            buildArgumentTypes();
        } catch (RuntimeException e) {
            throw new MCRConfigurationException(
                "Method is not compatible: " + firstMethod.getDeclaringClass().getName() + "#" + firstMethod.getName(),
                e);
        }
        if (Stream.of(methods).map(Method::getReturnType).anyMatch(firstMethod.getReturnType()::isAssignableFrom)) {
            JPConverter jpConverter = MCRSaxonUtils.getObjectConverter(firstMethod.getReturnType(), this.config);
            objectConverter = Optional
                .ofNullable(jpConverter);
            returnType = objectConverter.map(c -> SequenceType.makeSequenceType(c.getItemType(), c.getCardinality()));
            if (!objectConverter.isPresent()) {
                throw new MCRConfigurationException("Method return type " + firstMethod.getReturnType().getName()
                    + " is not compatible: " + firstMethod.getDeclaringClass().getName() + "#" + firstMethod.getName());
            }
        } else {
            objectConverter = Optional.empty();
            returnType = Optional.empty();
        }
    }

    private void buildArgumentTypes() {
        List<SequenceType> argList = Stream.of(methods)
            .collect(ArrayList::new, this::toSequenceTypes, MCRStaticMethodFnDefinition::largest);
        argumentTypes = argList.toArray(new SequenceType[argList.size()]);
    }

    private void toSequenceTypes(List<SequenceType> list, Method m) {
        Stream.of(m.getParameterTypes())
            .map(MCRSaxonUtils::getSequenceType)
            .map(Optional::get)
            .forEachOrdered(list::add);
    }

    private static boolean isSubList(List<?> l1, List<?> l2) {
        return ((l1.size() < l2.size()) ? Collections.indexOfSubList(l1, l2)
            : Collections.indexOfSubList(l2, l1)) != -1;
    }

    private static <T> List<T> largest(List<T> l1, List<T> l2) {
        if (!isSubList(l1, l2)) {
            throw new MCRException("Parameter are not compatible " + l1 + " " + l2);
        }
        return l1.size() < l2.size() ? l2 : l1;
    }

    @Override
    public int getMinimumNumberOfArguments() {
        return minArgs;
    }

    @Override
    public int getMaximumNumberOfArguments() {
        return maxArgs;
    }

    /* (non-Javadoc)
     * @see net.sf.saxon.lib.ExtensionFunctionDefinition#getFunctionQName()
     */
    @Override
    public StructuredQName getFunctionQName() {
        return qname;
    }

    /* (non-Javadoc)
     * @see net.sf.saxon.lib.ExtensionFunctionDefinition#getArgumentTypes()
     */
    @Override
    public SequenceType[] getArgumentTypes() {
        LogManager.getLogger().debug("Arguments {}", qname);
        return argumentTypes;
    }

    /* (non-Javadoc)
     * @see net.sf.saxon.lib.ExtensionFunctionDefinition#getResultType(net.sf.saxon.value.SequenceType[])
     */
    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return returnType.orElseThrow(() -> new MCRException("Multiple return types are currently not supported"));
    }

    /* (non-Javadoc)
     * @see net.sf.saxon.lib.ExtensionFunctionDefinition#makeCallExpression()
     */
    @Override
    public ExtensionFunctionCall makeCallExpression() {
        MCRStaticMethodFnCall call = new MCRStaticMethodFnCall(objectConverter);
        call.setDefinition(this);
        return call;
    }

    Method[] getMethods() {
        return methods;
    }

}
