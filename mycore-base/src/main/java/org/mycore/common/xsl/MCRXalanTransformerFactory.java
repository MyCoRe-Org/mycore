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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.xalan.processor.TransformerFactoryImpl;
import org.apache.xalan.templates.StylesheetRoot;
import org.apache.xalan.transformer.TransformerImpl;
import org.xml.sax.ContentHandler;

public class MCRXalanTransformerFactory extends TransformerFactoryImpl {

    @Override
    public Templates newTemplates(Source source) throws TransformerConfigurationException {
        return new TemplatesFacade(super.newTemplates(source));
    }

    private static class TemplatesFacade implements Templates {

        private final Templates delegate;

        public TemplatesFacade(Templates delegate) {
            this.delegate = delegate;
        }

        @Override
        public Transformer newTransformer() throws TransformerConfigurationException {
            TransformerImpl transformerImpl = (TransformerImpl) delegate.newTransformer();
            return new TransformerFacade(transformerImpl, transformerImpl.getStylesheet());
        }

        @Override
        public Properties getOutputProperties() {
            return this.delegate.getOutputProperties();
        }
    }

    private static class TransformerFacade extends TransformerImpl {

        private final TransformerImpl delegate;

        private TransformerFacade(TransformerImpl delegate, StylesheetRoot stylesheetRoot) {
            super(stylesheetRoot);
            this.delegate = delegate;
        }

        @Override
        public void setParameter(String name, Object object) {
            delegate.setParameter(name, null, object);
        }

        @Override
        public void transform(Source source, Result result) throws TransformerException {
            delegate.transform(source, result);
        }

        @Override
        public Object getParameter(String s) {
            return delegate.getParameter(s);
        }

        @Override
        public void clearParameters() {
            delegate.clearParameters();
        }

        @Override
        public void setURIResolver(URIResolver uriResolver) {
            delegate.setURIResolver(uriResolver);
        }

        @Override
        public URIResolver getURIResolver() {
            return delegate.getURIResolver();
        }

        @Override
        public void setOutputProperties(Properties properties) {
            delegate.setOutputProperties(properties);
        }

        @Override
        public Properties getOutputProperties() {
            return delegate.getOutputProperties();
        }

        @Override
        public void setOutputProperty(String s, String s1) throws IllegalArgumentException {
            delegate.setOutputProperty(s, s1);
        }

        @Override
        public String getOutputProperty(String s) throws IllegalArgumentException {
            return delegate.getOutputProperty(s);
        }

        @Override
        public void setErrorListener(ErrorListener errorListener) throws IllegalArgumentException {
            delegate.setErrorListener(errorListener);
        }

        @Override
        public ErrorListener getErrorListener() {
            return delegate.getErrorListener();
        }

        @Override
        public ContentHandler getInputContentHandler(boolean doDocFrag) {
            return delegate.getInputContentHandler(doDocFrag);
        }

        @Override
        public ContentHandler getInputContentHandler() {
            return delegate.getInputContentHandler();
        }

    }

}
