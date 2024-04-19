<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="*">
    <processor
      id="xslt"
      version="{system-property('xsl:version')}"
      vendor="{system-property('xsl:vendor')}"
      vendor-url="{system-property('xsl:vendor-url')}"
    />
  </xsl:template>

</xsl:stylesheet>
