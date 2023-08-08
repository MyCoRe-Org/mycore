<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcrderivate="http://www.mycore.de/xslt/derivate"
                xmlns:mcrproperty="http://www.mycore.de/xslt/property"
>


  <xsl:template name="iview2.getSupport">
    <xsl:param name="derivID" />
    <xsl:choose>
      <xsl:when test="$derivID">
        <xsl:variable name="supportedContentTypeStr" select="mcrproperty:one('MCR.Module-iview2.SupportedContentTypes')" />
        <xsl:variable name="mainFile" select="mcrderivate:get-main-file($derivID)" />
        <xsl:variable name="contentType" select="mcrderivate:get-file-content-type($derivID, $mainFile)" />
        <xsl:variable name="supportedContentTypes" select="tokenize($supportedContentTypeStr, ',')" />
        <xsl:if test="contains($supportedContentTypes, $contentType)">
          <xsl:value-of select="$mainFile" />
        </xsl:if>
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="derivateView">
    <xsl:param name="derivateID" />
    <xsl:param name="extensions" />
    <xsl:param name="imageSize" />
    <xsl:call-template name="derivateLinkView">
      <xsl:with-param name="derivateID" select="$derivateID" />
      <xsl:with-param name="file">
        <xsl:call-template name="iview2.getSupport" >
          <xsl:with-param name="derivID" select="$derivateID" />
        </xsl:call-template>
      </xsl:with-param>
      <xsl:with-param name="extensions" select="$extensions" />
      <xsl:with-param name="imageSize" select="$imageSize" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="derivateLinkView">
    <xsl:param name="file" />
    <xsl:param name="derivateID" />
    <xsl:param name="extensions" />
    <xsl:param name="imageSize" />
    <xsl:if test="$file != ''">
      <xsl:call-template name="iview2.clientLink">
        <xsl:with-param name="derivateID" select="$derivateID" />
        <xsl:with-param name="file" select="$file" />
        <xsl:with-param name="imageSize" select="$imageSize" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="iview2.clientLink">
    <xsl:param name="file" />
    <xsl:param name="derivateID" />
    <xsl:param name="imageSize" />

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
          <xsl:with-param name="imageSize" select="$imageSize" />
        </xsl:call-template>
      </a>
    </div>
  </xsl:template>

  <xsl:template name="iview2.getImageElement">
    <xsl:param name="derivate" />
    <xsl:param name="imagePath" />
    <xsl:param name="imageSize" select="'THUMBNAIL'" />
    <xsl:param name="style" select="''" />
    <xsl:param name="class" select="''" />

    <xsl:comment>
      Begin - iview2.getImageElement (mcr-module-startIview2.xsl)
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

    <img src="{concat($WebApplicationBaseURL,'servlets/MCRTileCombineServlet/',$imageSize,'/',$derivate,'/', encode-for-uri($file))}" style="{$style}" class="{$class}" />

    <xsl:comment>
      End - iview2.getImageElement (mcr-module-startIview2.xsl)
    </xsl:comment>
  </xsl:template>
</xsl:stylesheet>
