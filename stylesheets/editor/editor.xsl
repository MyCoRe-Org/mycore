<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.20 $ $Date: 2004-12-15 15:02:44 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:editor="http://www.mycore.org/editor"
>

<!-- ========================================================================= -->

<xsl:include href="editor-common.xsl" />

<!-- ======== http request parameters ======== -->
<xsl:param name="editor.source.new" select="'false'" /> <!-- if true, empty source -->

<xsl:param name="editor.source.url" /> <!-- if given, get source from this url -->
<xsl:param name="editor.source.id"  /> <!-- if given, substitute @@ID@@ in source/@url -->

<xsl:param name="editor.cancel.url" /> <!-- if given, use this url for cancel button -->
<xsl:param name="editor.cancel.id"  /> <!-- if given, substitute @@ID@@ in cancel/@url -->

<xsl:param name="StaticFilePath" /> <!-- path of static webpage including this editor -->

<!-- ========= http request parameter zum Durchreichen an target ======== -->
<xsl:param name="target.param.0" />
<xsl:param name="target.param.1" />
<xsl:param name="target.param.2" />
<xsl:param name="target.param.3" />
<xsl:param name="target.param.4" />
<xsl:param name="target.param.5" />
<xsl:param name="target.param.6" />
<xsl:param name="target.param.7" />
<xsl:param name="target.param.8" />
<xsl:param name="target.param.9" />

<!-- ========================================================================= -->

<!-- ======== handles editor ======== -->

<xsl:template match="editor">

  <xsl:variable name="uri" select="concat('webapp:', $StaticFilePath)" />
  <xsl:variable name="url" select="concat($ServletsBaseURL,'XMLEditor?_action=load.definition&amp;_uri=',$uri,'&amp;_ref=',@id,'&amp;MCRSessionID=',$MCRSessionID)" />

  <xsl:variable name="combined">
    <editor>
      <!-- ======== import editor definition ======== -->
      <xsl:variable name="imported.editor" select="document($url,.)/editor" />

      <!-- ======== remember editor session ID ======== -->
      <xsl:attribute name="session">
        <xsl:value-of select="$imported.editor/@session" />
      </xsl:attribute>

      <!-- ======== read input xml from source url ======== -->
      <xsl:for-each select="$imported.editor">
        <xsl:call-template name="editor.read.source" />
      </xsl:for-each>

      <!-- ======== copy editor definition ======== -->
      <xsl:apply-templates select="$imported.editor/*" mode="copy-editor" />
    </editor>
  </xsl:variable>

  <!-- ======== build nested panel structure ======== -->
  <xsl:apply-templates select="xalan:nodeset($combined)/editor/components" />

</xsl:template>

<!-- ======== copy imported editor definition ======== -->
<xsl:template match="*" mode="copy-editor">

  <xsl:copy>
    <xsl:for-each select="@*">
      <xsl:copy-of select="." />
    </xsl:for-each>
    <xsl:apply-templates select="node()" mode="copy-editor" />
  </xsl:copy>
</xsl:template>

<!-- ========================================================================= -->

<!-- ======== transforms xml to flat name=value pairs ======== -->

<xsl:template match="*" mode="editor.source.to.list">
  <xsl:param name="prefix" select="$editor.delimiter.root" />

  <!-- ======== local helper variables ======== -->
  <xsl:variable name="my.name" select="local-name()" />
  <xsl:variable name="my.position" 
    select="1 + count(preceding-sibling::*[local-name()=$my.name])" 
  />

  <!-- ======== build new prefix ======== -->
  <xsl:variable name="prefix.new">
    <xsl:value-of select="$prefix" />
    <xsl:if test="$prefix != $editor.delimiter.root">
      <xsl:value-of select="$editor.delimiter.element" />
    </xsl:if>
    <xsl:value-of select="local-name()" />
    <xsl:if test="$my.position &gt; 1">
      <xsl:value-of select="$editor.delimiter.pos.start" />
      <xsl:value-of select="$my.position" />
      <xsl:value-of select="$editor.delimiter.pos.end" />
    </xsl:if>
  </xsl:variable>

  <!-- ======== transform text content ======== -->
  <xsl:if test="string-length(normalize-space(text())) &gt; 0">
    <editor:source-variable name="{$prefix.new}" value="{text()}" />
  </xsl:if>

  <!-- ======== transform all attributes ======== -->
  <xsl:for-each select="./@*">
    <xsl:if test="string-length(normalize-space(.)) &gt; 0">
      <editor:source-variable name="{concat($prefix.new,$editor.delimiter.element,$editor.delimiter.attribute,local-name())}" value="{.}" />
    </xsl:if>
  </xsl:for-each>

  <!-- ======== transform all child elements ======== -->
  <xsl:for-each select="*">
    <xsl:apply-templates mode="editor.source.to.list" select=".">
      <xsl:with-param name="prefix" select="$prefix.new" />
    </xsl:apply-templates>
  </xsl:for-each>
</xsl:template>

<!-- ========================================================================= -->

<!-- ======== read source xml and transform to name=value pairs ======== -->
<xsl:template name="editor.read.source">

  <!-- ======== build url where to get the xml source from ======== -->
  <xsl:variable name="url.helper">
    <xsl:choose>
      <xsl:when test="string-length($editor.source.url) &gt; 0">
        <xsl:value-of select="$editor.source.url"/>
      </xsl:when>
      <xsl:when test="string-length($editor.source.id) &gt; 0">
        <xsl:value-of select="substring-before(source/@url,source/@token)"/>
        <xsl:value-of select="$editor.source.id"/>
        <xsl:value-of select="substring-after(source/@url,source/@token)"/>
      </xsl:when>
      <xsl:when test="string-length(source/@url) &gt; 0">
        <xsl:value-of select="source/@url" />
      </xsl:when>
    </xsl:choose>
  </xsl:variable>

  <!-- ======== if not empty new document, transform source to name=value ======== -->
  <xsl:if test="($editor.source.new != 'true') and (string-length($url.helper) &gt; 0)">
    <xsl:variable name="url">
      <xsl:call-template name="build.url">
        <xsl:with-param name="url" select="$url.helper" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:apply-templates select="document($url)/*" mode="editor.source.to.list" />
  </xsl:if>
</xsl:template>

<!-- ========================================================================= -->

<!-- ======== handle components ======== -->
<xsl:template match="components">
  <form>
    <xsl:call-template name="editor.set.form.attrib" />    

    <table border="0" cellspacing="0" cellpadding="0">

      <xsl:if test="panel[@id=current()/@root]/@lines='on'">
        <xsl:attribute name="style">
          <xsl:value-of select="concat('border-top: ',$editor.border,'; ')" />
          <xsl:value-of select="concat('border-left: ',$editor.border,'; ')" />
        </xsl:attribute>
      </xsl:if>

      <!-- ======== if exists, output editor headline ======== -->
      <xsl:apply-templates select="headline" />

      <tr>
        <td style="{concat('background-color: ',$editor.background.color,';')}">
          <!-- ======== start at the root panel ======== -->
          <xsl:apply-templates select="panel[@id=current()/@root]">
            <xsl:with-param name="var" select="@var" />
          </xsl:apply-templates>
        </td>
      </tr>

    </table>
  </form>
</xsl:template>

<!-- ======== set form attributes ======== -->
<xsl:template name="editor.set.form.attrib">    

  <!-- ======== action ======== -->
  <xsl:attribute name="action">
    <xsl:value-of select="$ServletsBaseURL" />
    <xsl:text>XMLEditor</xsl:text>
  </xsl:attribute>

  <!-- ======== method ======== -->
  <xsl:attribute name="method">
    <xsl:choose>
      <xsl:when test="descendant::file">
        <xsl:text>post</xsl:text>
      </xsl:when>
      <xsl:when test="../target/@method">
        <xsl:value-of select="../target/@method" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>post</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:attribute>

  <!-- ======== form data encoding ======== -->
  <xsl:if test="descendant::file">
    <xsl:attribute name="enctype">
      <xsl:text>multipart/form-data</xsl:text>
    </xsl:attribute>
  </xsl:if>

  <!-- ======== target type servlet or url or xml display ======== -->
  <xsl:choose>
    <xsl:when test="../target/@type">
      <input type="hidden" name="{$editor.delimiter.internal}target-type"
             value="{../target/@type}" />
    </xsl:when>
    <xsl:otherwise>
      <input type="hidden" name="{$editor.delimiter.internal}target-type" value="display" />
    </xsl:otherwise>
  </xsl:choose>

  <!-- ======== target url ======== -->
  <xsl:if test="../target/@url">
    <input type="hidden" name="{$editor.delimiter.internal}target-url" value="{../target/@url}" />
  </xsl:if>

  <!-- ======== target servlet name ======== -->
  <xsl:if test="../target/@name">
    <input type="hidden" name="{$editor.delimiter.internal}target-name" value="{../target/@name}" />
  </xsl:if>

  <!-- ======== target output format xml or name=value ======== -->
  <xsl:choose>
    <xsl:when test="../target/@format">
      <input type="hidden" name="{$editor.delimiter.internal}target-format" value="{../target/@format}" />
    </xsl:when>
    <xsl:otherwise>
      <input type="hidden" name="{$editor.delimiter.internal}target-format" value="xml" />
    </xsl:otherwise>
  </xsl:choose>

  <!-- ======== send editor session ID to servlet ======== -->
  <input type="hidden" name="{$editor.delimiter.internal}session" value="{../@session}" />
  <input type="hidden" name="{$editor.delimiter.internal}action" value="submit" />

  <!-- ======== durchreichen der XSL.target.param.X=name=value parameter ======== -->
  <xsl:call-template name="handle.target.parameter">
    <xsl:with-param name="param" select="$target.param.0" />
  </xsl:call-template>
  <xsl:call-template name="handle.target.parameter">  
    <xsl:with-param name="param" select="$target.param.1" />
  </xsl:call-template>
  <xsl:call-template name="handle.target.parameter">  
    <xsl:with-param name="param" select="$target.param.2" /> 
  </xsl:call-template>
  <xsl:call-template name="handle.target.parameter"> 
    <xsl:with-param name="param" select="$target.param.3" />
  </xsl:call-template>
  <xsl:call-template name="handle.target.parameter"> 
    <xsl:with-param name="param" select="$target.param.4" /> 
  </xsl:call-template>
  <xsl:call-template name="handle.target.parameter"> 
    <xsl:with-param name="param" select="$target.param.5" />  
  </xsl:call-template>
  <xsl:call-template name="handle.target.parameter">  
    <xsl:with-param name="param" select="$target.param.6" />
  </xsl:call-template>
  <xsl:call-template name="handle.target.parameter">
    <xsl:with-param name="param" select="$target.param.7" /> 
  </xsl:call-template>
  <xsl:call-template name="handle.target.parameter"> 
    <xsl:with-param name="param" select="$target.param.8" /> 
  </xsl:call-template>
  <xsl:call-template name="handle.target.parameter"> 
    <xsl:with-param name="param" select="$target.param.9" /> 
  </xsl:call-template>

</xsl:template>

<!-- ======== durchreichen der XSL.target.param.X=name=value parameter ======== -->
<xsl:template name="handle.target.parameter">
  <xsl:param name="param" />

  <xsl:if test="string-length(normalize-space($param)) &gt; 0">
    <input type="hidden" name="{substring-before($param,'=')}" value="{substring-after($param,'=')}" />
  </xsl:if>
</xsl:template>

<!-- ======== headline ======== -->
<xsl:template match="headline">
  <xsl:variable name="lines" select="../panel[@id=current()/../@root]/@lines"/>

  <tr> 
    <td>

      <xsl:attribute name="style">
        <xsl:if test="$lines='on'">
          <xsl:value-of select="concat('border-right: ',$editor.border,'; ')" />
          <xsl:value-of select="concat('border-bottom: ',$editor.border,'; ')" />
        </xsl:if>
        <xsl:value-of select="concat('padding: ',$editor.padding,'; ')" />
        <xsl:value-of select="concat($editor.headline.style,' ')" />
        <xsl:value-of select="concat($editor.font,' ')" />
      </xsl:attribute>

      <xsl:call-template name="editor.set.anchor" />
      <xsl:apply-templates select="text | output">
        <xsl:with-param name="var" select="../@var" />
      </xsl:apply-templates>

    </td>
  </tr>
</xsl:template>

<!-- ======== handle panel ======== -->
<xsl:template match="panel">
  <xsl:param name="var"      />
  <xsl:param name="pos"      />
  <xsl:param name="num.next" />

  <!-- ======== include cells of other panels by include/@ref ======== -->
  <xsl:variable name="cells" select="ancestor::components/panel[@id = current()/include/@ref]/cell|cell" />

  <table border="0" cellspacing="0" cellpadding="0">

    <!-- If panel is last component in parent panel, this table must be width 100% -->
    <xsl:if test="$num.next = '0'">
      <xsl:attribute name="width">100%</xsl:attribute>
    </xsl:if>

    <!-- ======== iterate rows in panel ======== -->
    <xsl:call-template name="editor.row">
      <xsl:with-param name="cells" select="$cells" />
      <xsl:with-param name="var"   select="$var"   />
      <xsl:with-param name="pos"   select="$pos"   />
    </xsl:call-template>
  </table>

  <!-- ======== handle hidden fields ======== -->
  <xsl:apply-templates select="ancestor::components/panel[@id = current()/include/@ref]/hidden|hidden">
    <xsl:with-param name="cells" select="$cells" />
    <xsl:with-param name="var"   select="$var"   />
    <xsl:with-param name="pos"   select="$pos"   />
  </xsl:apply-templates>
</xsl:template>

<!-- ======== handle row in panel ======== -->
<xsl:template name="editor.row">
  <xsl:param name="row.nr" select="'1'" />
  <xsl:param name="cells" />
  <xsl:param name="var" />
  <xsl:param name="pos" />

  <xsl:if test="count($cells[@row=$row.nr]) &gt; 0">
    <tr>
      <!-- ======== iterate through columns of this row ======== -->
      <xsl:call-template name="editor.col">
        <xsl:with-param name="row.nr" select="$row.nr" />
        <xsl:with-param name="cells"  select="$cells"  />
        <xsl:with-param name="var"    select="$var"    />
        <xsl:with-param name="pos"    select="$pos"    />
      </xsl:call-template>
    </tr>
  </xsl:if>

  <!-- ======== iterate through all rows ======== -->
  <xsl:if test="$cells[@row &gt; $row.nr]">
    <xsl:call-template name="editor.row">
      <xsl:with-param name="row.nr" select="1 + $row.nr" />
      <xsl:with-param name="cells"  select="$cells" />
      <xsl:with-param name="var"    select="$var"   />
      <xsl:with-param name="pos"    select="$pos"   />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!-- ======== handle column in row ======== -->
<xsl:template name="editor.col">
  <xsl:param name="row.nr" select="'1'" />
  <xsl:param name="col.nr" select="'1'" />
  <xsl:param name="cells" />
  <xsl:param name="var" />
  <xsl:param name="pos" />

  <!-- ======== find colspan for this cell ======== -->
  <xsl:variable name="my.col.span" select="$cells[(@row=$row.nr) and (@col=$col.nr)]/@colspan" />
  <xsl:variable name="col.span">
    <xsl:choose>
      <xsl:when test="$my.col.span">
        <xsl:value-of select="$my.col.span" />
      </xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- ======== is this the last column?  ======== -->
  <xsl:variable name="num.next" select="count($cells[@col &gt; (number($col.nr) + number($col.span) - 1)])"/>

  <td colspan="{$col.span}">
    <xsl:variable name="the.cell" select="$cells[(@row=$row.nr) and (@col=$col.nr)]" />

    <!-- ======== if there is no cell here, add space ======== -->
    <xsl:if test="count($the.cell) = 0">
      <xsl:if test="@lines='on'">
        <xsl:attribute name="style">
          <xsl:value-of select="concat('border-right: ',$editor.border,'; ')" />
          <xsl:value-of select="concat('border-bottom: ',$editor.border,'; ')" />
        </xsl:attribute>
      </xsl:if>
      <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
    </xsl:if>

    <!-- ======== if there is a cell here, handle it ======== -->
    <xsl:if test="count($the.cell) &gt; 0">

      <!-- ======== find the "position number" of this cell in panel ======== -->
      <xsl:variable name="pos.new">
        <xsl:value-of select="$pos" />
        <xsl:if test="$pos">
          <xsl:text>.</xsl:text>
        </xsl:if> 
        <xsl:choose>
          <xsl:when test="$the.cell/@sortnr">
            <xsl:value-of select="$the.cell/@sortnr"/>
          </xsl:when>
          <xsl:otherwise> 
            <xsl:value-of select="1 + count($cells[(@row &lt; $row.nr) or ( (@row = $row.nr) and (@col &lt; $col.nr) )])" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
 
      <!-- ======== set ID of this cell for later reference ======== -->
      <xsl:attribute name="id">
        <xsl:value-of select="$pos.new" />
      </xsl:attribute>

      <!-- ======== if there is a cell here, handle it ======== -->
      <xsl:apply-templates select="$the.cell">
        <xsl:with-param name="var"      select="$var"      />
        <xsl:with-param name="pos"      select="$pos.new"  />
        <xsl:with-param name="num.next" select="$num.next" />
      </xsl:apply-templates>

    </xsl:if>
  </td>

  <!-- ======== iterate through all other columns ======== -->
  <xsl:if test="$num.next &gt; 0">
    <xsl:call-template name="editor.col">
      <xsl:with-param name="row.nr" select="$row.nr" />
      <xsl:with-param name="col.nr" select="number($col.nr) + number($col.span)" />
      <xsl:with-param name="cells"  select="$cells" />
      <xsl:with-param name="var"    select="$var"   />
      <xsl:with-param name="pos"    select="$pos"   />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!-- ======== handle cell ======== -->
<xsl:template match="cell">
  <xsl:param name="var"      />
  <xsl:param name="pos"      />
  <xsl:param name="num.next" />

  <!-- ======== build new variable path ======== -->
  <xsl:variable name="var.new">
    <xsl:call-template name="editor.build.new.var">
      <xsl:with-param name="var" select="$var" />
    </xsl:call-template>
  </xsl:variable>

  <!-- ======== set align / valign ======== -->
  <xsl:call-template name="editor.set.anchor" />

  <!-- ======== set width / height ======== -->
  <xsl:copy-of select="@height" />
  <xsl:choose>
    <xsl:when test="@width">
      <xsl:copy-of select="@width"/>
    </xsl:when>
    <xsl:when test="$num.next = '0'">
      <xsl:attribute name="width">100%</xsl:attribute>
    </xsl:when>
  </xsl:choose>

  <xsl:variable name="outer.border" select="../@lines" />

  <!-- ======== handle referenced or embedded component ======== -->
  <xsl:for-each select="ancestor::components/*[@id = current()/@ref]|*">

    <!-- ======== set border style ======== -->
    <xsl:if test="local-name() = 'panel'">
      <xsl:variable name="inner.border" select="@lines" />
      <xsl:variable name="from.to" select="concat( $outer.border, ' to ', $inner.border )" /> 

      <xsl:if test="$from.to = 'on to off'">
        <xsl:attribute name="style">
          <xsl:value-of select="concat('border-right: ',$editor.border,'; ')" />
          <xsl:value-of select="concat('border-bottom: ',$editor.border,'; ')" />
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="$from.to = 'off to on'">
        <xsl:attribute name="style">
          <xsl:value-of select="concat('border-top: ',$editor.border,'; ')" />
          <xsl:value-of select="concat('border-left: ',$editor.border,'; ')" />
        </xsl:attribute>
      </xsl:if>

    </xsl:if>
    <xsl:if test="( local-name() != 'panel' ) and (position() = 1)">
      <xsl:attribute name="style">
        <xsl:if test="$outer.border='on'">
          <xsl:value-of select="concat('border-right: ',$editor.border,'; ')" />
          <xsl:value-of select="concat('border-bottom: ',$editor.border,'; ')" />
        </xsl:if>
        <xsl:value-of select="concat('padding: ',$editor.padding,'; ')" />
      </xsl:attribute>
    </xsl:if>

    <!-- ======== handle nested component (textfield, textarea, ...) ======== -->
    <xsl:apply-templates select=".">
      <xsl:with-param name="var"      select="$var.new"  />
      <xsl:with-param name="pos"      select="$pos"      />
      <xsl:with-param name="num.next" select="$num.next" />
    </xsl:apply-templates>

    <!-- ======== hidden field for sorting the entry ======== -->
    <!-- ======== hidden field for identifying entry ======== -->
    <xsl:if test="contains('textfield textarea password file list checkbox ', concat(name(),' '))">
      <input type="hidden" name="{$editor.delimiter.internal}sortnr-{$var.new}" value="{$pos}" />
      <input type="hidden" name="{$editor.delimiter.internal}id@{$var.new}" value="{@id}" />
    </xsl:if>
  </xsl:for-each>

</xsl:template>

<xsl:template name="editor.set.anchor">
  <!-- ======== set td/@align ======== -->
  <xsl:attribute name="align">
    <xsl:choose>
      <xsl:when test="@anchor='NORTH'"    >center</xsl:when>
      <xsl:when test="@anchor='NORTHEAST'">right</xsl:when>
      <xsl:when test="@anchor='EAST'"     >right</xsl:when>
      <xsl:when test="@anchor='SOUTHEAST'">right</xsl:when>
      <xsl:when test="@anchor='SOUTH'"    >center</xsl:when>
      <xsl:when test="@anchor='SOUTHWEST'">left</xsl:when>  
      <xsl:when test="@anchor='WEST'"     >left</xsl:when>
      <xsl:when test="@anchor='NORTHWEST'">left</xsl:when>
      <xsl:when test="@anchor='CENTER'"   >center</xsl:when>   
      <xsl:otherwise>left</xsl:otherwise>
    </xsl:choose>
  </xsl:attribute>

  <!-- ======== set td/@valign ======== -->
  <xsl:attribute name="valign">
    <xsl:choose>
      <xsl:when test="@anchor='NORTH'"    >top</xsl:when>
      <xsl:when test="@anchor='NORTHEAST'">top</xsl:when>
      <xsl:when test="@anchor='EAST'     ">middle</xsl:when>
      <xsl:when test="@anchor='SOUTHEAST'">bottom</xsl:when>
      <xsl:when test="@anchor='SOUTH'"    >bottom</xsl:when>
      <xsl:when test="@anchor='SOUTHWEST'">bottom</xsl:when>
      <xsl:when test="@anchor='WEST'     ">middle</xsl:when>
      <xsl:when test="@anchor='NORTHWEST'">top</xsl:when>
      <xsl:when test="@anchor='CENTER'"   >middle</xsl:when>
      <xsl:otherwise>top</xsl:otherwise>
    </xsl:choose>
  </xsl:attribute>
</xsl:template>

<!-- ======== build new variable path ======== -->
<xsl:template name="editor.build.new.var">
  <xsl:param name="var" />

  <xsl:value-of select="$var" />
  <xsl:if test="(string-length($var) &gt; 0) and (string-length(@var) &gt; 0)">
    <xsl:value-of select="$editor.delimiter.element" />
  </xsl:if>
  <xsl:value-of select="@var" />
</xsl:template>

<!-- ========================================================================= -->

<!-- ======== helpPopup ======== -->
<xsl:template match="helpPopup">
  <xsl:variable name="url" select="concat($ServletsBaseURL,'XMLEditor?_action=show.popup&amp;_session=',ancestor::editor/@session,'&amp;_ref=',@id,'&amp;MCRSessionID=',$MCRSessionID)" />

  <xsl:variable name="properties">
    <xsl:text>width=</xsl:text>
    <xsl:value-of select="@width" />
    <xsl:text>, </xsl:text>
    <xsl:text>height=</xsl:text>
    <xsl:value-of select="@height" />
    <xsl:text>, </xsl:text>
    <xsl:text>dependent=yes, location=no, menubar=no, resizable=yes, </xsl:text>
    <xsl:text>top=100, left=100, scrollbars=yes, status=no</xsl:text>
  </xsl:variable>

  <input type="button" value=" ? " onClick="window.open('{$url}','help','{$properties}');"
    style="{$editor.font} {$editor.button.style}" 
  />
</xsl:template>

<!-- ======== hidden ======== -->
<xsl:template match="hidden">
  <xsl:param name="cells" />
  <xsl:param name="var"   />
  <xsl:param name="pos"   />

  <xsl:variable name="var.new">
    <xsl:call-template name="editor.build.new.var">
      <xsl:with-param name="var" select="$var" />
    </xsl:call-template>
  </xsl:variable>
  <!-- ======== find the "position number" of this hidden field in panel ======== -->
  <xsl:variable name="pos.new">
    <xsl:if test="$pos">
      <xsl:value-of select="$pos" />
      <xsl:text>.</xsl:text>
    </xsl:if> 
    <xsl:choose>
      <xsl:when test="@sortnr">
        <xsl:value-of select="@sortnr"/>
      </xsl:when>
      <xsl:otherwise> 
        <xsl:value-of select="count($cells) + position()" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- ======== select matching nodes from source xml ======== -->  
  <xsl:variable name="selected.source.values" 
    select="ancestor::editor/editor:source-variable[ (@name = $var.new) or ( starts-with(@name,$var.new) and ( starts-with(substring-after(@name,$var.new),$editor.delimiter.element) or starts-with(substring-after(@name,$var.new),$editor.delimiter.pos.start) ) ) ]"
  />
  <xsl:choose>
    <!-- ======== if there are nodes, copy values to hidden fields ======== -->
    <xsl:when test="count($selected.source.values) &gt; 0"> 
      <xsl:for-each select="$selected.source.values">
        <input type="hidden" name="{@name}" value="{@value}" />
        <input type="hidden" name="{$editor.delimiter.internal}sortnr-{$var.new}" value="{$pos.new}" />
      </xsl:for-each>
    </xsl:when>
    <!-- ======== otherwise copy default value to hidden field ======== -->
    <xsl:otherwise>
      <input type="hidden" name="{$var.new}" value="{@default}" />
      <input type="hidden" name="{$editor.delimiter.internal}sortnr-{$var.new}" value="{$pos.new}" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ======== label ======== -->
<xsl:template match="text">
  <xsl:call-template name="output.label">
    <xsl:with-param name="usefont" select="'yes'" />
  </xsl:call-template>
</xsl:template>

<!-- ======== output ======== -->
<xsl:template match="output">
  <xsl:param name="var" />

  <xsl:variable name="var.new">
    <xsl:call-template name="editor.build.new.var">
      <xsl:with-param name="var" select="$var" />
    </xsl:call-template>
  </xsl:variable>

  <!-- ======== get the value of this field from xml source ======== -->
  <span style="{$editor.font}">
    <xsl:variable name="selected.cell" 
      select="ancestor::editor/editor:source-variable[@name=$var.new]" 
    />
    <xsl:choose>
      <xsl:when test="$selected.cell">
        <xsl:value-of select="$selected.cell/@value" disable-output-escaping="yes" />
      </xsl:when>
      <xsl:when test="@default">
        <xsl:value-of select="@default" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>  
  </span>
</xsl:template>

<!-- ======== textfield and textarea ======== -->
<xsl:template match="textfield | textarea">
  <xsl:param name="var" />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:value-of select="ancestor::editor/editor:source-variable[@name=$var]/@value" />  
  </xsl:variable>

  <!-- ======== build the value to display ======== -->
  <xsl:variable name="value">
    <xsl:choose>
      <xsl:when test="string-length($source) &gt; 0">
        <xsl:value-of select="$source" />
      </xsl:when>
      <xsl:otherwise> <!-- use default -->
        <xsl:value-of select="@default | @autofill | ./default | ./autofill" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  
  <xsl:if test="local-name() = 'textfield'">
    <input type="text" size="{@width}" name="{$var}" value="{$value}" 
      style="{$editor.font} height: {$editor.textinput.height}; border: {$editor.textinput.border};">
      <xsl:copy-of select="@maxlength" />
    </input>
  </xsl:if>
  <xsl:if test="local-name() = 'textarea'">
    <xsl:variable name="wrap" select="''" />
 
    <xsl:text disable-output-escaping="yes">&lt;textarea </xsl:text>
      <xsl:text>cols="</xsl:text><xsl:value-of select="@width"/><xsl:text>" </xsl:text>
      <xsl:text>rows="</xsl:text><xsl:value-of select="@height"/><xsl:text>" </xsl:text>
      <xsl:text>wrap="</xsl:text><xsl:value-of select="$wrap"/><xsl:text>" </xsl:text>
      <xsl:text>name="</xsl:text><xsl:value-of select="$var"/><xsl:text>" </xsl:text>
      <xsl:text>style="</xsl:text><xsl:value-of select="concat($editor.font,' border: ',$editor.textinput.border)"/><xsl:text>" </xsl:text>
      <xsl:text disable-output-escaping="yes">&gt;</xsl:text>

      <xsl:value-of select="$value" disable-output-escaping="yes" />
    <xsl:text disable-output-escaping="yes">&lt;/textarea&gt;</xsl:text>
  </xsl:if>
</xsl:template>

<!-- ======== file upload ======== -->
<xsl:template match="file">
  <xsl:param name="var" />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:value-of select="ancestor::editor/editor:source-variable[@name=$var]/@value" />  
  </xsl:variable>
  
  <!-- ======== if older value exists, display controls to delete old file ======== -->
  <xsl:if test="string-length($source) != 0">
    <span style="{$editor.font}">
      <xsl:text>Existierende Datei auf dem Server: </xsl:text>
      <b><xsl:value-of select="$source" /></b>
      <br/>
      <input type="checkbox" name="{$editor.delimiter.internal}delete-{$var}" value="true" />
      <xsl:text> l÷schen </xsl:text>
      <input type="hidden" name="{$var}" value="{$source}" />
      und/oder ersetzen durch diese Datei: <xsl:text/>
      <br/>
    </span>
  </xsl:if>

  <input type="file" size="{@width}" name="{$var}"
    style="{$editor.font} height: {$editor.textinput.height}; border: {$editor.textinput.border};">
    <xsl:if test="@accept">
      <xsl:attribute name="accept"><xsl:value-of select="@accept"/></xsl:attribute>
    </xsl:if>
    <xsl:if test="@maxlength">
      <xsl:attribute name="maxlength"><xsl:value-of select="@maxlength"/></xsl:attribute>
    </xsl:if>
  </input>
</xsl:template>

<!-- ======== password ======== -->
<xsl:template match="password">
  <xsl:param name="var" />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:value-of select="ancestor::editor/editor:source-variable[@name=$var]/@value" />  
  </xsl:variable>

  <input type="password" size="{@width}" value="{$source}" name="{$var}"
    style="{$editor.font} height: {$editor.textinput.height}; border: {$editor.textinput.border};"/>
</xsl:template>

<!-- ======== cancel ======== -->
<xsl:template match="cancelButton">

  <xsl:variable name="attr.url"   select="ancestor::editor/cancel/@url"   />
  <xsl:variable name="attr.token" select="ancestor::editor/cancel/@token" />

  <!-- ======== build url for cancel button ======== -->
  <xsl:variable name="url.helper">
    <xsl:choose>
      <xsl:when test="string-length($editor.cancel.url) &gt; 0">
        <xsl:value-of select="$editor.cancel.url"/>
      </xsl:when>

      <xsl:when test="$attr.url">
        <xsl:choose>
          <xsl:when test="contains($attr.url,$attr.token)">
            <xsl:value-of select="substring-before($attr.url,$attr.token)"/>
            <xsl:value-of select="$editor.cancel.id"/>
            <xsl:value-of select="substring-after($attr.url,$attr.token)"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$attr.url" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="url">
    <xsl:call-template name="build.url">
      <xsl:with-param name="url" select="$url.helper" />
    </xsl:call-template>
  </xsl:variable>

  <!-- ======== pass cancel url to servlet ======== -->
  <input type="hidden" name="{$editor.delimiter.internal}cancelURL" value="{$url}" />

  <!-- ======== output cancel button ======== -->
  <xsl:call-template name="editor.button">
    <xsl:with-param name="url"   select="$url"   />
    <xsl:with-param name="width" select="@width" />
  </xsl:call-template>
</xsl:template>

<!-- ======== button ======== -->
<xsl:template match="button">
  <xsl:call-template name="editor.button">
    <xsl:with-param name="url"   select="@url"   />
    <xsl:with-param name="width" select="@width" />
  </xsl:call-template>
</xsl:template>

<!-- ======== output any button ======== -->
<xsl:template name="editor.button">
  <xsl:param name="url"   />
  <xsl:param name="width" />

  <xsl:variable name="label">
    <xsl:call-template name="output.label" />
  </xsl:variable>

  <input type="button" value="{$label}" onClick="self.location.href='{$url}'">
    <xsl:attribute name="style">
      <xsl:value-of select="concat($editor.font,' ',$editor.button.style)"/>
      <xsl:if test="$width">
        <xsl:value-of select="concat(' width: ',$width,';')"/>
      </xsl:if>
    </xsl:attribute>
  </input>
</xsl:template>

<!-- ======== submitButton ======== -->
<xsl:template match="submitButton">
  <xsl:variable name="label">
    <xsl:call-template name="output.label" />
  </xsl:variable>

  <input type="submit" value="{$label}">
    <xsl:attribute name="style">
      <xsl:value-of select="concat($editor.font,' ',$editor.button.style)"/>
      <xsl:if test="@width">
        <xsl:value-of select="concat(' width: ',@width,';')"/>
      </xsl:if>
    </xsl:attribute>
  </input>
</xsl:template>

<!-- ======== list ======== -->
<xsl:template match="list">
  <xsl:param name="var" />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:value-of select="ancestor::editor/editor:source-variable[@name=$var]/@value" />  
  </xsl:variable>

  <!-- ======== build the value to display ======== -->
  <xsl:variable name="value">
    <xsl:choose>
      <xsl:when test="string-length($source) &gt; 0">
        <xsl:value-of select="$source" />
      </xsl:when>
      <xsl:otherwise> <!-- use default -->
        <xsl:value-of select="@default" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- ====== type multirow ======== -->
  <xsl:if test="@type='multirow'">
    <xsl:call-template name="editor.list">
      <xsl:with-param name="var"   select="$var"      />
      <xsl:with-param name="value" select="$value"    />
      <xsl:with-param name="rows"  select="@rows"     />
      <xsl:with-param name="multi" select="@multiple" />
    </xsl:call-template>
  </xsl:if>
  <!-- ====== type dropdown ======== -->
  <xsl:if test="@type='dropdown'">
    <xsl:call-template name="editor.list">
      <xsl:with-param name="var"   select="$var"      />
      <xsl:with-param name="value" select="$value"    />
    </xsl:call-template>
  </xsl:if>
  <!-- ====== type radio ======== -->
  <xsl:if test="(@type='radio') or (@type='checkbox')">
    <xsl:call-template name="editor.list.radio.cb">
      <xsl:with-param name="var"   select="$var"      />
      <xsl:with-param name="value" select="$value"    />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!-- ======== handle list of radio ======== -->
<xsl:template name="editor.list.radio.cb">
  <xsl:param name="var"   />
  <xsl:param name="value" />

  <xsl:variable name="last" select="count(item)-1" />

  <xsl:variable name="cols">
    <xsl:choose>
       <xsl:when test="@cols"><xsl:value-of select="@cols"/></xsl:when>
       <xsl:when test="@rows"><xsl:value-of select="(($last - ($last mod @rows)) div @rows)+1"/></xsl:when>
       <xsl:otherwise><xsl:value-of select="count(item)"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="rows">
    <xsl:choose>
       <xsl:when test="@rows"><xsl:value-of select="@rows"/></xsl:when>
       <xsl:otherwise><xsl:value-of select="(($last - ($last mod $cols)) div $cols)+1"/></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="type" select="@type" />

  <table border="0" cellpadding="0" cellspacing="0">
    <xsl:for-each select="item">

      <xsl:variable name="pxy" select="position() - 1" />

      <xsl:variable name="col" select="$pxy mod $cols" />

      <!-- ======== start a new row? ======== -->
      <xsl:if test="$col = 0">
        <xsl:if test="position() &gt; 1">
          <xsl:text disable-output-escaping="yes">&lt;/tr&gt;</xsl:text>
        </xsl:if>
        <xsl:text disable-output-escaping="yes">&lt;tr&gt;</xsl:text>
      </xsl:if>

      <td>
        <xsl:if test="$type='radio'">
          <xsl:call-template name="editor.radio">
            <xsl:with-param name="var"   select="$var"   />
            <xsl:with-param name="value" select="$value" />
            <xsl:with-param name="item"  select="."      />
          </xsl:call-template>
        </xsl:if>
        <xsl:if test="$type='checkbox'">
          <xsl:call-template name="editor.checkbox">
            <xsl:with-param name="var"   select="$var"   />
            <xsl:with-param name="value" select="$value" />
            <xsl:with-param name="item"  select="."      />
          </xsl:call-template>
        </xsl:if>
      </td>

      <!-- ======== end last row? ======== -->
      <xsl:if test="position() = last()">
        <xsl:text disable-output-escaping="yes">&lt;/tr&gt;</xsl:text>
      </xsl:if>

    </xsl:for-each>
  </table>
</xsl:template>

<!-- ======== output radio button ======== -->
<xsl:template name="editor.radio">
  <xsl:param name="var"   />
  <xsl:param name="value" />
  <xsl:param name="item"  />

  <input type="radio" name="{$var}" value="{$item/@value}">
    <xsl:if test="$item/@value = $value">
      <xsl:attribute name="checked">checked</xsl:attribute>
    </xsl:if>
  </input>
  <xsl:for-each select="$item">
    <xsl:call-template name="output.label">
      <xsl:with-param name="usefont" select="'yes'" />
    </xsl:call-template>
  </xsl:for-each>
  <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
</xsl:template>

<!-- ======== output checkbox ======== -->
<xsl:template name="editor.checkbox">
  <xsl:param name="var"   />
  <xsl:param name="value" />
  <xsl:param name="item"  />

  <input type="checkbox" name="{$var}" value="{$item/@value}">
    <xsl:if test="$item/@value = $value">
      <xsl:attribute name="checked">checked</xsl:attribute>
    </xsl:if>
  </input>
  <xsl:for-each select="$item">                                                                                                                                                  
    <xsl:call-template name="output.label">
      <xsl:with-param name="usefont" select="'yes'" />
    </xsl:call-template>
  </xsl:for-each>
  <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
</xsl:template>

<!-- ======== checkbox ======== -->
<xsl:template match="checkbox">
  <xsl:param name="var"   />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:value-of select="ancestor::editor/editor:source-variable[@name=$var]/@value" />  
  </xsl:variable>

  <input type="checkbox" name="{$var}" value="{@value}">
    <xsl:choose>
      <xsl:when test="@value = $source">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
      <xsl:when test="@checked = 'true'">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
    </xsl:choose>
  </input>

  <xsl:call-template name="output.label">
    <xsl:with-param name="usefont" select="'yes'" />
  </xsl:call-template>
</xsl:template>

<!-- ======== space ======== -->
<xsl:template match="space">
  <div>
    <xsl:attribute name="style">
      <xsl:text>margin: 0px; </xsl:text>
      <xsl:if test="@width">
        <xsl:text>width: </xsl:text>
        <xsl:value-of select="@width" />
        <xsl:text>; </xsl:text>
      </xsl:if>
      <xsl:if test="@height">
        <xsl:text>height: </xsl:text>
        <xsl:value-of select="@height" />
        <xsl:text>; </xsl:text>
      </xsl:if>
    </xsl:attribute> 
  </div>
</xsl:template>

<!-- ======== handle multirow or dropdown ======== -->
<xsl:template name="editor.list">
  <xsl:param name="var"   />
  <xsl:param name="value" />
  <xsl:param name="rows"  select="'1'"/>
  <xsl:param name="multi" select="'false'"/>

  <!-- ======== html select list ======== -->
  <select name="{$var}" size="{$rows}">
    <xsl:if test="$multi = 'true'">
      <xsl:attribute name="multiple">multiple</xsl:attribute>
    </xsl:if>

    <xsl:attribute name="style">
      <xsl:value-of select="concat($editor.font.select,' border: ',$editor.textinput.border,'; ')"/>
      <xsl:if test="@width">
        <xsl:value-of select="concat('width: ',@width,';')"/>
      </xsl:if>
    </xsl:attribute>

    <xsl:apply-templates select="item" mode="editor.list">
      <xsl:with-param name="value" select="$value" />
    </xsl:apply-templates>
  </select>
</xsl:template>

<!-- ======== If url is relative, add WebApplicationBaseURL and make it absolute ======== -->
<xsl:template name="build.url">
  <xsl:param name="url" />
  
  <xsl:choose>
    <xsl:when test="starts-with($url,'http://') or starts-with($url,'https://') or starts-with($url,'file://')">
      <xsl:value-of select="$url" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="concat($WebApplicationBaseURL,$url)" />
    </xsl:otherwise>
  </xsl:choose>

  <!-- append MCRSessionID if not already exists in URL -->
  <xsl:if test="not(contains($url, 'MCRSessionID='))">
    <xsl:choose>
      <xsl:when test="contains($url,'?')"> <!-- there are other http get style parameters in url -->
        <xsl:value-of select="'&amp;'" />  <!-- append new parameter -->
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="'?'" />      <!-- this is the only parameter -->
      </xsl:otherwise>
    </xsl:choose>
    <xsl:value-of select="concat('MCRSessionID=',$MCRSessionID)" />
  </xsl:if>
</xsl:template>

<!-- ======== html select list option ======== -->
<xsl:template match="item" mode="editor.list">
  <xsl:param name="value"  />
  <xsl:param name="indent" select="''"/>

  <option value="{@value}">
    <xsl:if test="@value = $value">
      <xsl:attribute name="selected">selected</xsl:attribute>
    </xsl:if>
    <xsl:value-of select="$indent" disable-output-escaping="yes"/>
    <xsl:call-template name="output.label" />
  </option>

  <!-- ======== handle nested items ======== -->
  <xsl:apply-templates select="item" mode="editor.list">
    <xsl:with-param name="value"  select="$value" />
    <xsl:with-param name="indent" select="concat($editor.list.indent,$indent)" />
  </xsl:apply-templates>
</xsl:template>


<!-- ========================================================================= -->

</xsl:stylesheet>

