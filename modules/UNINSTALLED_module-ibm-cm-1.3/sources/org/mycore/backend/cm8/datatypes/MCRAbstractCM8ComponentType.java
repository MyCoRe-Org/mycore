package org.mycore.backend.cm8.datatypes;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRMetaDefault;

import com.ibm.mm.sdk.common.DKAttrDefICM;
import com.ibm.mm.sdk.common.DKComponentTypeDefICM;
import com.ibm.mm.sdk.common.DKConstantICM;
import com.ibm.mm.sdk.common.DKDatastoreDefICM;
import com.ibm.mm.sdk.common.DKException;

abstract class MCRAbstractCM8ComponentType implements MCRCM8ComponentType, DKConstantICM {

    final static int VARCHAR_MAX_LENGTH = 32672;

    final static int LOB_MAX_LENGTH = 5 * 1024 * 1024;

    final static int LENGTH_RADIX = (int) Math.ceil(log2(LOB_MAX_LENGTH));

    private DKDatastoreDefICM dsDefICM;

    private String itemPrefix;

    private static final Logger LOGGER = Logger.getLogger(MCRAbstractCM8ComponentType.class);

    /**
     * This method create a DKComponentTypeDefICM to create a complete ItemType
     * from the configuration.
     * 
     * @param element
     *            a MCR datamodel element as JDOM Element
     * @param textserach
     *            the flag to use textsearch as string (this value has no effect
     *            for this class)
     * @return a DKComponentTypeDefICM for the MCR datamodel element
     * @throws Exception
     * @throws DKException
     * @exception MCRPersistenceException
     *                general Exception of MyCoRe
     */
    protected DKComponentTypeDefICM getBaseItemTypeDef(final org.jdom.Element element) throws DKException, Exception {
        final String attrName = getAttrName(element, false);
        final short min = Short.parseShort(element.getParentElement().getAttributeValue("minOccurs"));
        final String maxAttr = element.getAttributeValue("maxOccurs");
        final short max = maxAttr.equals("unbounded") ? (short) VARCHAR_MAX_LENGTH : Short.parseShort(maxAttr);

        final DKComponentTypeDefICM compType = new DKComponentTypeDefICM(getDsDefICM().getDatastore());

        // create component child
        compType.setName(attrName);
        compType.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
        compType.setCardinalityMin(min);
        compType.setCardinalityMax(max);

        // add lang attribute
        DKAttrDefICM attr = (DKAttrDefICM) dsDefICM.retrieveAttr("mcrLang");
        attr.setNullable(true);
        attr.setUnique(false);
        attr.setDefaultValue(MCRConfiguration.instance().getString("MCR.metadata_default_lang", "de"));
        attr.setDescription("Language of attribute value");
        compType.addAttr(attr);

        // add type attribute
        attr.setNullable(true);
        attr.setUnique(false);
        attr = (DKAttrDefICM) dsDefICM.retrieveAttr("mcrType");
        attr.setDescription("Optional type of attribute value to destinct it from other values");
        compType.addAttr(attr);

        return compType;
    }

    /**
     * maps element <code>name</code> attribute to CM attribute name.
     * 
     * First the implemented <code>getDataTypeChar()</code> Method is used to
     * get the first character which should be unique across MCRMetadataTypes.
     * The second character is the <code>length</code> attribute coded in one
     * character. Currently this is log2(length) as HEX value;
     * 
     * e.g. <code>MCRMetaLangText</code> maps to 't', <code>length</code>=1000
     * and <code>name</code> is "TitleWithHtmlTags" would result in:
     * "taTitleWithHtml"
     * 
     * @param element
     *            metadata element from datamodel definition
     * @return String of CM attribute name that is never longer than 15
     *         characters
     */
    protected String getAttrName(final org.jdom.Element element, final boolean useLengthFallback) {
        final String attrName = element.getAttributeValue("name");
        return getItemPrefix() + cutString(attrName, 13);
    }

    /**
     * @param str
     * @return
     */
    protected static String cutString(final String str, final int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, 13);
    }

    /**
     * @param element
     * @param useLengthFallback
     * @return
     */
    protected int getAttrSize(final org.jdom.Element element, final boolean useLengthFallback) {
        int length = 0;
        final String lengthAttr = element.getAttributeValue("length");
        if (lengthAttr == null) {
            if (useLengthFallback) {
                length = MCRMetaDefault.DEFAULT_STRING_LENGTH;
            }
        } else {
            length = Integer.parseInt(lengthAttr);
        }
        return length;
    }

    protected String getAttrDescription(final Element element) {
        return element.getAttributeValue("name");
    }

    protected abstract char getDataTypeChar();

    protected static char getCodedLength(final int length) {
        if (length < 1) {
            return '_';
        }
        if (length > VARCHAR_MAX_LENGTH) {
            return 'X';
        }
        return Integer.toString((int) log2(length), LENGTH_RADIX).charAt(0);
    }

    protected static int getNormalizedSize(final int size) {
        return (int) Math.pow(2, Math.ceil(log2(size)));
    }

    private static double log2(final double exp) {
        return Math.log(exp) / Math.log(2);
    }

    /**
     * @param dsDefICM
     *            the dsDefICM to set
     */
    public void setDsDefICM(final DKDatastoreDefICM dsDefICM) {
        this.dsDefICM = dsDefICM;
    }

    /**
     * @return the dsDefICM
     */
    protected DKDatastoreDefICM getDsDefICM() {
        return dsDefICM;
    }

    /**
     * @param name
     * @param size
     * @return
     * @throws Exception
     * @throws DKException
     */
    protected DKAttrDefICM getVarCharAttr(final String name, final String description, final int size,
            final boolean nullable) throws Exception, DKException {
        final String attrName = cutString(name, 14) + Character.toString(getCodedLength(size));
        int attrSize = getNormalizedSize(size);
        if (attrSize > VARCHAR_MAX_LENGTH) {
            attrSize = VARCHAR_MAX_LENGTH;
        }
        MCRCM8AttributeUtils.createAttributeVarChar(getDsDefICM(), attrName, attrSize, true);
        final DKAttrDefICM attr = (DKAttrDefICM) getDsDefICM().retrieveAttr(attrName);
        attr.setNullable(nullable);
        attr.setDescription(description);
        if (attr.isTextSearchable()) {
            LOGGER.debug("Setting textsearch properties for attr: " + attr.getName());
            attr.setTextIndexDef(MCRCM8AttributeUtils.getTextDefinition());
        }
        attr.setUnique(false);
        return attr;
    }

    protected DKAttrDefICM getVarCharAttr(final String name, final String description, final int size)
            throws Exception, DKException {
        return getVarCharAttr(name, description, size, true);
    }

    /**
     * @param name
     * @param description
     * @param size
     * @return
     * @throws Exception
     * @throws DKException
     */
    protected DKAttrDefICM getNoTSVarCharAttr(final String name, final String description, final int size)
            throws Exception, DKException {
        final DKAttrDefICM attr = getVarCharAttr(name, description, size);
        attr.setTextSearchable(false);
        return attr;
    }

    /**
     * @return the itemPrefix
     */
    protected String getItemPrefix() {
        return itemPrefix;
    }

    /**
     * @param itemPrefix
     *            the itemPrefix to set
     */
    public void setComponentNamePrefix(final String itemPrefix) {
        this.itemPrefix = itemPrefix;
    }

}