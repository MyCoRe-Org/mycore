<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mcrlanguage="http://www.mycore.de/xslt/language"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:function name="mcrlanguage:detect-language" as="xs:string?">
    <xsl:param name="text" as="xs:string" />

    <xsl:variable name="result" select="document('detectLanguage:full:' || fn:encode-for-uri($text))/string" />
    <xsl:sequence select="if ($result != '') then $result else ()" />
  </xsl:function>

  <xsl:function name="mcrlanguage:detect-language-by-character" as="xs:string?">
    <xsl:param name="text" as="xs:string" />

    <xsl:variable name="result" select="document('detectLanguage:character:' || fn:encode-for-uri($text))/string" />
    <xsl:sequence select="if ($result != '') then $result else ()" />
  </xsl:function>

</xsl:stylesheet>
