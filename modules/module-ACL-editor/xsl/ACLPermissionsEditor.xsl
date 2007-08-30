<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
    <xsl:include href="MyCoReLayout.xsl" />
    <xsl:include href="editor.xsl" />
    <xsl:variable name="PageTitle" select="'Module-ACL Editor'" />

    <xsl:template match="/ACLPermissionsEditor">
        <xsl:call-template name="include.editor">
            <xsl:with-param name="uri" select="'webapp:modules/module-ACL-editor/web/editor/editor-ACL_Filter.xml'" />
            <xsl:with-param name="ref" select="'ACL-Permission_Filter'" />
        </xsl:call-template>
        <xsl:call-template name="include.editor">
            <xsl:with-param name="uri" select="'webapp:modules/module-ACL-editor/web/editor/editor-ACL_Permissions.xml'" />
            <xsl:with-param name="ref" select="'ACL-PermissionsEditor'" />
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>