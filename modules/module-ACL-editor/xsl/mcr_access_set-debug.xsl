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
	
	<xsl:template match="/mcr_access_set-debug">
		<a href="http://141.35.23.203:8291/modules/module-ACL-editor/web/editor/editor_start_ACL_editor.xml"><b>Permissions editieren</b></a><br/>
		<table border="1">
			<tr>
				<td>ACPOOL</td>
				<td>OBJID</td>
				<td>DESCRIPTION</td>
				<td>RID</td>
			</tr>
			<xsl:apply-templates />
		</table>
	</xsl:template>
	
	<xsl:template match="mcr_access">
		<tr>
			<td><xsl:value-of select="ACPOOL"/></td>
			<td><xsl:value-of select="OBJID"/></td>
			<td><xsl:value-of select="DESCRIPTION"/></td>
			<td><xsl:value-of select="RID"/></td>
		</tr>
	</xsl:template>
</xsl:stylesheet>