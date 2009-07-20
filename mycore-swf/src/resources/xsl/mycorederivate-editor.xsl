<?xml version="1.0" encoding="UTF-8"?>
<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2007-11-26 10:07:30 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager"
> 

<xsl:output method="xml" encoding="UTF-8"/>

<xsl:template match="/mycorederivate">
  <mycorederivate>
    <xsl:copy-of select="@ID"/>
    <xsl:copy-of select="@label"/>
    <xsl:copy-of select="@version"/>
    <xsl:copy-of select="@xsi:noNamespaceSchemaLocation"/>
	<!-- check the WRITEDB permission -->
	<xsl:if test="acl:checkPermission(@ID,'writedb')">
      <xsl:copy-of select="derivate"/>
      <xsl:copy-of select="service"/>
    </xsl:if>
  </mycorederivate>
</xsl:template>

</xsl:stylesheet>
