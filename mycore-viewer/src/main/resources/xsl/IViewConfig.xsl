<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions"
                xmlns:uuid="java:java.util.UUID"
                exclude-result-prefixes="iview2">
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="MCR.Viewer.bootstrapURL" />

  <xsl:output method="html" encoding="UTF-8" indent="yes" />

  <xsl:template match="/">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="/IViewConfig">
    <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
    <html>
      <head>
        <script type="text/javascript" src="//code.jquery.com/jquery-3.3.1.min.js"></script>
        <xsl:choose>
          <xsl:when test="xml/properties/property[@name='mobile'] = 'true'">
            <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no" />
            <link href="//netdna.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.css" rel="stylesheet"
                  type="text/css" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:if test="string-length($MCR.Viewer.bootstrapURL)&gt;0">
              <script type="text/javascript" src="{$MCR.Viewer.bootstrapURL}/js/bootstrap.min.js"></script>
              <link href="{$MCR.Viewer.bootstrapURL}/css/bootstrap.css" type="text/css" rel="stylesheet"></link>
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates select="xml/resources/resource" mode="iview.resource" />
        <script>
          window.onload = function() {
            var json = <xsl:value-of select="json" />;
            new mycore.viewer.MyCoReViewer(jQuery("body"), json.properties);
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
        <script src="{text()}" type="text/javascript" />
      </xsl:when>
      <xsl:when test="@type='css'">
        <link href="{text()}" type="text/css" rel="stylesheet"></link>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
