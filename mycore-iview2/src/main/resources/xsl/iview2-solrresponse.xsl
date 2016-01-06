<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:acl="xalan://org.mycore.access.MCRAccessManager"
  xmlns:xalan="http://xml.apache.org/xalan" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" exclude-result-prefixes="acl mcrxsl encoder xalan i18n">

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
          select="concat($ServletsBaseURL,'MCRThumbnailServlet/',$derivateId,mcrxsl:encodeURIPath($filePath),$HttpSession)" />
        <xsl:choose>
          <xsl:when test="$MCR.Module-iview2.useNewViewer='true'">
            <a onMouseOver="show('{$toolTipImg}')" onMouseOut="toolTip()" href="{$WebApplicationBaseURL}rsc/viewer/{$derivateId}/{$filePath}"
              title="{i18n:translate('metaData.iView')}">
              <xsl:value-of select="$fileName" />
            </a>
          </xsl:when>
          <xsl:otherwise>
            <a onMouseOver="show('{$toolTipImg}')" onMouseOut="toolTip()"
              href="{concat($WebApplicationBaseURL, 'receive/', $mcrid, '?jumpback=true&amp;maximized=true&amp;page=',$filePath,'&amp;derivate=', $derivateId)}"
              title="{i18n:translate('metaData.iView')}">
              <xsl:value-of select="$fileName" />
            </a>
          </xsl:otherwise>
        </xsl:choose>

      </xsl:when>
      <xsl:otherwise>
        <a href="{concat($fileNodeServlet,$derivateId,mcrxsl:encodeURIPath($filePath),$HttpSession)}">
          <xsl:value-of select="$fileName" />
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="doc" mode="iview">
    <xsl:variable name="mcrid" select="@id" />
    <xsl:variable name="derivates" select="key('derivate', $mcrid)" />

    <xsl:comment>
      Start - match="doc" mode="iview"
    </xsl:comment>

    <xsl:for-each select="$derivates/str[@name='maindoc']">
      <xsl:call-template name="iViewLinkPrev">
        <xsl:with-param name="mcrid" select="$mcrid" />
        <xsl:with-param name="derivate" select="../str[@name='id']" />
        <xsl:with-param name="fileName" select="." />
      </xsl:call-template>
    </xsl:for-each>

    <xsl:for-each select="./arr[@name='derivateLink']/str">
      <xsl:call-template name="iViewLinkPrev">
        <xsl:with-param name="mcrid" select="$mcrid" />
        <xsl:with-param name="derivateLink" select="." />
      </xsl:call-template>
    </xsl:for-each>

    <xsl:comment>
      End - match="doc" mode="iview"
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
        <xsl:variable name="object-view-derivate" select="acl:checkPermission($mcrid,'view-derivate')" />
        <xsl:variable name="isDisplayedEnabled" select="mcrxsl:isDisplayedEnabledDerivate($derivate)" />
        <xsl:variable name="mayWriteDerivate" select="acl:checkPermission($derivate,'writedb')" />
        <xsl:choose>
          <xsl:when test="($object-view-derivate and $isDisplayedEnabled = 'true') or $mayWriteDerivate">

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

            <a href="{$href}" title="{i18n:translate('metaData.iView')}">
              <xsl:call-template name="iview2.getImageElement">
                <xsl:with-param select="$derivate" name="derivate" />
                <xsl:with-param select="$pageToDisplay" name="imagePath" />
                <xsl:with-param select="'resultListPreviewImage'" name="class" />
              </xsl:call-template>
            </a>
          </xsl:when>

          <xsl:otherwise>
            <xsl:if test="$isDisplayedEnabled = 'true'">
              <xsl:variable name="objectType" select="substring-before(substring-after($mcrid,'_'),'_')" />
              <span>
                <!-- Zugriff auf 'Abbildung' gesperrt -->
                <xsl:value-of select="i18n:translate('metaData.derivateLocked',i18n:translate(concat('metaData.',$objectType,'.[derivates]')))" />
              </span>
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
    </xsl:if>
    <xsl:comment>
      End - iViewLinkPrev (iview2-solrresponse.xsl)
    </xsl:comment>
  </xsl:template>
</xsl:stylesheet>