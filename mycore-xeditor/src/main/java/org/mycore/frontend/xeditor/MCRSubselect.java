package org.mycore.frontend.xeditor;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;

public class MCRSubselect {

    public static final String PARAM_SUBSELECT_SESSION = "_xed_subselect_session";

    private final static Logger LOGGER = Logger.getLogger(MCRSubselect.class);

    private String xPath;

    private String href;

    private MCREditorSession session;

    public MCRSubselect(MCREditorSession session, String parameter) throws JaxenException, JDOMException {
        this.session = session;

        int pos = parameter.lastIndexOf(":");
        this.xPath = parameter.substring(0, pos);
        this.href = decode(parameter.substring(pos + 1));

        LOGGER.info("New subselect for " + xPath + " using pattern " + href);

        MCRBinding binding = new MCRBinding(xPath, session.getRootBinding());
        this.href = new MCRXPathEvaluator(binding).replaceXPaths(href, true);

        binding.detach();
    }

    public String getXPath() {
        return xPath;
    }

    public String getSessionParam() {
        return PARAM_SUBSELECT_SESSION + "=" + session.getCombinedSessionStepID();
    }

    public String getRedirectURL() {
        return href + (href.contains("?") ? "&" : "?") + getSessionParam();
    }

    public static String encode(String href) {
        try {
            return Hex.encodeHexString(href.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new MCRException(ex);
        }
    }

    public static String decode(String href) {
        try {
            return new String(Hex.decodeHex(href.toCharArray()));
        } catch (DecoderException ex) {
            throw new MCRException(ex);
        }
    }
}