package org.mycore.test.neo4j.parser;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.language.MCRLanguageFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JParser;

import java.io.IOException;

public class MCRNeo4JParserTest extends MCRStoreTestCase {
   @BeforeClass
   public static void beforeClass() {
      System.setProperty("log4j.configurationFile", "log4j2-test.xml");
   }

   @Test
   public void testParseMCRObject() throws Exception {
      MCRLanguageFactory.instance().getLanguage("xx");
      MCRConfiguration2.set("MCR.Metadata.Type.work", "true");
      MCRConfiguration2.set("MCR.Metadata.Type.manuscript", "true");

      MCRConfiguration2.set("MCR.Neo4J.NodeAttribute.manuscript.descriptor",
            "/mycoreobject/metadata/def.mss82");
      MCRConfiguration2.set("MCR.Neo4J.NodeAttribute.manuscript.signature",
            "/mycoreobject/metadata/def.mss02");
      MCRConfiguration2.set("MCR.Neo4J.ParserClass.MCRMetaLangText",
          "org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JMetaLangTextParser");
      MCRConfiguration2.set("MCR.Neo4J.ParserClass.MCRMetaClassification",
          "org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JMetaClassificationParser");
      MCRConfiguration2.set("MCR.Neo4J.ParserClass.MCRMetaHistoryDate",
          "org.mycore.datamodel.metadata.neo4jparser.MCRNeo4JMetaHistoryDateParser");
      
      final MCRNeo4JParser parser = new MCRNeo4JParser();
      final MCRObject manuscript = new MCRObject(read("/mcrobjects/a_mcrobject_00000001.xml"));
      final String result = parser.createNeo4JQuery(manuscript);
      System.out.println(result);
   }

   protected Document read(String file) {
      try {
         return (new SAXBuilder()).build(this.getClass().getResourceAsStream(file));
      } catch (IOException | JDOMException var3) {
         throw new RuntimeException(var3);
      }
   }
}
