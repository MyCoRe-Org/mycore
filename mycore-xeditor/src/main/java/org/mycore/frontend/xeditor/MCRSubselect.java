package org.mycore.frontend.xeditor;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.frontend.xeditor.tracker.MCRSubselectStart;

public class MCRSubselect {

    private static final String PARAM_SUBSELECT_SESSION = "xed_subselect_session=";

    private final static Logger LOGGER = Logger.getLogger(MCRSubselect.class);

    private String xPath;

    private String href;

    private MCREditorSession session;

    public MCRSubselect(MCREditorSession session, String parameter) throws JaxenException, JDOMException {
        this.session = session;

        int pos = parameter.lastIndexOf(":");
        this.xPath = parameter.substring(0, pos);
        this.href = decode(parameter.substring(pos + 1));

        LOGGER.info("Start subselect for " + xPath + " using pattern " + href);

        MCRBinding binding = new MCRBinding(xPath, session.getRootBinding());
        Object node = binding.getBoundNode();
        Element element = node instanceof Element ? (Element) node : ((Attribute) node).getParent();
        binding.track(MCRSubselectStart.startSubselect(element, xPath));

        this.href = binding.replaceXPaths(href);

        binding.detach();
    }

    public String getRedirectURL() {
        String param = PARAM_SUBSELECT_SESSION + session.getCombinedSessionStepID();
        return href + (href.contains("?") ? "&" : "?") + param;
    }

    public static String encode(String href) {
        try {
            return Hex.encodeHexString(href.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String decode(String href) {
        try {
            return new String(Hex.decodeHex(href.toCharArray()));
        } catch (DecoderException ex) {
            throw new RuntimeException(ex);
        }
    }
}