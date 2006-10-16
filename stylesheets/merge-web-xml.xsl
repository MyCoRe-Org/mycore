<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.2.2.2 $ $Date: 2006-10-16 16:56:25 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <xsl:output indent="yes" method="xml" encoding="UTF-8" />
  <xsl:strip-space elements="*" />
  <xsl:preserve-space elements="" />

  <xsl:template match="/">
    <xsl:variable name="wia" xmlns="" xmlns:servlet="http://java.sun.com/xml/ns/j2ee"
      select="document('web1.xml')/*[local-name()='web-app']" />
    <xsl:variable name="wib" select="*[local-name()='web-app']" />

    <web-app version="2.4" xmlns="http://java.sun.com/xml/ns/j2ee"
      xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd">
      <xsl:copy-of select="$wia/*[local-name()='display-name']" />

      <xsl:copy-of select="$wia/*[local-name()='listener']" />
      <xsl:copy-of select="$wib/*[local-name()='listener']" />

      <xsl:copy-of select="$wia/*[local-name()='filter']" />
      <xsl:copy-of select="$wib/*[local-name()='filter']" />

      <xsl:copy-of select="$wia/*[local-name()='servlet']" />
      <xsl:copy-of select="$wib/*[local-name()='servlet']" />

      <xsl:copy-of select="$wia/*[local-name()='filter-mapping']" />
      <xsl:copy-of select="$wib/*[local-name()='filter-mapping']" />

      <xsl:copy-of select="$wia/*[local-name()='servlet-mapping']" />
      <xsl:copy-of select="$wib/*[local-name()='servlet-mapping']" />

      <xsl:copy-of select="$wia/*[local-name()='mime-mapping']" />
      <xsl:copy-of select="$wib/*[local-name()='mime-mapping']" />

      <xsl:copy-of select="$wia/*[local-name()='session-config']" />
      <xsl:copy-of select="$wia/*[local-name()='welcome-file-list']" />
    </web-app>
  </xsl:template>
</xsl:stylesheet>

