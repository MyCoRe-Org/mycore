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

package org.mycore.common;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.lang.annotation.Annotation;
import java.util.Optional;

public class MCRTestAnnotationWatcher<A extends Annotation> extends TestWatcher {

    private final Class<A> annotationType;

    private volatile A annotation;

    public MCRTestAnnotationWatcher(Class<A> annotationType) {
        this.annotationType = annotationType;
    }

    protected void starting(Description d) {
        this.annotation = d.getAnnotation(annotationType);
    }

    public Optional<A> getAnnotation() {
        return Optional.ofNullable(this.annotation);
    }

}
