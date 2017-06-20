/**
 * 
 */
package org.mycore.frontend.cli.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.MessageFormat;

import org.mycore.frontend.cli.MCRCommandLineInterface;

/**
 * Annotates a public static method as a command that could be executed via {@link MCRCommandLineInterface}.
 * @author Thomas Scheffler (yagee)
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.METHOD)
public @interface MCRCommand {
    /**
     * The syntax of the command in {@link MessageFormat} syntax
     */
    String syntax();

    /**
     * Help text that should be returned by <code>help</code> command.
     */
    String help() default "";

    /**
     * I18N key for the help text that should be returned by <code>help</code> command.
     */
    String helpKey() default "";

    /**
     * If {@link #syntax()} conflicts, use <code>order</code> to specify in which order the invocation should be tried.
     */
    int order() default 1;
}
