<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions"
  exclude-result-prefixes="iview2">
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="MCR.Module-iview2.BaseURL" />
  <xsl:param name="MCR.Module-iview2.bootstrapURL" />
  <xsl:param name="MCR.Module-iview2.DeveloperMode" />

  <xsl:output method="html" encoding="UTF-8" indent="yes" />

  <xsl:template match="/">
    <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="/IViewConfig">
    <xsl:variable name="minified">
      <xsl:if test="$MCR.Module-iview2.DeveloperMode != 'true'">
        <xsl:value-of select="'.min'" />
      </xsl:if>
    </xsl:variable>

    <html>
      <head>
        <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/lib/jquery-2.0.3.min.js"></script>
        <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview-client-base{$minified}.js"></script>

        <xsl:choose>
          <xsl:when test="iviewClientConfiguration/mobile = 'true'">
            <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview-client-mobile{$minified}.js"></script>
            <link href="{$WebApplicationBaseURL}modules/iview2/css/mobile.css" type="text/css" rel="stylesheet"></link>
            <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no" />
            <link href="//netdna.bootstrapcdn.com/font-awesome/4.0.3/css/font-awesome.css" rel="stylesheet" />
          </xsl:when>
          <xsl:otherwise>
            <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview-client-desktop{$minified}.js"></script>
            <script type="text/javascript" src="{$MCR.Module-iview2.bootstrapURL}/js/bootstrap.min.js"></script>
            <link href="{$MCR.Module-iview2.bootstrapURL}/css/bootstrap.css" type="text/css" rel="stylesheet"></link>
            <link href="{$WebApplicationBaseURL}modules/iview2/css/default.css" type="text/css" rel="stylesheet"></link>
          </xsl:otherwise>
        </xsl:choose>

        <xsl:choose>
          <xsl:when test="iviewClientConfiguration/doctype = 'mets'">
            <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview-client-mets{$minified}.js"></script>
          </xsl:when>
          <xsl:when test="iviewClientConfiguration/doctype = 'pdf'">
            <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview-client-pdf{$minified}.js"></script>
          </xsl:when>
        </xsl:choose>

        <xsl:if test="iviewClientConfiguration/metadataUrl and iviewClientConfiguration/metadataUrl!=''">
          <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/iview-client-metadata{$minified}.js"></script>
        </xsl:if>

        <xsl:apply-templates select="iviewClientConfiguration/resources/resource" mode="iview.resource"/>

        <script>
          window.onload = function() {
          new mycore.iview.imageviewer.MyCoReImageViewer(jQuery("body"),
          <xsl:value-of select="jsonConfig" />
          );
          };
        </script>
      </head>
      <body>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="resource" mode="iview.resource">
    <xsl:choose>
      <xsl:when test="@type='script'">
        <script src="{$WebApplicationBaseURL}{text()}" type="text/javascript"  />
      </xsl:when>
      <xsl:when test="@type='css'">
        <link href="{$WebApplicationBaseURL}{text()}" type="text/css" rel="stylesheet"></link>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>