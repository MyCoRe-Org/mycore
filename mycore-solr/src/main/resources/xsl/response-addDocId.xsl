<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<!-- 
		This stylesheet adds the document-id and the objectType to the <doc> element
		<code>
			<doc>
				<str name="id">DocPortal_document_00410901</str>
				<str name="objectType">document</str>
				...
			</doc>
		</code>
		transforms to
		<code>
			<doc id="DocPortal_document_00410901" objectType="document">
				<str name="id">DocPortal_document_00410901</str>
				<str name="objectType">document</str>
				...
			</doc>
		</code>
	 -->
  <xsl:template match="response">
    <xsl:copy>
      <xsl:copy-of select="@*|node()[not(name()='result') and not(name()='response')]" />
      <xsl:apply-templates select="result|response" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="result">
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates select="doc" />
    </xsl:copy>
  </xsl:template>

  <!-- Adds the mycore document id and type to the <doc> tag -->
  <xsl:template match="doc">
    <xsl:copy>
      <xsl:attribute name="id">
		<xsl:value-of select="str[@name='id']" />
      </xsl:attribute>
      <xsl:attribute name="objectType">
		<xsl:value-of select="str[@name='objectType']" />
      </xsl:attribute>
      <xsl:copy-of select="@*|node()" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>