<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  exclude-result-prefixes="xsl">

  <xsl:template match="/ORCIDOAuthResponse">
    <xsl:choose>
      <xsl:when test="error">
        <div>ERROR</div>
      </xsl:when>
      <xsl:otherwise>
        <div>SUCCESS</div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
