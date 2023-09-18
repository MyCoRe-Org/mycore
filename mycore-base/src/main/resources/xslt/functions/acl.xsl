<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcracl="http://www.mycore.de/xslt/acl"
                exclude-result-prefixes="fn xs">

  <xsl:function name="mcracl:check-permission" as="xs:boolean">
    <xsl:param name="id" as="xs:string?" />
    <xsl:param name="permission" as="xs:string" />
    <xsl:variable name="uri" select="xs:anyURI(concat('checkPermission:', (if (fn:string-length($id) &gt; 0)
     then (concat($id,':'))
     else ('')), $permission))" />
    <xsl:variable name="permDoc" select="document($uri)" />
    <xsl:sequence select="xs:boolean($permDoc/boolean/text())" />
  </xsl:function>

</xsl:stylesheet>
