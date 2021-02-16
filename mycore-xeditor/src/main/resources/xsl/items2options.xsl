<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor">
  <!-- Transforms output of "classification:editorComplete:*" URIs to xeditor compatible format -->
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="MaxLengthVisible" />
  <xsl:param name="Mode" />
  <xsl:param name="allSelectable" select="'false'" />

  <xsl:variable name="editor.list.indent" select="'&#160;&#160;&#160;'" />
  <xsl:template match="items">
    <xsl:choose>
      <xsl:when test="$Mode='editor'">
        <xsl:apply-templates select="." mode="editor" />
      </xsl:when>
      <xsl:otherwise>
        <select>
          <xsl:copy-of select="@*" />
          <xsl:apply-templates />
        </select>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="item">
    <xsl:param name="indent" select="''" />

    <xsl:variable name="toolTip">
      <xsl:apply-templates select="." mode="toolTip" />
    </xsl:variable>
    
    <xsl:choose>
	    <xsl:when test="label[lang('x-group')] and not($allSelectable='true')">
		   	<optgroup title="{$toolTip}">
		   	  <xsl:attribute name="label">
		   	  	<xsl:value-of select="$indent" disable-output-escaping="yes" />
		      	<xsl:apply-templates select="." mode="label" />
		   	  </xsl:attribute>
		      <xsl:copy-of select="@*" />
		      <xsl:apply-templates select="item">
			  	<xsl:with-param name="indent" select="concat($editor.list.indent,$indent)" />
			  </xsl:apply-templates>
		    </optgroup>
	    </xsl:when>
	    <xsl:otherwise>
	        <option title="{$toolTip}">
	          <xsl:if test="label[lang('x-disable')] and not($allSelectable='true')">
		   	  	<xsl:attribute name="disabled"/>
		   	  </xsl:if>
		      <xsl:copy-of select="@*" />
		      <xsl:value-of select="$indent" disable-output-escaping="yes" />
		      <xsl:apply-templates select="." mode="label" />
		    </option>
		    <xsl:apply-templates select="item">
		      <xsl:with-param name="indent" select="concat($editor.list.indent,$indent)" />
		    </xsl:apply-templates>
	    </xsl:otherwise>
	</xsl:choose>

  </xsl:template>

  <xsl:template match="item" mode="label">
    <xsl:variable name="onDisplay">
      <xsl:choose>
        <xsl:when test="label[lang($CurrentLang)]">
          <xsl:value-of select="label[lang($CurrentLang)]" />
        </xsl:when>
        <xsl:when test="label[lang($DefaultLang)]">
          <xsl:value-of select="label[lang($DefaultLang)]" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="label[1]" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:call-template name="shorten">
      <xsl:with-param name="onDisplay" select="$onDisplay" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="item" mode="toolTip">
    <xsl:choose>
      <xsl:when test="label[lang($CurrentLang)]">
        <xsl:value-of select="label[lang($CurrentLang)]" />
      </xsl:when>
      <xsl:when test="label[lang($DefaultLang)]">
        <xsl:value-of select="label[lang($DefaultLang)]" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="items" mode="editor">
    <items>
      <xsl:apply-templates mode="editor" />
    </items>
  </xsl:template>

  <xsl:template match="item" mode="editor">
    <item value="{@value}">
      <xsl:for-each select="label">
        <xsl:apply-templates select="." mode="editor" />
      </xsl:for-each>
      <!-- ==== handle children ==== -->
      <xsl:apply-templates select="item" mode="editor" />
    </item>
  </xsl:template>

  <xsl:template match="label" mode="editor">
    <label xml:lang="{@xml:lang}">
      <xsl:call-template name="shorten">
        <xsl:with-param name="onDisplay" select="." />
      </xsl:call-template>
    </label>
  </xsl:template>

  <xsl:template name="shorten">
    <xsl:param name="onDisplay" />
    <xsl:choose>
      <xsl:when test="$MaxLengthVisible and (string-length($onDisplay) &gt; $MaxLengthVisible) ">
        <xsl:value-of select="concat(substring($onDisplay, 0, $MaxLengthVisible), ' [...]')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$onDisplay" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
