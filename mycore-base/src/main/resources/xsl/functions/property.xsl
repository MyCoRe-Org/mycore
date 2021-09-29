<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcrproperty="http://www.mycore.de/xslt/property"
                exclude-result-prefixes="fn xs">

  <xsl:function name="mcrproperty:all" as="element()">
    <xsl:param name="keyPrefix" as="xs:string"/>
    <xsl:variable name="propertiesDoc" />
    <xsl:sequence select="fn:document(concat('property:', $keyPrefix, '*'))/properties" />
  </xsl:function>

  <xsl:function name="mcrproperty:one" as="xs:string">
    <xsl:param name="key" as="xs:string"/>
    <xsl:variable name="entryDoc" select="fn:document(concat('property:', $key))"/>
    <xsl:value-of select="$entryDoc/entry/text()"/>
  </xsl:function>

</xsl:stylesheet>
