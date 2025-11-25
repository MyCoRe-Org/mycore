package org.mycore.solr.common.xml;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.common.luke.FieldFlag;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.solr.MCRSolrCore;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

/**
 *
 * Retrieves the solr field information for a given solr core, using {@link LukeRequest}
 * <pre>
 * Usage:
 * solrfields:{core}
 *
 * Example:
 * solrfields:main
 *
 * result:
 * </pre>
 * <code>
 * &lt;solrFields core="main"&gt;
 *   &lt;fields&gt;
 *     &lt;field name="id" type="string" schema="required:true" docs="1"&gt;
 *       &lt;flags&gt;
 *         &lt;flag&gt;INDEXED&lt;/flag&gt;
 *         &lt;flag&gt;STORED&lt;/flag&gt;
 *       &lt;/flags&gt;
 *     &lt;/field&gt;
 *     ...
 *   &lt;/fields&gt;
 *   &lt;dynamicFields&gt;
 *     &lt;field name="*_i" type="plint" schema="multiValued:false"&gt;
 *       &lt;flags&gt;
 *         &lt;flag&gt;INDEXED&lt;/flag&gt;
 *         &lt;flag&gt;STORED&lt;/flag&gt;
 *       &lt;/flags&gt;
 *     &lt;/field&gt;
 *     ...
 *   &lt;/dynamicFields&gt;
 * &lt;/solrFields&gt;
 * </code>
 */
public class MCRSolrFieldsResolver implements URIResolver {

    public static final String SOLRFIELDS_PREFIX = "solrfields:";

    private String extractSolrCore(String href) {
        return href.substring(SOLRFIELDS_PREFIX.length());
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        if (!href.startsWith(SOLRFIELDS_PREFIX)) {
            throw new TransformerException("MCRSolrFieldsResolver requires " + SOLRFIELDS_PREFIX + "url");
        }

        String solrCore = extractSolrCore(href);

        Optional<MCRSolrCore> mayCore = MCRSolrCoreManager.get(solrCore);

        if (mayCore.isEmpty()) {
            throw new TransformerException("MCRSolrCore not found: " + solrCore);
        }

        MCRSolrCore core = mayCore.get();

        SolrClient client = core.getClient();

        LukeRequest lukeRequest = new LukeRequest();
        MCRSolrAuthenticationManager.obtainInstance().applyAuthentication(lukeRequest,
            MCRSolrAuthenticationLevel.ADMIN);

        lukeRequest.setShowSchema(true);

        try {
            LukeResponse response = lukeRequest.process(client);

            Element root = new Element("solrFields");
            root.setAttribute("core", solrCore);
            Document doc = new Document(root);

            Element fields = new Element("fields");
            root.addContent(fields);

            Optional.ofNullable(response.getFieldInfo())
                .stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .map(this::toElement)
                .forEach(fields::addContent);

            Element dynamicFields = new Element("dynamicFields");
            root.addContent(dynamicFields);

            Optional.ofNullable(response.getDynamicFieldInfo())
                .stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .map(this::toElement)
                .forEach(dynamicFields::addContent);

            return new JDOMSource(doc);
        } catch (SolrServerException | IOException e) {
            throw new TransformerException("Error while retrieving fields from Solr core " + solrCore, e);
        }
    }

    private Element toElement(LukeResponse.FieldInfo fieldInfo) {
        Element field = new Element("field");
        field.setAttribute("name", fieldInfo.getName());
        if (fieldInfo.getType() != null) {
            field.setAttribute("type", fieldInfo.getType());
        }
        if (fieldInfo.getSchema() != null) {
            field.setAttribute("schema", fieldInfo.getSchema());
        }
        field.setAttribute("docs", String.valueOf(fieldInfo.getDocs()));
        field.setAttribute("distinct", String.valueOf(fieldInfo.getDistinct()));

        if (fieldInfo.getFlags() != null) {
            Element flags = new Element("flags");
            field.addContent(flags);
            for (FieldFlag flag : fieldInfo.getFlags()) {
                flags.addContent(new Element("flag").setText(flag.toString()));
            }
        }
        return field;
    }
}
