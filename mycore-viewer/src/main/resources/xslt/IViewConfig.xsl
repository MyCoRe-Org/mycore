<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html" media-type="text/html" />

  <xsl:include href="default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />
  <xsl:param name="MCR.Viewer.bootstrapURL" /> <!-- just for legacy reasons -->
  <xsl:param name="MCR.Viewer.FontaweSomeURL" />
  <xsl:param name="MCR.Viewer.BootstrapURL" >
    <xsl:if test="string-length($MCR.Viewer.bootstrapURL)&gt;0">
      <xsl:choose>
        <xsl:when
                test="'/'=substring($MCR.Viewer.bootstrapURL, string-length($MCR.Viewer.bootstrapURL) - string-length('/') + 1)">
          <xsl:value-of select="$MCR.Viewer.bootstrapURL"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat($MCR.Viewer.bootstrapURL, '/')"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:param>

  <xsl:output method="html" encoding="UTF-8" indent="yes" />

  <xsl:template match="/">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="/IViewConfig">
    <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
    <html>
      <head>
        <script type="text/javascript" src="{$WebApplicationBaseURL}modules/iview2/js/lib/jquery.min.js"></script>
          <xsl:if test="string-length($MCR.Viewer.BootstrapURL)&gt;0">
              <script type="text/javascript" src="{$MCR.Viewer.BootstrapURL}js/bootstrap.min.js"></script>
              <link href="{$MCR.Viewer.BootstrapURL}css/bootstrap.css" type="text/css" rel="stylesheet"></link>
          </xsl:if>
          <xsl:if test="string-length($MCR.Viewer.FontaweSomeURL)&gt;0">
              <link href="{$MCR.Viewer.FontaweSomeURL}" type="text/css" rel="stylesheet"></link>
          </xsl:if>
        <xsl:apply-templates select="xml/resources/resource" mode="iview.resource" />
        <script type="module">
          import { MyCoReViewer } from '<xsl:value-of select="$WebApplicationBaseURL" />modules/iview2/js/iview-client-base.es.js';
          window.onload = function() {
            var json = <xsl:value-of select="json" />;
            new MyCoReViewer(jQuery("body"), json.properties);
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
