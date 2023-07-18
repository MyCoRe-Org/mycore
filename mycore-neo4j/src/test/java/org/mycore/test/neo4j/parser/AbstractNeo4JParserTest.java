package org.mycore.test.neo4j.parser;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.utils.MCRXMLTransformer;

import java.io.IOException;
import java.util.Map;

public abstract class AbstractNeo4JParserTest extends MCRStoreTestCase {
    protected final Document doc;
    protected final Element metadata;

    public AbstractNeo4JParserTest() {
        try {
            doc = new SAXBuilder().build(getClass().getResourceAsStream("/mcrobjects/a_mcrobject_00000001.xml"));
        } catch (JDOMException | IOException e) {
            throw new RuntimeException(e);
        }
        metadata = doc.getRootElement().getChild("metadata");
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> properties = super.getTestProperties();
        properties.put("MCR.Metadata.Type.mcrobject", "true");

        return properties;
    }

    protected void addClassification(String file) throws Exception {
        Document classification = (new SAXBuilder()).build(this.getClass().getResourceAsStream(file));
        MCRCategory category = MCRXMLTransformer.getCategory(classification);
        MCRCategoryDAOFactory.getInstance().addCategory((MCRCategoryID)null, category);
    }
}
