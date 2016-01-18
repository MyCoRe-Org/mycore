package org.mycore.mods.classification;


import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
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

    private static List<MCRCategoryID> getMappings(String label) {
        return Stream.of(label.split("\\s"))
                .map(categIdString -> categIdString.split(":"))
                .map(categIdArr -> new MCRCategoryID(categIdArr[0], categIdArr[1]))
                .collect(Collectors.toList());
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

    private void createMapping(MCRObject obj) {
        // vorher alle mit generator *-mycore löschen
        MCRMODSWrapper mcrmodsWrapper = new MCRMODSWrapper(obj);
        mcrmodsWrapper.getElements("//classification[contains(@generator, '" + GENERATOR_SUFFIX + "')]")
                .stream().forEach(Element::detach);

        LOGGER.info("check mappings " + obj.getId().toString());
        mcrmodsWrapper.getMcrCategoryIDs().stream()
                .map(categoryId -> DAO.getCategory(categoryId, 0))
                .filter(Objects::nonNull)
                .map(category -> category.getLabel("x-mapping"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(MCRLabel::getText)
                .map(MCRClassificationMappingEventHandler::getMappings)
                .flatMap(List::stream)
                .distinct()
                .forEach(categoryId -> {
                    LOGGER.info("add Mapping to " + categoryId.toString());
                    Optional<Element> createdClassificationElement = mcrmodsWrapper.setElement("classification", "generator", getGenerator(), null);
                    Element element = createdClassificationElement.orElseThrow(() -> new MCRException("Could not add mapping to classification " + categoryId.toString()));
                    MCRClassMapper.assignCategory(element, categoryId);
                });

        LOGGER.info("mapping complete.");
    }

    protected static String getGenerator() {
        return MCRClassificationMappingEventHandler.class.getSimpleName() + GENERATOR_SUFFIX;
    }

}
