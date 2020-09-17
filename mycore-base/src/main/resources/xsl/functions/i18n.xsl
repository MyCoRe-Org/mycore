<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
                exclude-result-prefixes="fn xs">

    <xsl:function name="mcri18n:translate" as="xs:string">
        <xsl:param name="key" as="xs:string"/>
        <xsl:variable name="translation"  select="fn:document(concat('i18n:', $key))" />
        <xsl:value-of select="$translation/i18n/text()" />
    </xsl:function>

</xsl:stylesheet>
