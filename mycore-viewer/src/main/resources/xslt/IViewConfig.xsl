<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html" media-type="text/html" />

  <xsl:include href="default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:param name="MCR.Viewer.Bootstrap.Css.URL"/>
  <xsl:param name="MCR.Viewer.Bootstrap.Css.Integrity"/>
  <xsl:param name="MCR.Viewer.Bootstrap.Js.URL"/>
  <xsl:param name="MCR.Viewer.Bootstrap.Js.Integrity"/>
  <xsl:param name="MCR.Viewer.Popper.Js.URL"/>
  <xsl:param name="MCR.Viewer.Popper.Js.Integrity"/>

  <xsl:param name="MCR.Viewer.Fontawesome.Css.URL"/>
  <xsl:param name="MCR.Viewer.Fontawesome.Css.Integrity"/>
  <xsl:param name="MCR.Viewer.Fontawesome.Js.URL"/>
  <xsl:param name="MCR.Viewer.Fontawesome.Js.Integrity"/>

  <xsl:output method="html" encoding="UTF-8" indent="yes" />

  <xsl:template match="/">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template name="createViewerLinkElement">
    <xsl:param name="href"/>
    <xsl:param name="integrity"/>

    <xsl:if test="$href and string-length($href) &gt; 0">
      <xsl:element name="link">
        <xsl:attribute name="rel">stylesheet</xsl:attribute>
        <xsl:attribute name="href">
          <xsl:value-of select="$href"/>
        </xsl:attribute>
        <xsl:if test="$integrity and string-length($integrity) &gt; 0">
          <xsl:attribute name="integrity">
            <xsl:value-of select="$integrity"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="crossorigin">anonymous</xsl:attribute>
      </xsl:element>
    </xsl:if>
  </xsl:template>

  <xsl:template name="createViewerScriptElement">
    <xsl:param name="src"/>
    <xsl:param name="integrity"/>

    <xsl:if test="$src and string-length($src) &gt; 0">
      <xsl:element name="script">
        <xsl:attribute name="src">
          <xsl:value-of select="$src"/>
        </xsl:attribute>
        <xsl:if test="$integrity and string-length($integrity) &gt; 0">
          <xsl:attribute name="integrity">
            <xsl:value-of select="$integrity"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="crossorigin">anonymous</xsl:attribute>
      </xsl:element>
  </xsl:template>

  <xsl:template match="/IViewConfig">
    <html>
      <head>
        <xsl:call-template name="createViewerLinkElement">
            <xsl:with-param name="href" select="$MCR.Viewer.Bootstrap.Css.URL"/>
            <xsl:with-param name="integrity" select="$MCR.Viewer.Bootstrap.Css.Integrity"/>
        </xsl:call-template>
        <xsl:call-template name="createViewerScriptElement">
            <xsl:with-param name="src" select="$MCR.Viewer.Bootstrap.Js.URL"/>
            <xsl:with-param name="integrity" select="$MCR.Viewer.Bootstrap.Js.Integrity"/>
        </xsl:call-template>
        <xsl:call-template name="createViewerScriptElement">
            <xsl:with-param name="src" select="$MCR.Viewer.Popper.Js.URL"/>
            <xsl:with-param name="integrity" select="$MCR.Viewer.Popper.Js.Integrity"/>
        </xsl:call-template>
        <xsl:call-template name="createViewerLinkElement">
            <xsl:with-param name="href" select="$MCR.Viewer.Fontawesome.Css.URL"/>
            <xsl:with-param name="integrity" select="$MCR.Viewer.Fontawesome.Css.Integrity"/>
        </xsl:call-template>
        <xsl:call-template name="createViewerScriptElement">
            <xsl:with-param name="src" select="$MCR.Viewer.Fontawesome.Js.URL"/>
            <xsl:with-param name="integrity" select="$MCR.Viewer.Fontawesome.Js.Integrity"/>
        </xsl:call-template>

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
