<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:mcraccesskey="http://www.mycore.de/xslt/accesskey"
  exclude-result-prefixes="fn xs">

  <xsl:function name="mcraccesskey:check-rest-path-permission" as="xs:boolean">
    <xsl:param name="typeId" as="xs:string" />
    <xsl:param name="permission" as="xs:string" />
    <xsl:variable name="permDoc" select="fn:document(concat('checkrestapiaccess:', (if ($typeId='derivate')
      then ('derivates/{derid}/accesskeys')
      else ('accesskeys')), ':', $permission))" />
    <xsl:value-of select="$permDoc/boolean/text()" />
  </xsl:function>

</xsl:stylesheet>
