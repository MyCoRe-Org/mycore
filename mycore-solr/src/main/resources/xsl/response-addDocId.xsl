<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:include href="copynodes.xsl" />

	<xsl:template match="doc">
		<xsl:copy>
			<xsl:attribute name="id">
				<xsl:value-of select="str[@name='id']" />
			</xsl:attribute>
			<xsl:attribute name="objectType">
				<xsl:value-of select="str[@name='objectType']" />
			</xsl:attribute>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>