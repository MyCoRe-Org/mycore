package org.mycore.restapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark REST methods that require a property activation
 * @author Thomas Scheffler (yagee)
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)

public @interface MCRApiDraft {

    /**
     * Used to construct the property name
     *
     * The returned value is prefixed by <code>MCR.RestApi.Draft.</code>
     */
    String value();
}
