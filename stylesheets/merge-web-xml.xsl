<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.2 $ $Date: 2006-03-14 11:18:54 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>
<xsl:output 
  indent="yes"
  method="xml" 
  encoding="UTF-8"
  doctype-system="web-app_2_3.dtd"  
  doctype-public="-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN"
/>
<xsl:strip-space elements="*" />
<xsl:preserve-space elements="" />

<xsl:template match="/">
  <xsl:variable name="wia" select="document('web1.xml')/web-app" />
  <xsl:variable name="wib" select="web-app" />
  
  <web-app>
    <xsl:copy-of select="$wia/display-name" />

    <xsl:copy-of select="$wia/listener" />
    <xsl:copy-of select="$wib/listener" />

    <xsl:copy-of select="$wia/servlet" />
    <xsl:copy-of select="$wib/servlet" />

    <xsl:copy-of select="$wia/servlet-mapping" />
    <xsl:copy-of select="$wib/servlet-mapping" />

    <xsl:copy-of select="$wia/mime-mapping" />
    <xsl:copy-of select="$wib/mime-mapping" />

    <xsl:copy-of select="$wia/session-config" />
    <xsl:copy-of select="$wia/welcome-file-list" />
  </web-app>

</xsl:template>

</xsl:stylesheet>

