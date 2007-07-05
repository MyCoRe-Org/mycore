<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!--  MyCoRe - Module-Broadcasting 					-->
<!--  												-->
<!-- Module-Broadcasting 1.0, 04-2007  				-->
<!-- +++++++++++++++++++++++++++++++++++++			-->
<!--  												-->
<!-- Andreas Trappe 	- idea, concept, dev.		-->
<!--												-->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
	<xsl:include href="MyCoReLayout.xsl"/>
	<xsl:variable name="PageTitle" select="'Module-ACL Editor'"/>
	
	<xsl:template match="/mcr_join_table">
		<table border="1">
			<tr>
				<td>RID</td>
				<td>ACPOOL</td>
				<td>OBJID</td>
				<td>RULE</td>
				<td>DESCRIPTION</td>
			</tr>
			<xsl:apply-templates />
		</table>
	</xsl:template>
	
	<xsl:template match="mcr_table_item">
		<tr>
			<td><xsl:value-of select="RID"/></td>
			<td><xsl:value-of select="@ACPOOL"/></td>
			<td><xsl:value-of select="OBJID"/></td>
			<td><xsl:value-of select="RULE"/></td>
			<td><xsl:value-of select="DESCRIPTION"/></td>
		</tr>
	</xsl:template>
</xsl:stylesheet>