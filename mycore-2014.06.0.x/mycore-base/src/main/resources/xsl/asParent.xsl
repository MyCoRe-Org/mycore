<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="mycoreobject">
    <mycoreobject>
      <structure>
        <parents class="MCRMetaLinkID" notinherit="true" heritable="false">
          <parent xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="locator" xlink:href="{@ID}" />
        </parents>
      </structure>
    </mycoreobject>
  </xsl:template>
</xsl:stylesheet>