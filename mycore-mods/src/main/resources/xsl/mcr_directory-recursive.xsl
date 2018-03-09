<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:include href="copynodes.xsl" />
    <xsl:template match="numChildren" />
    <xsl:template match="child[@type='directory']">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
            <path xml:space="preserve">
        <xsl:value-of select="concat(substring-after(substring-after(uri, ':'), ':'),'/')" />
        <xsl:variable name="derId" select="substring-before(substring-after(uri,':/'), ':')" />
        <xsl:variable name="filePath" select="substring-after(substring-after(uri, ':'), ':')" />
        <xsl:variable name="suburi" select="concat('ifs:',$derId,$filePath)" />
        <xsl:apply-templates select="document($suburi)/mcr_directory/children/child" />
      </path>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
