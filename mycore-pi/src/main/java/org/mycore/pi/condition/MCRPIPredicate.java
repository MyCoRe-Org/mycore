package org.mycore.pi.condition;

import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import org.mycore.datamodel.metadata.MCRBase;

public interface MCRPIPredicate extends Predicate<MCRBase> {
    default Map<String, String > getProperties(){
        return Collections.emptyMap();
    }
}
