<?xml version="1.0" encoding="ISO_8859-1"?>

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:editor="http://www.mycore.org/editor"
>

<!-- ========================================================================= -->

<!-- ======== delimiters for variable names ======== -->

<xsl:variable name="editor.delimiter.internal"  select="'_'" />
<xsl:variable name="editor.delimiter.root"      select="'/'" />
<xsl:variable name="editor.delimiter.element"   select="'/'" />
<xsl:variable name="editor.delimiter.attribute" select="'@'" />
<xsl:variable name="editor.delimiter.pos.start" select="'['" />
<xsl:variable name="editor.delimiter.pos.end"   select="']'" />

<!-- ======== css ======== -->

<xsl:variable name="editor.headline.style" 
  select="'color: #ffffff; background-color: #7791ac; '"
/>
<xsl:variable name="editor.font" 
  select="'font-size: 12px; font-family: Verdana, Geneva, Arial, SansSerif; line-height: 14px; '"
/>
<xsl:variable name="editor.textinput.height" 
  select="'20px'" 
/>
<xsl:variable name="editor.textinput.border"
  select="'solid 1px #817e70; padding-left: 2px'"
/>
<xsl:variable name="editor.padding"
  select="'3px'"
/>
<xsl:variable name="editor.border"
  select="'1px solid #7791ac'"
/>
<xsl:variable name="editor.background.color"
  select="'#f6f6f6'" 
/>
<xsl:variable name="editor.button.style"
  select="'color: #ffffff; font-weight: bold; background-color: #7791ac; letter-spacing: 1px; '" 
/>

<!-- ======== list indentation ======== -->

<xsl:variable name="editor.list.indent">
  <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;&amp;nbsp;</xsl:text>
</xsl:variable>

<!-- ========================================================================= -->

</xsl:stylesheet>

