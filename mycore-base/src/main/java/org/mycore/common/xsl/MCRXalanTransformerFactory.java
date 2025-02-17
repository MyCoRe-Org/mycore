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

package org.mycore.common.xsl;

import java.util.Properties;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TemplatesHandler;

import org.apache.xalan.processor.StylesheetHandler;
import org.apache.xalan.processor.TransformerFactoryImpl;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.transformer.TransformerImpl;

/**
 * A custom {@code TransformerFactory} implementation for Xalan that improves
 * performance when handling parameters by disabling namespace support and avoiding
 * the use of the inefficient {@code m_userParams} vector.
 * <p>
 * In the standard Xalan implementation, parameters are stored in a vector, and each
 * parameter set operation performs a linear search through this vector. This can lead
 * to significant performance degradation when many parameters (such as "mycore" properties)
 * are set (e.g., over 1200 times). To address this, this custom factory disables support
 * for namespace-based parameters and bypasses the internal {@code m_userParams} vector.
 * <p>
 * <b>Key modifications:</b>
 * <ul>
 *   <li>Namespace-based parameters are not supported. Any parameter name beginning with
 *       the character '{' is rejected and will result in an {@link IllegalArgumentException}.</li>
 *   <li>The internal vector-based parameter storage ({@code m_userParams}) is not used;
 *       instead, parameters are set via a call to the superclass method with a {@code null}
 *       namespace.</li>
 * </ul>
 * <p>
 * This implementation provides custom subclasses for the stylesheet handler, templates,
 * and transformer.
 *
 * @see javax.xml.transform.Templates
 * @see javax.xml.transform.Transformer
 */
public class MCRXalanTransformerFactory extends TransformerFactoryImpl {

    @Override
    public TemplatesHandler newTemplatesHandler() throws TransformerConfigurationException {
        return new MyStylesheetHandler(this);
    }

    private static final class MyStylesheetHandler extends StylesheetHandler {

        private MyStylesheetHandler(TransformerFactoryImpl processor) throws TransformerConfigurationException {
            super(processor);
        }

        @Override
        public Templates getTemplates() {
            return new MyTemplates((StylesheetRoot) super.getTemplates());
        }

    }

    private static final class MyTemplates implements Templates {

        StylesheetRoot delegate;

        private MyTemplates(StylesheetRoot delegate) {
            this.delegate = delegate;
        }

        @Override
        public Transformer newTransformer() {
            return new MyTransformerImpl(delegate);
        }

        @Override
        public Properties getOutputProperties() {
            return delegate.getOutputProperties();
        }

    }

    private static final class MyTransformerImpl extends TransformerImpl {

        private MyTransformerImpl(StylesheetRoot stylesheet) {
            super(stylesheet);
        }

        @Override
        public void setParameter(String name, Object value) {
            if (name.charAt(0) == '{') {
                throw new IllegalArgumentException("Namespaces for parameters are not supported: " + name);
            }
            super.setParameter(name, null, value);
        }

    }

}
