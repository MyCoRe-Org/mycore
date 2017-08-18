/**
 *
 */
package org.mycore.common;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, CONSTRUCTOR, PACKAGE })
/**
 * @author Thomas Scheffler (yagee)
 *
 */
public @interface MCRSuppressWarning {
    /**
     * The set of warnings that are to be suppressed by a reflection call of the
     * annotated element.
     *
     * @return the set of warnings to be suppressed
     */
    String[] value();

}
