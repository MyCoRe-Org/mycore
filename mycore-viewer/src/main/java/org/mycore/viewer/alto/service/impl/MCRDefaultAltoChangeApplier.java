package org.mycore.viewer.alto.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.viewer.alto.model.MCRAltoChangeSet;
import org.mycore.viewer.alto.model.MCRAltoWordChange;
import org.mycore.viewer.alto.service.MCRAltoChangeApplier;

public class MCRDefaultAltoChangeApplier implements MCRAltoChangeApplier {

    private static final Logger LOGGER = LogManager.getLogger();

    private Map<String, List<MCRAltoWordChange>> fileChangeMap = new ConcurrentHashMap<>();

    @Override
    public void applyChange(MCRAltoChangeSet changeSet) {
        String derivateID = changeSet.getDerivateID();

        changeSet.getWordChanges().stream().forEach(change -> {
            List<MCRAltoWordChange> list = fileChangeMap.computeIfAbsent(change.getFile(), (k) -> new ArrayList<>());
            list.add(change);
        });

        fileChangeMap.keySet().forEach(file -> {
            LOGGER.info("Open file {} to apply changes!", file);
            MCRPath altoFilePath = MCRPath.getPath(derivateID, file);

            if (!Files.exists(altoFilePath)) {
                LOGGER.warn("Could not find file {} which was referenced by alto change!", altoFilePath);
                throw new MCRException(new IOException("Alto-File " + altoFilePath.toString() + " does not exist"));
            }

            Document altoDocument = readALTO(altoFilePath);
            List<MCRAltoWordChange> wordChangesInThisFile = fileChangeMap.get(file);
            wordChangesInThisFile.stream().forEach(wordChange -> {
                String xpath = String
                    .format(Locale.ROOT, "//alto:String[number(@HPOS)=number('%d') and number(@VPOS)=number('%d')]",
                        wordChange.getHpos(), wordChange.getVpos());
                List<Element> wordToChange = XPathFactory.instance()
                    .compile(xpath, Filters.element(), null, MCRConstants.ALTO_NAMESPACE).evaluate(altoDocument);

                if (wordToChange.size() != 1) {
                    LOGGER.warn("Found {} words to change.", wordToChange.size());
                }
                wordToChange.forEach(word -> {
                    word.setAttribute("CONTENT", wordChange.getTo());
                    word.setAttribute("WC", "1");
                });
            });
            storeALTO(altoFilePath, altoDocument);
        });
    }

    private void storeALTO(MCRPath altoFilePath, Document altoDocument) {
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        try (OutputStream outputStream = Files
            .newOutputStream(altoFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            xmlOutputter.output(altoDocument, outputStream);
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    private Document readALTO(MCRPath altoFilePath) {
        try (InputStream inputStream = Files.newInputStream(altoFilePath, StandardOpenOption.READ)) {
            return new SAXBuilder().build(inputStream);
        } catch (JDOMException | IOException e) {
            throw new MCRException(e);
        }
    }

}
