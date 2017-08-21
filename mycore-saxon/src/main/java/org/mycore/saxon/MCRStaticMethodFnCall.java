/**
 *
 */
package org.mycore.saxon;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;

import net.sf.saxon.Configuration;
import net.sf.saxon.dom.DOMObjectModel;
import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.expr.PJConverter;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.om.LazySequence;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceExtent;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRStaticMethodFnCall extends ExtensionFunctionCall {

    private Optional<JPConverter> objectConverter;

    public MCRStaticMethodFnCall(Optional<JPConverter> objectConverter) {
        this.objectConverter = objectConverter;
    }

    /* (non-Javadoc)
     * @see net.sf.saxon.lib.ExtensionFunctionCall#call(net.sf.saxon.expr.XPathContext, net.sf.saxon.om.Sequence[])
     */
    @Override
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        Optional<InvokableMethod> method = getMethod(context, arguments);
        Object nativeResult;
        try {
            nativeResult = method.orElseThrow(() -> new XPathException("Could not find a method to invoke!")).invoke();
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new XPathException(e);
        }
        Optional<JPConverter> myObjectConverter = this.objectConverter;
        if (!myObjectConverter.isPresent()) {
            myObjectConverter = Optional
                .ofNullable(MCRSaxonUtils.getObjectConverter(nativeResult.getClass(), context.getConfiguration()));
        }
        return myObjectConverter
            .orElseThrow(() -> new XPathException("Cannot convert " + nativeResult.getClass() + " to Saxon Sequence!"))
            .convert(nativeResult, context);
    }

    private MCRStaticMethodFnDefinition getStaticMethodFnDefinition() {
        return (MCRStaticMethodFnDefinition) getDefinition();
    }

    private Optional<InvokableMethod> getMethod(XPathContext context, Sequence[] arguments) {
        LogManager.getLogger().debug(() -> Arrays.toString(arguments));
        MCRStaticMethodFnDefinition definition = getStaticMethodFnDefinition();
        return Stream.of(definition.getMethods())
            .filter(m -> (m.getParameterCount() == arguments.length || m.getParameterCount() == arguments.length + 1))
            .map(m -> toInvokeableMethod(context, m, arguments))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private Optional<InvokableMethod> toInvokeableMethod(XPathContext context, Method m, Sequence[] arguments) {
        Class<?>[] checkParams = m.getParameterTypes();
        //allow first parameter to be XPathContext and shift all other to right
        if (m.getParameterCount() > arguments.length) {
            Class<?> firstParam = m.getParameterTypes()[0];
            if (!XPathContext.class.isAssignableFrom(firstParam)) {
                return Optional.empty();
            }
            checkParams = Arrays.copyOfRange(m.getParameterTypes(), 1, m.getParameterCount());
        }
        assert (arguments.length == checkParams.length);
        Object[] params = new Object[m.getParameterCount()];
        int paramOffset = 0;
        if (params.length != arguments.length) {
            Class<?> firstParam = m.getParameterTypes()[0];
            if (!XPathContext.class.isAssignableFrom(firstParam)) {
                params[0] = context;
                paramOffset++;
            }
        }
        Configuration configuration = context.getConfiguration();
        TypeHierarchy typeHierarchy = configuration.getTypeHierarchy();
        try {
            for (int i = 0; i < checkParams.length; i++) {
                Sequence sequence = arguments[i];
                Class<?> param = checkParams[i];
                LogManager.getLogger().debug("Conversion test {}", param.getName());
                PJConverter converter;
                converter = DOMObjectModel.getInstance().getPJConverter(param);
                if (converter == null) {
                    if (sequence instanceof LazySequence) {
                        sequence = SequenceExtent.makeSequenceExtent(sequence.iterate());
                    }
                    ItemType itemType = SequenceTool.getItemType(sequence, typeHierarchy);
                    int cardinality = SequenceTool.getCardinality(sequence);
                    converter = MCRSaxonUtils
                        .getSequenceConverter(configuration, itemType, cardinality, param).get();
                }
                params[i + paramOffset] = converter.convert(sequence, param, context);
            }
        } catch (RuntimeException | XPathException e) {
            LogManager.getLogger().debug("Conversion test failed", e);
            return Optional.empty();
        }
        //we have a good candidate;
        return Optional.of(new InvokableMethod(m, params));
    }

    private static class InvokableMethod {
        Method m;

        Object[] params;

        InvokableMethod(Method m, Object[] params) {
            this.m = m;
            this.params = params;
        }

        public Object invoke() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            return m.invoke(null, params);
        }
    }

}
