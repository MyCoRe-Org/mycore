<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions" exclude-result-prefixes="iview2 encoder">

  <xsl:param name="MCR.Module-iview2.useNewViewer" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />

  <xsl:template name="iview2.getSupport">
    <xsl:param name="derivID" />
    <xsl:value-of select="iview2:getSupportedMainFile($derivID)" />
  </xsl:template>

  <xsl:template name="derivateView">
    <xsl:param name="derivateID" />
    <xsl:param name="extensions" />
    <xsl:call-template name="derivateLinkView">
      <xsl:with-param name="derivateID" select="$derivateID" />
      <xsl:with-param name="file" select="iview2:getSupportedMainFile($derivateID)" />
      <xsl:with-param name="extensions" select="$extensions" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="derivateLinkView">
    <xsl:param name="file" />
    <xsl:param name="derivateID" />
    <xsl:param name="extensions" />
    <xsl:if test="$file != ''">
      <xsl:call-template name="iview2.clientLink">
        <xsl:with-param name="derivateID" select="$derivateID" />
          <xsl:with-param name="file" select="$file" />
        </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="iview2.clientLink">
    <xsl:param name="file" />
    <xsl:param name="derivateID" />
    
    <xsl:variable name="linkedFile">
      <xsl:choose>
        <xsl:when test="starts-with($file, '/')">
          <xsl:value-of select="substring-after($file,'/')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$file" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <div class="container">
      <a class="thumbnail col-md-12" href="{concat($WebApplicationBaseURL,'rsc/viewer/', $derivateID, '/', $linkedFile)}">
        <xsl:call-template name="iview2.getImageElement">
          <xsl:with-param name="derivate" select="$derivateID" />
          <xsl:with-param name="imagePath" select="$file" />
        </xsl:call-template>
      </a>
    </div>
  </xsl:template>

  <xsl:template name="iview2.getImageElement">
    <xsl:param name="derivate" />
    <xsl:param name="imagePath" />
    <xsl:param name="style" select="''" />
    <xsl:param name="class" select="''" />

    <xsl:comment>
      Begin - iview2.getImageElement (mcr-module-iview2.xsl)
    </xsl:comment>

    <xsl:variable name="file">
      <xsl:choose>
        <xsl:when test="starts-with($imagePath, '/')">
          <xsl:value-of select="substring-after($imagePath,'/')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$imagePath" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <img src="{concat($WebApplicationBaseURL,'servlets/MCRTileCombineServlet/THUMBNAIL/',$derivate,'/',$file)}" style="{$style}" class="{$class}" />

    <xsl:comment>
      End - iview2.getImageElement (mcr-module-iview2.xsl)
    </xsl:comment>
  </xsl:template>
</xsl:stylesheet>