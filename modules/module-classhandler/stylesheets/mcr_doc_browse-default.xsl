<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
>

<xsl:variable name="Navigation.title"       select="i18n:translate('titles.pageTitle.classBrowse')" />

<xsl:variable name="MainTitle" select="i18n:translate('titles.mainTitle')"/>
<xsl:variable name="PageTitle" select="$Navigation.title"/>

<xsl:variable name="Servlet" select="'/browse/*'"/> 
<xsl:variable name="browse.numberOf" select="i18n:translate('Browse.numOf')"/>
<xsl:variable name="browse.doc" select="i18n:translate('Browse.docs','')"/>

<xsl:include href="MyCoReLayout.xsl" />

</xsl:stylesheet>
