<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcrurl="http://www.mycore.de/xslt/url"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n">

  <!-- not tested with xslt 3-->

  <xsl:template match="altoChanges">
    <script src="{$WebApplicationBaseURL}modules/iview2/alto/list.js"></script>
    <xsl:variable name="sessionID" select="mcrurl:get-param($RequestURL, 'sessionID')" />

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
            <xsl:value-of select="mcri18n:translate('component.viewer.altoChanges.sessionID')" />
          </th>
          <th>
            <xsl:value-of select="mcri18n:translate('component.viewer.altoChanges.objectTitle')" />
          </th>
          <th>
            <xsl:value-of select="mcri18n:translate('component.viewer.altoChanges.derivateID')" />
          </th>
          <th>
            <xsl:value-of select="mcri18n:translate('component.viewer.altoChanges.created')" />
          </th>
        </tr>
      </thead>
      <tbody data-id="list-table-body" style="cursor:pointer">

      </tbody>
    </table>
  </xsl:template>

</xsl:stylesheet>
