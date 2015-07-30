<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  exclude-result-prefixes="i18n">

  <!-- To use this functionality just create a <xsl:template match="doc" mode="printlatestobjects"> where you define the content of the result-table -->
  <xsl:template match="printlatestobjects">
    <xsl:variable name="objectType" select="@objecttype" />
    <xsl:variable name="sortField" select="@sortfield" />
    <xsl:variable name="sortOrder" select="@sortorder" />
    <xsl:variable name="maxResults" select="@maxresults" />

    <xsl:variable name="queryURI"
      select="concat($WebApplicationBaseURL, 'servlets/solr/select?q=+objectType:', $objectType, '&amp;sort=', $sortField, '+', $sortOrder, '&amp;rows=', $maxResults, '&amp;XSL.Style=xml')" />
    <table id="resultList" cellspacing="0" cellpadding="0">
      <xsl:apply-templates select="document($queryURI)/response/result/doc" mode="printlatestobjects" />
    </table>
    <div id="latestmore">
      <a href="{$ServletsBaseURL}solr/select?q=+objectType:{$objectType}&amp;sort=modified+desc">
        <xsl:value-of select="i18n:translate('latestObjects.more')" />
      </a>
    </div>
  </xsl:template>

</xsl:stylesheet>
