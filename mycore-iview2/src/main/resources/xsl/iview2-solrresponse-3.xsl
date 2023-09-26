<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcracl="http://www.mycore.de/xslt/acl"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n">

  <xsl:param name="MCR.Module-iview2.useNewViewer" />


  <xsl:template match="doc" mode="fileLink" priority="2">
    <xsl:param name="mcrid" select="str[@name='returnId']" />
    <xsl:param name="derivateId" select="str[@name='derivateID']" />
    <xsl:param name="fileNodeServlet" select="concat($ServletsBaseURL,'MCRFileNodeServlet/')" />
    <!-- doc element of 'unmerged' response -->
    <xsl:variable name="filePath" select="str[@name='filePath']" />
    <xsl:variable name="fileName" select="str[@name='fileName']" />

    <xsl:choose>
      <xsl:when test="key('derivate',$mcrid)[str/@name='iviewFile' and str[@name='id']=$derivateId]">
        <!-- iview support detected generate link to image viewer -->
        <xsl:variable name="toolTipImg"
          select="concat($ServletsBaseURL,'MCRThumbnailServlet/',$derivateId, encode-for-uri($filePath),$HttpSession)" />
        <xsl:choose>
          <xsl:when test="$MCR.Module-iview2.useNewViewer='true'">
            <a onMouseOver="show('{$toolTipImg}')" onMouseOut="toolTip()" href="{$WebApplicationBaseURL}rsc/viewer/{$derivateId}/{$filePath}"
              title="{mcri18n:translate('metaData.iView')}">
              <xsl:value-of select="$fileName" />
            </a>
          </xsl:when>
          <xsl:otherwise>
            <a onMouseOver="show('{$toolTipImg}')" onMouseOut="toolTip()"
              href="{concat($WebApplicationBaseURL, 'receive/', $mcrid, '?jumpback=true&amp;maximized=true&amp;page=',$filePath,'&amp;derivate=', $derivateId)}"
              title="{mcri18n:translate('metaData.iView')}">
              <xsl:value-of select="$fileName" />
            </a>
          </xsl:otherwise>
        </xsl:choose>

      </xsl:when>
      <xsl:otherwise>
        <a href="{concat($fileNodeServlet,$derivateId,encode-for-uri($filePath),$HttpSession)}">
          <xsl:value-of select="$fileName" />
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="doc" mode="iview">
    <xsl:param name="imageSize" select="'THUMBNAIL'"/>

    <xsl:variable name="mcrid" select="@id" />
    <xsl:variable name="derivates" select="key('derivate', $mcrid)" />

    <xsl:comment>
      Start - match="doc" mode="iview" (iview2-solrresponse.xsl)
    </xsl:comment>

    <xsl:for-each select="$derivates/str[@name='derivateMaindoc']">
      <xsl:call-template name="iViewLinkPrev">
        <xsl:with-param name="mcrid" select="$mcrid" />
        <xsl:with-param name="derivate" select="../str[@name='id']" />
        <xsl:with-param name="fileName" select="." />
        <xsl:with-param name="imageSize" select="$imageSize" />
      </xsl:call-template>
    </xsl:for-each>

    <xsl:for-each select="./arr[@name='derivateLink']/str">
      <xsl:call-template name="iViewLinkPrev">
        <xsl:with-param name="mcrid" select="$mcrid" />
        <xsl:with-param name="derivateLink" select="." />
        <xsl:with-param name="imageSize" select="$imageSize" />
      </xsl:call-template>
    </xsl:for-each>

    <xsl:comment>
      End - match="doc" mode="iview" (iview2-solrresponse.xsl)
    </xsl:comment>
  </xsl:template>

  <xsl:template name="iViewLinkPrev">
    <xsl:param name="derivateLink" />
    <xsl:param name="derivate">
      <xsl:if test="$derivateLink">
        <xsl:value-of select="substring-before($derivateLink , '/')" />
      </xsl:if>
    </xsl:param>
    <xsl:param name="mcrid" />
    <xsl:param name="fileName" />
    <xsl:param name="class" select="'resultListPreviewImage'" />
    <xsl:param name="imageSize" select="'THUMBNAIL'" />

    <xsl:comment>
      Start - iViewLinkPrev (iview2-solrresponse.xsl)
    </xsl:comment>

    <xsl:if test="string-length($derivate) &gt; 0 and $mcrid">
      <xsl:variable name="pageToDisplay">
        <xsl:choose>
          <xsl:when test="$fileName">
            <xsl:value-of select="$fileName" />
          </xsl:when>
          <xsl:when test="$derivateLink">
            <xsl:value-of select="concat('/', substring-after($derivateLink, '/'))" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="iview2.getSupport">
              <xsl:with-param select="$derivate" name="derivID" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:if test="$pageToDisplay != ''">
        <xsl:variable name="object-view-derivate" select="mcracl:check-permission($derivate,'view')" />
        <xsl:variable name="object-read-derivate" select="mcracl:check-permission($derivate,'read')" />
        <xsl:variable name="mayWriteDerivate" select="mcracl:check-permission($derivate,'writedb')" />
        <xsl:choose>
          <xsl:when test="$object-view-derivate or $object-read-derivate or $mayWriteDerivate">
            <xsl:variable name="file">
              <xsl:choose>
                <xsl:when test="starts-with($pageToDisplay, '/')">
                  <xsl:value-of select="substring-after($pageToDisplay,'/')" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$pageToDisplay" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>

            <xsl:variable name="href" select="concat($WebApplicationBaseURL,  'rsc/viewer/', $derivate, '/', $file)" />

            <a href="{$href}" title="{mcri18n:translate('metaData.iView')}">
              <xsl:call-template name="iview2.getImageElement">
                <xsl:with-param select="$derivate" name="derivate" />
                <xsl:with-param select="$pageToDisplay" name="imagePath" />
                <xsl:with-param select="$class" name="class" />
                <xsl:with-param select="$imageSize" name="imageSize" />
              </xsl:call-template>
            </a>
          </xsl:when>

          <xsl:otherwise>
              <xsl:variable name="objectType" select="substring-before(substring-after($mcrid,'_'),'_')" />
              <span class="derivateLocked">
                <!-- Zugriff auf 'Abbildung' gesperrt -->
                <xsl:value-of select="mcri18n:translate-with-params('metaData.derivateLocked',mcri18n:translate(concat('metaData.',$objectType,'.[derivates]')))" />
              </span>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
    </xsl:if>
    <xsl:comment>
      End - iViewLinkPrev (iview2-solrresponse.xsl)
    </xsl:comment>
  </xsl:template>
</xsl:stylesheet>
