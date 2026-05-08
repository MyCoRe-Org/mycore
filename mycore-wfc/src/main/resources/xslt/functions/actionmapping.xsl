<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcractionmapping="http://www.mycore.de/xslt/actionmapping"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:variable name="actionMappingURIPrefix" as="xs:string" select="'actionmapping:'" />

  <xsl:function name="mcractionmapping:get-url-for-id" as="xs:anyURI?">
    <xsl:param name="action" as="xs:string" />
    <xsl:param name="mcrID" as="xs:string" />
    <xsl:param name="absolute" as="xs:boolean" />

    <xsl:variable name="url" as="xs:string" select="
      string(document(
        concat(
          $actionMappingURIPrefix,
          'getURLforID?action=',
          $action,
          '&amp;id=',
          $mcrID,
          '&amp;absolute=',
          $absolute
        )
      )/string)
    " />
    <xsl:sequence select="if ($url != '') then xs:anyURI($url) else ()" />
  </xsl:function>

  <xsl:function name="mcractionmapping:get-url-for-collection" as="xs:anyURI?">
    <xsl:param name="action" as="xs:string" />
    <xsl:param name="collection" as="xs:string" />
    <xsl:param name="absolute" as="xs:boolean" />

    <xsl:variable name="url" as="xs:string" select="
      string(document(
        concat(
          $actionMappingURIPrefix,
          'getURLforCollection?action=',
          $action,
          '&amp;collection=',
          $collection,
          '&amp;absolute=',
          $absolute
        )
      )/string)
    " />
    <xsl:sequence select="if ($url != '') then xs:anyURI($url) else ()" />
  </xsl:function>

</xsl:stylesheet>
