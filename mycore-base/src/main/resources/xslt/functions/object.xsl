<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrobject="http://www.mycore.de/xslt/object"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:function name="mcrobject:get" as="document-node()?">
    <xsl:param name="id" as="xs:string" />

    <xsl:sequence select="mcrobject:get($id, true())" />
  </xsl:function>

  <xsl:function name="mcrobject:get" as="document-node()?">
    <xsl:param name="id" as="xs:string" />
    <xsl:param name="expanded" as="xs:boolean" />

    <xsl:variable name="uri" select="concat('mcrobject:', $id, if ($expanded) then '' else '?expanded=false')" />
    <xsl:sequence select="if (doc-available($uri)) then doc($uri) else ()" />
  </xsl:function>

  <xsl:function name="mcrobject:get-revision" as="document-node()?">
    <xsl:param name="id" as="xs:string" />
    <xsl:param name="revision" as="xs:string" />

    <xsl:variable name="uri" select="concat('mcrobject:', $id, '?r=', encode-for-uri($revision))" />
    <xsl:sequence select="if (doc-available($uri)) then doc($uri) else ()" />
  </xsl:function>

  <!--
    Returns version information for a MyCoRe object.

    Parameters
      * id: the MCR object ID (e.g. "mcr_test_00000001")

    Returns
      * root element of the version info
    -->
  <xsl:function name="mcrobject:get-version-info" as="document-node()?">
    <xsl:param name="id" as="xs:string" />

    <xsl:sequence select="document(concat('versioninfo:', $id))" />
  </xsl:function>

</xsl:stylesheet>
