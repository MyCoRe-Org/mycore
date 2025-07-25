/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xml.utils.WrappedRuntimeException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public final class MCRErrorListener implements ErrorListener {
    private static final Logger LOGGER = LogManager.getLogger();

    private TransformerException exceptionThrown;

    private String lastMessage;

    public TransformerException getExceptionThrown() {
        return exceptionThrown;
    }

    private boolean triggerException(TransformerException e) {
        if (exceptionThrown != null) {
            return false;
        } else {
            exceptionThrown = e;
            return true;
        }
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.ErrorListener#warning(javax.xml.transform.TransformerException)
     */
    @Override
    public void warning(TransformerException exception) {
        TransformerException unwrappedException = unwrapException(exception);
        StackTraceElement[] stackTrace = unwrappedException.getStackTrace();
        if (stackTrace.length > 0 && stackTrace[0].getMethodName().equals("message")) {
            //org.apache.xalan.transformer.MsgMgr.message to print a message
            String messageAndLocation = getMyMessageAndLocation(unwrappedException);
            this.lastMessage = messageAndLocation;
            LOGGER.info(messageAndLocation);
        } else {
            LOGGER.warn("Exception while XSL transformation:{}", unwrappedException::getMessageAndLocation);
        }
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.ErrorListener#error(javax.xml.transform.TransformerException)
     */
    @Override
    public void error(TransformerException exception) throws TransformerException {
        TransformerException unwrappedException = unwrapException(exception);
        if (triggerException(unwrappedException)) {
            LOGGER.error("Exception while XSL transformation:{}", unwrappedException::getMessageAndLocation);
        }
        throw unwrappedException;
    }

    /* (non-Javadoc)
     * @see javax.xml.transform.ErrorListener#fatalError(javax.xml.transform.TransformerException)
     */
    @Override
    public void fatalError(TransformerException exception) throws TransformerException {
        TransformerException unwrappedException = unwrapException(exception);
        StackTraceElement[] stackTrace = unwrappedException.getStackTrace();
        if (stackTrace.length > 0 && stackTrace[0].getMethodName().equals("execute")
            && stackTrace[0].getClassName().endsWith("ElemMessage")) {
            LOGGER.debug("Original exception: ", unwrappedException);
            unwrappedException = new TransformerException(lastMessage);
        }
        if (triggerException(unwrappedException)) {
            LOGGER.fatal("Exception while XSL transformation.", unwrappedException);
        }
        throw unwrappedException;
    }

    public static TransformerException unwrapException(TransformerException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof TransformerException te) {
                return unwrapException(te);
            }
            if (cause instanceof WrappedRuntimeException wrte) {
                cause = wrte.getException();
            } else {
                cause = cause.getCause();
            }
        }
        return exception;
    }

    public static String getMyMessageAndLocation(TransformerException exception) {
        SourceLocator locator = exception.getLocator();
        StringBuilder msg = new StringBuilder();
        if (locator != null) {
            String systemID = locator.getSystemId();
            int line = locator.getLineNumber();
            int col = locator.getColumnNumber();
            if (systemID != null) {
                msg.append("SystemID: ");
                msg.append(systemID);
            }
            if (line != 0) {
                msg.append(" [");
                msg.append(line);
                if (col != 0) {
                    msg.append(',');
                    msg.append(col);
                }
                msg.append(']');
            }
        }
        msg.append(": ");
        msg.append(exception.getMessage());
        return msg.toString();
    }
}
