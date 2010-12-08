/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.oai;

import static org.mycore.oai.MCROAIConstants.ARG_RESUMPTION_TOKEN;
import static org.mycore.oai.MCROAIConstants.ARG_VERB;
import static org.mycore.oai.MCROAIConstants.ERROR_BAD_ARGUMENT;
import static org.mycore.oai.MCROAIConstants.ERROR_ID_DOES_NOT_EXIST;
import static org.mycore.oai.MCROAIConstants.NS_OAI;
import static org.mycore.oai.MCROAIConstants.NS_XSI;
import static org.mycore.oai.MCROAIConstants.SCHEMA_LOC_OAI;
import static org.mycore.oai.MCROAIConstants.V_ALWAYS;
import static org.mycore.oai.MCROAIConstants.V_EXCLUSIVE;
import static org.mycore.oai.MCROAIConstants.V_REQUIRED;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRMetaISO8601Date;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQueryParser;

/**
 * Each verb handler implements one of the six OAI-PMH verbs. 
 * MCRVerbHandler provides common functionality for argument checking, building the 
 * response body, adding error codes and checking identifiers. 
 * 
 * @author Frank L\u00fctzenkirchen
 */
public abstract class MCRVerbHandler {
    protected final static Logger LOGGER = Logger.getLogger(MCRVerbHandler.class);

    /** The data provider instance */
    protected MCROAIDataProvider provider;

    /** The root element of the xml response to a request, as defined by section 3.2 of the OAI-PMH specification */
    protected Element response;

    /** The element that contains the output of the handled request, an element with the same name as the verb of the respective OAI-PMH request */
    protected Element output;

    /** Holds error elements as defined in section 3.6 of the OAI-PMH specifications */
    protected List<Element> errors = new ArrayList<Element>();

    protected Properties parms = new Properties();

    protected List<String> setURIs = new ArrayList<String>();

    protected MCRCondition restriction;

    MCRVerbHandler(MCROAIDataProvider provider) {
        this.provider = provider;

        MCRConfiguration config = MCRConfiguration.instance();

        Properties p = config.getProperties(provider.getPrefix() + "Sets.");
        for (Iterator it = p.values().iterator(); it.hasNext();)
            setURIs.add((String) (it.next()));

        String r = config.getString(provider.getPrefix() + "Search.Restriction", null);
        if (r != null) {
            try {
                restriction = new MCRQueryParser().parse(r);
            } catch (Exception ex) {
                String msg = "Unable to parse " + provider.getPrefix() + "Search.Restriction=" + r;
                throw new MCRConfigurationException(msg, ex);
            }
        }

        response = new Element("OAI-PMH", NS_OAI);
        response.setAttribute(new Attribute("schemaLocation", SCHEMA_LOC_OAI, NS_XSI));

        Element responseDate = new Element("responseDate", NS_OAI);
        response.addContent(responseDate);
        responseDate.setText(buildUTCDate(new Date(), MCRMetaISO8601Date.IsoFormat.COMPLETE_HH_MM_SS));

        Element request = new Element("request", NS_OAI);
        request.setText(provider.getOAIBaseURL());
        response.addContent(request);
    }

    Document handle(Map<String, String[]> parameters) {
        Properties allowedParameters = new Properties();
        allowedParameters.setProperty(ARG_VERB, V_ALWAYS);
        setAllowedParameters(allowedParameters);

        boolean exclusive = false;
        boolean other = false;

        // Phase 1: Check for allowed arguments
        for (Iterator<String> pi = parameters.keySet().iterator(); pi.hasNext();) {
            String parameterName = pi.next();

            // Argument not allowed for this verb?
            if (!allowedParameters.containsKey(parameterName)) {
                addError(ERROR_BAD_ARGUMENT, "Bad argument: " + parameterName);
            } else {
                // Argument repeated?
                String[] values = parameters.get(parameterName);
                if (values.length > 1) {
                    addError(ERROR_BAD_ARGUMENT, "Argument " + parameterName + " is repeated");
                } else if ((values[0] != null) && (values[0].trim().length() > 0)) {
                    if (allowedParameters.get(parameterName) == V_EXCLUSIVE)
                        exclusive = true;
                    else if (allowedParameters.get(parameterName) != V_ALWAYS)
                        other = true;

                    parms.put(parameterName, values[0].trim());
                }
            }
        }

        if (hasErrors())
            return getResponse();

        // Phase 2: Check exclusive argument
        if (exclusive && other) {
            addError(ERROR_BAD_ARGUMENT, ARG_RESUMPTION_TOKEN + " is exclusive argument");
            return getResponse();
        }

        // Phase 3: Check for missing required arguments
        if (!exclusive) {
            for (Iterator ai = allowedParameters.keySet().iterator(); ai.hasNext();) {
                String argument = (String) (ai.next());
                String type = allowedParameters.getProperty(argument);
                String value = parms.getProperty(argument);

                if ((type == V_REQUIRED) && (value == null)) {
                    addError(ERROR_BAD_ARGUMENT, "Missing required argument: " + argument);
                    return getResponse();
                }
            }
        }

        // Add correct arguments to response
        Element request = response.getChild("request", NS_OAI);
        for (Iterator pi = parms.keySet().iterator(); pi.hasNext();) {
            String argument = (String) (pi.next());
            String value = parms.getProperty(argument);

            if (value != null)
                request.setAttribute(argument, value);
        }

        if (!hasErrors()) {
            output = new Element(parms.getProperty(ARG_VERB), NS_OAI);
            handleRequest();
        }

        return getResponse();
    }

    /**
     * Returns true if any error conditions were detected so far
     */
    boolean hasErrors() {
        return errors.size() > 0;
    }

    /**
     * Returns the response to the OAI-PMH request, as defined by section 3.2 of the OAI-PMH specification.
     */
    protected Document getResponse() {
        if (hasErrors())
            response.addContent(errors);
        else
            response.addContent(output);

        Document doc = new Document(response);

        // Add link to XSL stylesheet for displaying OAI response in web browser
        String xsl = MCRConfiguration.instance().getString(provider.getPrefix() + "ResponseStylesheet", "oai/oai2.xsl");
        if (!xsl.isEmpty()) {
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("type", "text/xsl");
            pairs.put("href", MCRServlet.getBaseURL() + xsl);
            doc.addContent(0, new ProcessingInstruction("xml-stylesheet", pairs));
        }

        return doc;
    }

    /**
     * Adds an error to the response.
     * 
     * @param code the error code, see {@link MCROAIConstants} and section 3.6 of the OAI-PMH specification.
     * @param message the optional error message providing more detailed information on the error cause. Can be null.
     */
    void addError(String code, String message) {
        LOGGER.error(code + (message == null ? "" : ": " + message));

        Element error = new Element("error", NS_OAI);
        error.setAttribute("code", code);
        if (message != null)
            error.setText(message);
        errors.add(error);
    }

    /**
     * Checks a record identifier for correct syntax and existence. Identifiers must start with "oai:" followed by the
     * repository identifier, followed by a unique item identifier, for example "oai:duepublico.uni-due.de:4711".
     * This conforms to section 2.4 of the OAI-PMH specification and the oai-identifier syntax described in the accompanying
     * OAI implementation guidelines. When the identifier is not valid, this method will add an error to the response. 
     * 
     * @param identifier the identifier to check.
     * @return true, if the record identifier is syntactically correct and an item with that identifier exists.
     */
    boolean checkIdentifier(String identifier) {
        String prefix = "oai:" + provider.getRepositoryIdentifier() + ":";
        if (!identifier.startsWith(prefix)) {
            addError(ERROR_ID_DOES_NOT_EXIST, "Identifier must start with " + prefix);
            return false;
        }

        identifier = identifier.substring(prefix.length());

        boolean exists = provider.getAdapter().exists(identifier);
        if (!exists)
            addError(ERROR_ID_DOES_NOT_EXIST, "Record ID does not exists: " + identifier);

        return exists;
    }

    static String buildUTCDate(Date date, MCRMetaISO8601Date.IsoFormat format) {
        MCRMetaISO8601Date iso = new MCRMetaISO8601Date();
        iso.setDate(date);
        iso.setFormat(format);
        return iso.getISOString();
    }

    MCROAIDataProvider getProvider() {
        return provider;
    }

    void setAllowedParameters(Properties p) {
    }

    void handleRequest() {
    }
}
