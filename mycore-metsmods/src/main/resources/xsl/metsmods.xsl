<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.9 $ $Date: 2009/03/13 09:56:46 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  
  <xsl:param name="UploadID"/>
  <xsl:param name="UploadPath"/>
  <xsl:param name="language"/>
  
  <!-- - - - - variables for starting applet - - - - -->
  
  <xsl:variable name="applet_mm.mime" select="'application/x-java-applet;version=1.3.1'"/>
  <xsl:variable name="applet_mm.codebase" select="concat($WebApplicationBaseURL,'applet_metsmods')"/>
  <xsl:variable name="applet_mm.class" select="'org.mycore.frontend.metsmods.MCRMetsModsApplet.class'"/>
  <xsl:variable name="applet_mm.archives" select="'metsmods.jar,jdom-1.1.jar,mycore.jar'"/>
  <xsl:variable name="applet_mm.cache" select="'No'"/>
  <xsl:variable name="applet_mm.width" select="'600'"/>
  <xsl:variable name="applet_mm.height" select="'400'"/>
  <xsl:variable name="applet_mm.nojava" select="'In Ihrem Browser ist nicht das erforderliche Java-Plug-in installiert.'"/>
  <xsl:variable name="applet_mm.netscape.plugin" select="concat($WebApplicationBaseURL,'authoring/einrichten.xml')"/>
  <!--<xsl:variable name="applet.microsoft.classid" select="'clsid:8AD9C840-044E-11D1-B3E9-00805F499D93'" />-->
  <xsl:variable name="applet_mm.microsoft.plugin" select="concat($WebApplicationBaseURL,'plugins/download/j2re-1_4_0_01-windows-i586-i.exe')"/>
  <xsl:variable name="applet_mm.metsfile" select="concat($WebApplicationBaseURL,'servlets/MCRFileNodeServlet/',$UploadPath,'mets.xml')"/>
  
  <!-- IE and NE as one tag see http://java.sun.com/products/plugin/1.2/docs/tags.html -->
  
  <xsl:template match="metsmods">
    
    <xsl:variable name="url">
      <xsl:value-of select="concat($WebApplicationBaseURL,'servlets/MCRUploadServlet',$HttpSession,'?method=redirecturl&amp;uploadId=',$UploadID)"/>
    </xsl:variable>
    <xsl:variable name="httpSession">
      <xsl:value-of select="substring-after($JSessionID,'=')"/>
    </xsl:variable>
    
    <object width="{$applet_mm.width}" height="{$applet_mm.height}">
      
      <param name="codebase" value="{$applet_mm.codebase}"/>
      <param name="code" value="{$applet_mm.class}"/>
      <param name="cache_archive" value="{$applet_mm.archives}"/>
      <param name="cache_option" value="{$applet_mm.cache}"/>
      <param name="httpSession" value="{$httpSession}"/>
      <param name="metsfile" value="{$applet_mm.metsfile}"/>
      <param name="url" value="{$url}"/>
      <param name="ServletsBase" value="{$ServletsBaseURL}"/>
      <param name="uploadId" value="{$UploadID}"/>
      <param name="language" value="{$language}"/>
      <noembed>
        <xsl:value-of select="$applet_mm.nojava"/>
      </noembed>
      
      <comment> <!-- for netscape -->
        <xsl:element name="embed">
          <xsl:attribute name="type">
            <xsl:value-of select="$applet_mm.mime"/>
          </xsl:attribute>
          <xsl:attribute name="codebase">
            <xsl:value-of select="$applet_mm.codebase"/>
          </xsl:attribute>
          <xsl:attribute name="code">
            <xsl:value-of select="$applet_mm.class"/>
          </xsl:attribute>
          <xsl:attribute name="cache_option">
            <xsl:value-of select="$applet_mm.cache"/>
          </xsl:attribute>
          <xsl:attribute name="archive">
            <xsl:value-of select="$applet_mm.archives"/>
          </xsl:attribute>
          <xsl:attribute name="cache_archive">
            <xsl:value-of select="$applet_mm.archives"/>
          </xsl:attribute>
          <xsl:attribute name="width">
            <xsl:value-of select="$applet_mm.width"/>
          </xsl:attribute>
          <xsl:attribute name="height">
            <xsl:value-of select="$applet_mm.height"/>
          </xsl:attribute>
          <xsl:attribute name="metsfile">
            <xsl:value-of select="$applet_mm.metsfile"/>
          </xsl:attribute>
          <xsl:attribute name="httpSession">
            <xsl:value-of select="$httpSession"/>
          </xsl:attribute>
          <xsl:attribute name="ServletsBase">
            <xsl:value-of select="$ServletsBaseURL"/>
          </xsl:attribute>
          <xsl:attribute name="uploadId">
            <xsl:value-of select="$UploadID"/>
          </xsl:attribute>
          <xsl:attribute name="url">
            <xsl:value-of select="$url"/>
          </xsl:attribute>
          <xsl:attribute name="language">
            <xsl:value-of select="$language"/>
          </xsl:attribute>
          <noembed>
            <xsl:value-of select="$applet_mm.nojava"/>
          </noembed>
        </xsl:element>
      </comment>
      
    </object>
  </xsl:template>
  
</xsl:stylesheet>