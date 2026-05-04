<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrmedia="http://www.mycore.de/xslt/media"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:variable name="mediaSourcesURIPrefix" as="xs:string" select="'mediasources:'" />

  <xsl:function name="mcrmedia:get-sources" as="element(source)*">
    <xsl:param name="derivateId" as="xs:string" />
    <xsl:param name="path" as="xs:string" />

    <xsl:sequence select="mcrmedia:get-sources($derivateId, $path, ())" />
  </xsl:function>

  <xsl:function name="mcrmedia:get-sources" as="element(source)*">
    <xsl:param name="derivateId" as="xs:string" />
    <xsl:param name="path" as="xs:string" />
    <xsl:param name="userAgent" as="xs:string?" />

    <xsl:variable name="uri" as="xs:string" select="
      concat(
        $mediaSourcesURIPrefix,
        'getSources?derivateId=',
        encode-for-uri($derivateId),
        '&amp;path=',
        encode-for-uri($path),
        if (exists($userAgent)) then concat('&amp;userAgent=', encode-for-uri($userAgent)) else ''
      )
    " />

    <xsl:sequence select="document($uri)/sources/source" />
  </xsl:function>
</xsl:stylesheet>
