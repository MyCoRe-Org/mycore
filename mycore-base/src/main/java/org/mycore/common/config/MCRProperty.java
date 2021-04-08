package org.mycore.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation tells the {@link MCRConfigurableInstanceHelper} which properties need to be assigned to which field
 * or method. All annotated members need to be public. The fields should always have the type {@link String} and if you
 * need a custom type, then you can annotate a method with a single parameter of type {@link String}, which can then
 * create/retrieve the object and assign it to your field.
 *
 * @author Sebastian Hofmann
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Inherited
public @interface MCRProperty {

    /**
     * @return The name of property
     */
    String name();

    /**
     * @return true if the property has to be present in the properties. {@link MCRConfigurationException} is thrown
     * if the property is required but not present.
     */
    boolean required() default true;


    /**
     * @return true if the property is absolute and not specific for this instance e.G. MCR.NameOfProject
     */
    boolean absolute() default false;
}
