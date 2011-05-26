<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- Adds share/subscribe buttons for links and RSS feeds -->

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:encoder="xalan://java.net.URLEncoder"  
  exclude-result-prefixes="xalan i18n encoder">

<!-- Adds a Share button to link to URLs -->
  
<xsl:template name="shareButton">
  <xsl:param name="linkURL" /> 
  <xsl:param name="linkTitle" />
  <xsl:param name="alt" select="i18n:translate('share')" />
  
  <xsl:call-template name="linkButton">
    <xsl:with-param name="linkURL" select="$linkURL" />
    <xsl:with-param name="linkTitle" select="$linkTitle" />
    <xsl:with-param name="alt" select="$alt" />
    <xsl:with-param name="linkType" select="'share_save'" />
    <xsl:with-param name="js" select="'page'" />
  </xsl:call-template>
</xsl:template>

<!-- Adds a Subscribe button to subscribe to RSS feed -->
<xsl:template name="subscribeButton">
  <xsl:param name="feedURL" /> 
  <xsl:param name="feedTitle" />
  <xsl:param name="alt" select="i18n:translate('subscribe')" />
  
  <xsl:call-template name="linkButton">
    <xsl:with-param name="linkURL" select="$feedURL" />
    <xsl:with-param name="linkTitle" select="$feedTitle" />
    <xsl:with-param name="alt" select="$alt" />
    <xsl:with-param name="linkType" select="'subscribe'" />
    <xsl:with-param name="js" select="'feed'" />
  </xsl:call-template>
</xsl:template>

<!-- Adds a Share/Subscribe button that links to AddToAny service -->
<xsl:template name="linkButton">
  <xsl:param name="linkURL" /> 
  <xsl:param name="linkTitle" />
  <xsl:param name="linkType" />
  <xsl:param name="alt" />
  <xsl:param name="js" />
  
  <a class="a2a_dd" href="http://www.addtoany.com/{$linkType}?linkurl={encoder:encode($linkURL,'UTF-8')}&amp;linkname={$linkTitle}">
    <img src="http://static.addtoany.com/buttons/{$linkType}_171_16.gif" width="171" height="16" border="0" alt="{$alt}" />
  </a>
  <xsl:variable name="menuLang">
    <xsl:choose>
      <xsl:when test="$CurrentLang = 'en'">en-US</xsl:when>
      <xsl:otherwise><xsl:value-of select="$CurrentLang" /></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <script type="text/javascript">
    var a2a_config = a2a_config || {};
    a2a_config.onclick = 1;
    a2a_config.linkname = "<xsl:value-of select="$linkTitle" />";
    a2a_config.linkurl = "<xsl:value-of select="$linkURL" />";
    a2a_config.locale = "<xsl:value-of select="$menuLang" />";
  </script>
  <script type="text/javascript" src="http://static.addtoany.com/menu/{$js}.js"><xsl:text> </xsl:text></script>
</xsl:template>
  
</xsl:stylesheet>
