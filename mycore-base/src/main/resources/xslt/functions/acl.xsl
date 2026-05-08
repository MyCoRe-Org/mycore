<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mcracl="http://www.mycore.de/xslt/acl"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:function name="mcracl:check-permission" as="xs:boolean">
    <xsl:param name="id" as="xs:string?" />
    <xsl:param name="permission" as="xs:string" />
    <xsl:variable name="uri" select="xs:anyURI(concat('checkPermission:', (if (fn:string-length($id) &gt; 0)
     then (concat($id,':'))
     else ('')), $permission))" />
    <xsl:variable name="permDoc" select="document($uri)" />
    <xsl:sequence select="xs:boolean($permDoc/boolean/text())" />
  </xsl:function>

  <xsl:function name="mcracl:is-current-user-guest-user" as="xs:boolean">
    <xsl:sequence select="$CurrentUser=(mcrproperty:one('MCR.Users.Guestuser.UserName'),'guest')[1]" />
  </xsl:function>

  <xsl:function name="mcracl:is-current-user-in-role" as="xs:boolean">
    <xsl:param name="role" as="xs:string" />
    <xsl:variable name="uri" select="xs:anyURI(concat('currentUserInfo:role=',$role))" />
    <xsl:variable name="currentUser" select="document($uri)" />
    <xsl:sequence select="$currentUser/user/role[@name=$role]/text()='true'" />
  </xsl:function>

</xsl:stylesheet>
