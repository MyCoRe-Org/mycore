<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet
	version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
		<xsl:output
			method="xml"
			encoding="UTF-8"/>

		<xsl:template match="/">
			<metadata>
				<oai_dc:dc
					xmlns:xlink="http://www.w3.org/1999/xlink"
					xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
					xmlns:dc="http://purl.org/dc/elements/1.1/"
					xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd"
					xmlns:xml="http://www.w3.org/XML/1998/namespace">
						<xsl:apply-templates select="//metadata"/>
				</oai_dc:dc>
			</metadata>
		</xsl:template>

		<xsl:template match="metadata"
			xmlns:xlink="http://www.w3.org/1999/xlink"
			xmlns:dc="http://purl.org/dc/elements/1.1/">
				<xsl:for-each select="*/*">
					<xsl:choose>
						<xsl:when test="../keyword">
							<dc:subject>
								<xsl:call-template name="lang"/>
								<xsl:value-of select="."/>
							</dc:subject>
						</xsl:when>
						<xsl:when test="@xlink:title | @categid | text()">
							<xsl:element name="dc:{name()}">
								<xsl:call-template name="lang"/>
									<xsl:choose>
										<xsl:when test="@xlink:title">
											<xsl:call-template name="persons"/>
										</xsl:when>
										<xsl:when test="@categid">
											<xsl:value-of select="@categid"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="."/>
										</xsl:otherwise>
									</xsl:choose>
							</xsl:element>
						</xsl:when>
					</xsl:choose>
				</xsl:for-each>
		</xsl:template>

	<xsl:template name="persons"
		xmlns:xlink="http://www.w3.org/1999/xlink">
			<xsl:for-each select="../*">
				<xsl:value-of select="@xlink:title"/>
			</xsl:for-each>
	</xsl:template>

	<xsl:template name="lang">
		<xsl:for-each select="@xml:lang">
			<xsl:copy/>
		</xsl:for-each>
	</xsl:template>

</xsl:stylesheet>