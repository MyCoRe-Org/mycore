package org.mycore.mods.classification;

import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

/**
 * Maps classifications in Mods-Documents.
 * <p>You can define a label <b><code>x-mapping</code></b> in a classification with space seperated categoryIds to which the classification will be mapped.</p>
 * <code>
 * &lt;category ID=&quot;article&quot; counter=&quot;1&quot;&gt;<br>
 * &nbsp;&lt;label xml:lang=&quot;en&quot; text=&quot;Article / Chapter&quot; /&gt;<br>
 * &nbsp;&lt;label xml:lang=&quot;de&quot; text=&quot;Artikel / Aufsatz&quot; /&gt;<br>
 * &nbsp;&lt;label xml:lang=&quot;x-mapping&quot; text=&quot;diniPublType:article&quot; /&gt;<br>
 * &lt;/category&gt;
 * </code>
 *
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRClassificationMappingEventHandler extends MCREventHandlerBase {

    public static final String GENERATOR_SUFFIX = "-mycore";

    private static final Logger LOGGER = Logger.getLogger(MCRClassificationMappingEventHandler.class);

    private static final MCRCategoryDAO DAO = MCRCategoryDAOFactory.getInstance();

    private static Map<MCRCategoryID, MCRCategoryID> getMappings(MCRCategory category) {
        Optional<MCRLabel> labelOptional = category.getLabel("x-mapping");
        Hashtable<MCRCategoryID, MCRCategoryID> mappingTable = new Hashtable<>();

        if (labelOptional.isPresent()) {
            String label = labelOptional.get().getText();
            Stream.of(label.split("\\s"))
                .map(categIdString -> categIdString.split(":"))
                .map(categIdArr -> new MCRCategoryID(categIdArr[0], categIdArr[1]))
                .filter(DAO::exist)
                .forEach(mappingTarget -> {
                    mappingTable.put(category.getId(), mappingTarget);
                });
        }

        return mappingTable;
    }

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        createMapping(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        createMapping(obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        createMapping(obj);
    }

    private static String getGenerator(MCRCategoryID src, MCRCategoryID target) {
        return String.format(Locale.ROOT, "%s2%s%s", src.getRootID(), target.getRootID(), GENERATOR_SUFFIX);
    }

    private void createMapping(MCRObject obj) {
        MCRMODSWrapper mcrmodsWrapper = new MCRMODSWrapper(obj);
        if(mcrmodsWrapper.getMODS()==null){
            return;
        }
        // vorher alle mit generator *-mycore löschen
        mcrmodsWrapper.getElements("//classification[contains(@generator, '" + GENERATOR_SUFFIX + "')]")
                .stream().forEach(Element::detach);

        LOGGER.info("check mappings " + obj.getId().toString());
        mcrmodsWrapper.getMcrCategoryIDs().stream()
                .map(categoryId -> DAO.getCategory(categoryId, 0))
                .filter(Objects::nonNull)
                .map(MCRClassificationMappingEventHandler::getMappings)
                .flatMap(map -> map.entrySet().stream())
                .distinct()
                .forEach(mapping -> {
                    String taskMessage = String.format(Locale.ROOT, "add mapping from '%s' to '%s'", mapping.getKey().toString(), mapping.getValue().toString());
                    LOGGER.info(taskMessage);
                    Optional<Element> createdClassificationElement = mcrmodsWrapper.setElement("classification", "generator", getGenerator(mapping.getKey(), mapping.getValue()), null);
                    Element element = createdClassificationElement.orElseThrow(() -> new MCRException("Could not: " + taskMessage));
                    MCRClassMapper.assignCategory(element, mapping.getValue());
                });
        LOGGER.debug("mapping complete.");
    }

}
