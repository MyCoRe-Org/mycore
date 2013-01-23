package org.mycore.importer.mapping.mapper;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.jdom2.Element;
import org.mycore.importer.MCRImportField;
import org.mycore.importer.MCRImportRecord;
import org.mycore.importer.mapping.MCRImportMetadata;
import org.mycore.importer.mapping.MCRImportObject;
import org.mycore.importer.mapping.resolver.metadata.MCRImportAbstractMetadataResolver;
import org.mycore.importer.mapping.resolver.metadata.MCRImportMetadataResolver;

/**
 * The multidata mapper is capable of mapping <code>MCRImportFields</code> in a
 * <code>MCRImportRecord</code> which have the same id. For example:
 * <table cellspacing="3" cellpadding="3" border="1" rules="groups">
 *   <thead>
 *    <tr align="left">
 *      <th width="40px">Id</th>
 *      <th width="150px">Value</th>
 *    </tr>
 *  </thead>
 *  <tr align="left">
 *    <td>text</td>
 *    <td>Sample text</td>
 *  </tr>
 *  <tr align="left">
 *    <td>text</td>
 *    <td>another sample text</td>
 *  </tr>
 * </table>
 * Is mapped to:
 * <p>
 * &lt;def.metaText class="MCRMetaLangText"&gt;</br>
 *   &nbsp;&nbsp;&lt;metaText xml:lang="de" form="plain">Sample text&lt;/metaText&gt;</br>
 *   &nbsp;&nbsp;&lt;metaText xml:lang="de" form="plain">another sample text&lt;/metaText&gt;</br>
 * &lt;/def.metaText&gt;
 * </p>
 * <p>
 * Its important that the field definition only uses one field!</br>
 * &lt;map fields="text" to="metaText" type="multidata"&gt;</br>
 * &nbsp;&nbsp;...</br>
 * &lt;/map&gt;
 * </p>
 * @author Matthias Eichner
 */
public class MCRImportMultiDataMapper extends MCRImportAbstractMapper {

    @Override
    public void map(MCRImportObject importObject, MCRImportRecord record, Element map) {
        super.map(importObject, record, map);
        MCRImportMetadataResolver metadataResolver = createResolverInstance();
        for(MCRImportField field : fields) {
            Element metadataChild = new Element(map.getAttributeValue("to"));
            List<MCRImportField> internalFieldList = new ArrayList<MCRImportField>();
            internalFieldList.add(field);
            if(metadataResolver.resolve(map, internalFieldList, metadataChild)) {
                MCRImportMetadata metadataObject = importObject.addMetadataChild(metadataChild);
                if(metadataResolver instanceof MCRImportAbstractMetadataResolver) {
                    Hashtable<String, String> enclosingAttributes = ((MCRImportAbstractMetadataResolver)metadataResolver).getEnclosingAttributes();
                    metadataObject.addAttributeMap(enclosingAttributes);
                }
            }
        }
    }

    public String getType() {
        return "multidata";
    }
}