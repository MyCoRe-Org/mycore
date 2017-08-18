/**
 *
 */
package org.mycore.saxon;

import java.util.stream.Stream;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.ExtensionFunction;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
class MCRExtensionFunctionDefinitionWrapper extends ExtensionFunctionDefinition {

    private ExtensionFunction f;

    private SequenceType resultType;

    private SequenceType[] argumentTypes;

    /**
     *
     */
    public MCRExtensionFunctionDefinitionWrapper(ExtensionFunction f) {
        this.f = f;
        this.resultType = f.getResultType().getUnderlyingSequenceType();
        argumentTypes = Stream.of(f.getArgumentTypes())
            .map(t -> t.getUnderlyingSequenceType())
            .toArray(i -> new SequenceType[i]);
    }

    /* (non-Javadoc)
     * @see net.sf.saxon.lib.ExtensionFunctionDefinition#getFunctionQName()
     */
    @Override
    public StructuredQName getFunctionQName() {
        return f.getName().getStructuredQName();
    }

    /* (non-Javadoc)
     * @see net.sf.saxon.lib.ExtensionFunctionDefinition#getArgumentTypes()
     */
    @Override
    public SequenceType[] getArgumentTypes() {
        return argumentTypes;
    }

    /* (non-Javadoc)
     * @see net.sf.saxon.lib.ExtensionFunctionDefinition#getResultType(net.sf.saxon.value.SequenceType[])
     */
    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return resultType;
    }

    /* (non-Javadoc)
     * @see net.sf.saxon.lib.ExtensionFunctionDefinition#makeCallExpression()
     */
    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {

            @Override
            public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                XdmValue[] args = new XdmValue[arguments.length];
                for (int i = 0; i < args.length; i++) {
                    GroundedValue val = SequenceExtent.makeSequenceExtent(arguments[i].iterate());
                    args[i] = XdmValue.wrap(val);
                }
                try {
                    XdmValue result = f.call(args);
                    return result.getUnderlyingValue();
                } catch (SaxonApiException e) {
                    throw new XPathException(e);
                }
            }
        };
    }

}
