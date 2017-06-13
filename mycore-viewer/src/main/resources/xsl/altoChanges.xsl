<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
                exclude-result-prefixes="i18n">


  <xsl:param name="WebApplicationBaseURL"></xsl:param>
  <xsl:param name="RequestURL" />


  <xsl:template match="altoChanges">
    <script src="{$WebApplicationBaseURL}modules/iview2/alto/list.js"></script>
    <xsl:variable name="sessionID">
      <xsl:call-template name="UrlGetParam">
        <xsl:with-param name="url" select="$RequestURL" />
        <xsl:with-param name="par" select="'sessionID'" />
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="listURL">
      <xsl:choose>
        <xsl:when test="string-length($sessionID) &gt; 0">
          <xsl:value-of select="concat($WebApplicationBaseURL,'rsc/viewer/alto/list?session=', $sessionID)" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat($WebApplicationBaseURL,'rsc/viewer/alto/list')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <table class="table" data-base-url="{$WebApplicationBaseURL}"
           data-alto-changes="{$listURL}">
      <thead>
        <tr>
          <th>
            <xsl:value-of select="i18n:translate('component.viewer.altoChanges.sessionID')" />
          </th>
          <th>
            <xsl:value-of select="i18n:translate('component.viewer.altoChanges.objectTitle')" />
          </th>
          <th>
            <xsl:value-of select="i18n:translate('component.viewer.altoChanges.derivateID')" />
          </th>
          <th>
            <xsl:value-of select="i18n:translate('component.viewer.altoChanges.created')" />
          </th>
        </tr>
      </thead>
      <tbody data-id="list-table-body" style="cursor:pointer">

      </tbody>
    </table>
  </xsl:template>

</xsl:stylesheet>
