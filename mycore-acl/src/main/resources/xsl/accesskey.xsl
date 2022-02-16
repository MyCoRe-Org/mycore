<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template name="checkRestAccessKeyPathPermission">
    <xsl:param name="typeId" />
    <xsl:param name="permission" />
    <xsl:variable name="restPath">
      <xsl:choose>
        <xsl:when test="$typeId='derivate'">
          <xsl:value-of select="'derivates/{derid}/accesskeys'" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'accesskeys'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:value-of select="(document(concat('checkrestapiaccess:', $restPath, ':', $permission))/boolean)" />
  </xsl:template>

</xsl:stylesheet>
