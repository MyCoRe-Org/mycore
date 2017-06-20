package org.mycore.mets.model.converter;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mycore.mets.model.simple.MCRMetsFile;
import org.mycore.mets.model.simple.MCRMetsFileUse;
import org.mycore.mets.model.simple.MCRMetsLink;
import org.mycore.mets.model.simple.MCRMetsPage;
import org.mycore.mets.model.simple.MCRMetsSection;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

public class MCRMetsTestUtil {

    private static Properties PROPERTIES = new Properties();

    public static String readJsonFile(String path) throws IOException {
        InputStream resourceAsStream = MCRMetsTestUtil.class.getClassLoader().getResourceAsStream("json/" + path);
        List<String> stringList = IOUtils.readLines(resourceAsStream, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        stringList.forEach(sb::append);
        return sb.toString();

    }

    public static Document readXMLFile(String path) throws JDOMException, IOException {
        InputStream resourceAsStream = MCRMetsTestUtil.class.getClassLoader().getResourceAsStream("xml/" + path);
        SAXBuilder builder = new SAXBuilder();
        return builder.build(resourceAsStream);
    }

    public static Properties getProperties() {
        if (PROPERTIES.isEmpty()) {
            try {
                PROPERTIES.load(MCRMetsTestUtil.class.getClassLoader().getResourceAsStream("mets.properties"));
            } catch (IOException e) {
                throw new RuntimeException("could not load application properties!", e);
            }
        }

        return PROPERTIES;
    }

    public static <T> T instantiate(final String className, final Class<T> type) {
        try {
            return type.cast(Class.forName(className).newInstance());
        } catch (final InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> boolean comparer(Comparator<T> comparator, T object, T... withList) {
        return Arrays.stream(withList).noneMatch(with -> comparator.compare(object, with) != 0);
    }

    public static <T, R> List<R> bulk(BulkOperation<T, R> op, T... on) {
        return Arrays.asList(on).stream().map(op::doOperation).collect(toList());
    }

    public interface BulkOperation<T, R> {
        public R doOperation(T input);
    }

    public static MCRMetsSimpleModel buildMetsSimpleModel() {
        MCRMetsSimpleModel metsSimpleModel = new MCRMetsSimpleModel();

        builSimpleModelSections(metsSimpleModel);
        buildSimpleModelPages(metsSimpleModel);
        buildSimpleModelLinkList(metsSimpleModel);
        return metsSimpleModel;
    }

    private static void buildSimpleModelLinkList(MCRMetsSimpleModel metsSimpleModel) {
        List<MCRMetsLink> linkList = metsSimpleModel.sectionPageLinkList;
        MCRMetsSection rootSection = metsSimpleModel.getRootSection();
        List<MCRMetsPage> pageList = metsSimpleModel.getMetsPageList();
        linkList.add(new MCRMetsLink(rootSection, pageList.get(0)));
        linkList.add(new MCRMetsLink(rootSection.getMetsSectionList().get(0), pageList.get(1)));
        linkList.add(new MCRMetsLink(rootSection.getMetsSectionList().get(1), pageList.get(2)));
    }

    private static void buildSimpleModelPages(MCRMetsSimpleModel metsSimpleModel) {
        MCRMetsPage metsPage1 = new MCRMetsPage("1", "URN:special-urn1");
        metsPage1.getFileList().add(new MCRMetsFile("1.jpg", "image/jpeg", MCRMetsFileUse.MASTER));
        metsPage1.getFileList().add(new MCRMetsFile("1.xml", "text/xml", MCRMetsFileUse.ALTO));

        MCRMetsPage metsPage2 = new MCRMetsPage("2", "URN:special-urn2");
        metsPage2.getFileList().add(new MCRMetsFile("2.jpg", "image/jpeg", MCRMetsFileUse.MASTER));
        metsPage2.getFileList().add(new MCRMetsFile("2.xml", "text/xml", MCRMetsFileUse.ALTO));

        MCRMetsPage metsPage3 = new MCRMetsPage("3", "URN:special-urn3");
        metsPage3.getFileList().add(new MCRMetsFile("3.jpg", "image/jpeg", MCRMetsFileUse.MASTER));
        metsPage3.getFileList().add(new MCRMetsFile("3.xml", "text/xml", MCRMetsFileUse.ALTO));

        metsSimpleModel.getMetsPageList().add(metsPage1);
        metsSimpleModel.getMetsPageList().add(metsPage2);
        metsSimpleModel.getMetsPageList().add(metsPage3);
    }

    private static void builSimpleModelSections(MCRMetsSimpleModel metsSimpleModel) {
        MCRMetsSection rootSection = new MCRMetsSection("testRootType", "testRootLabel", null);
        metsSimpleModel.setRootSection(rootSection);

        MCRMetsSection subSection1 = new MCRMetsSection("subSection", "subSection1Label", rootSection);
        MCRMetsSection subSection2 = new MCRMetsSection("subSection", "subSection2Label", rootSection);

        rootSection.addSection(subSection1);
        rootSection.addSection(subSection2);
    }
}
