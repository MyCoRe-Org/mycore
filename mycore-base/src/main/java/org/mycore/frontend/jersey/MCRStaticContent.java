package org.mycore.frontend.jersey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mycore.frontend.jersey.feature.MCRJerseyDefaultFeature;

/**
 * The MCRStaticContent annotation marks a resource or method to run without
 * a MyCoRe transaction, session or access filter. Basically it disables
 * the {@link MCRJerseyDefaultFeature} for the annotated element.
 * 
 * <p>Use this annotation if you deliver static content.</p>
 * 
 * <p>You can annotate whole resources or just single methods.</p>
 * 
 * @author Matthias Eichner
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MCRStaticContent {

}
