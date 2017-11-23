/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
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
