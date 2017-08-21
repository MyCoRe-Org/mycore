/**
 *
 */
package org.mycore.saxon;

import java.util.Optional;

import net.sf.saxon.Configuration;
import net.sf.saxon.dom.DOMObjectModel;
import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.expr.PJConverter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceType;

/**
 * @author THomas Scheffler (yagee)
 *
 */
class MCRSaxonUtils {

    private static SequenceType getInternalOrNodeSequenceType(Class<?> nativeType) {
        SequenceType internal = PJConverter.getEquivalentSequenceType(nativeType);
        if (internal == null) {
            return DOMObjectModel.getInstance().getPJConverter(nativeType) != null ? SequenceType.NODE_SEQUENCE : null;
        }
        return internal;
    }

    static Optional<SequenceType> getSequenceType(Class<?> nativeType) {
        return Optional.ofNullable(getInternalOrNodeSequenceType(nativeType));
    }

    static JPConverter getObjectConverter(Class<?> nativeType, Configuration config) {
        JPConverter converter = DOMObjectModel.getInstance().getJPConverter(nativeType, config);
        return (converter != null) ? converter : JPConverter.allocate(nativeType, null, config);
    }

    static Optional<PJConverter> getSequenceConverter(Configuration config, ItemType itemType, int cardinality,
        Class<?> targetClass) throws XPathException {
        return Optional.of(PJConverter.allocate(config, itemType, cardinality, targetClass));
    }

}
