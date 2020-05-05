package org.mycore.pi.condition;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIJobService;

public abstract class MCRPICombinedPredicate extends MCRPIPredicateBase {

    public MCRPICombinedPredicate(String propertyPrefix) {
        super(propertyPrefix);
    }

    protected Stream<Predicate<MCRBase>> getCombinedPredicates() {
        final Map<String, String> properties = getProperties();
        return properties
            .keySet()
            .stream()
            .filter(p -> {
                return !p.contains("."); // do not handle sub properties
            })
            .map(Integer::parseInt)
            .sorted()
            .map(Object::toString)
            .map((subProperty) -> {
                return MCRPIJobService.getPredicateInstance(getPropertyPrefix() + subProperty);
            });
    }
}
