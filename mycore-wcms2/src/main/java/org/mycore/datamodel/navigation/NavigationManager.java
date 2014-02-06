package org.mycore.datamodel.navigation;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;


public class NavigationManager {
    
    private static final Logger LOGGER = Logger.getLogger(NavigationManager.class);

    public Navigation load(Document doc) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Navigation.class);
        Unmarshaller m = jc.createUnmarshaller();
        Object navigation = m.unmarshal(doc);
        if(navigation instanceof Navigation) {
            return (Navigation)navigation;
        }
        return null;
    }
    
    public Navigation load(File navigationFile) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Navigation.class);
        Unmarshaller m = jc.createUnmarshaller();
        Object navigation = m.unmarshal(navigationFile);
        if(navigation instanceof Navigation) {
            return (Navigation)navigation;
        }
        return null;
    }
    
    /**
     * Save navigation.xml with JAXB.
     * If MCR.navigationFile.SaveInOldFormat is true,
     * the navigation.xml is reopen again, changed to the old Layout and saved again.
     * To be compatible with old navigation.xml layout.
     * 
     */
    public void save(Navigation navigation, File saveTo) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Navigation.class);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(navigation, saveTo);
               
        if(MCRConfiguration.instance().getBoolean("MCR.navigationFile.SaveInOldFormat")){
            try{
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                docFactory.setNamespaceAware(true);
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(saveTo);
                Node node = doc.getFirstChild();
                ((Element) node).setAttribute("href", node.getNodeName());
                NodeList nodeList = node.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++){
                    if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE){
                        String name = ((Element) nodeList.item(i)).getAttribute("id");
                        doc.renameNode(nodeList.item(i), null, name);
                        ((Element) nodeList.item(i)).removeAttribute("id");
                        ((Element) nodeList.item(i)).setAttribute("href", name);
                    }
                }
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(saveTo);
                transformer.transform(source, result);  
            } catch (Exception exc) {
                LOGGER.error(exc);
            }
        }
    }

}
