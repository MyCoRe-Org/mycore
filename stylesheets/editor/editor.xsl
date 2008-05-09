<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.74 $ $Date: 2007-10-10 13:32:28 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:encoder="xalan://java.net.URLEncoder"
>

<!-- ========================================================================= -->

<xsl:include href="editor-common.xsl" />

<!-- ======== http request parameters ======== -->

<!-- ======== constants, do not change ======== -->
<xsl:variable name="editor.delimiter.internal"  select="'_'" />
<xsl:variable name="editor.delimiter.root"      select="'/'" />
<xsl:variable name="editor.delimiter.element"   select="'/'" />
<xsl:variable name="editor.delimiter.attribute" select="'@'" />
<xsl:variable name="editor.delimiter.pos.start" select="'['" />
<xsl:variable name="editor.delimiter.pos.end"   select="']'" />

<xsl:variable name="editor.list.indent">
  <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;&amp;nbsp;</xsl:text>
</xsl:variable>

<!-- ======== FCK Editor JavaScript laden ======== -->
<xsl:variable name="head.additional">
  <xsl:if test="//textarea[@wysiwygEditor='true']">
    <script type="text/javascript" src="{$WebApplicationBaseURL}fck/fckeditor.js" />
    <script type="text/javascript">
      <xsl:text>
        function startFCK( path, width, height )
        {
          var fck = new FCKeditor( path );
          fck.BasePath = '</xsl:text><xsl:value-of select="concat($WebApplicationBaseURL,'fck/')" /><xsl:text>';
          fck.Width = width;
          fck.Height = height;
          fck.ToolbarSet = 'mcr';
          fck.ReplaceTextarea();
        }
      </xsl:text>
    </script>
  </xsl:if>
</xsl:variable>

<!-- ========================================================================= -->

<!-- ======== include editor from within any xsl stylesheet ======== -->

<xsl:template name="include.editor">
  <xsl:param name="uri" /> <!-- URI of the editor definition xml -->
  <xsl:param name="ref" /> <!-- ID of the editor in the XML file at given URI -->
  <xsl:param name="validate" select="'false'" /> <!-- If true, validate against editor.xsd schema -->
  
  <xsl:variable name="url">
    <xsl:value-of select="$ServletsBaseURL" />
    <xsl:text>XMLEditor</xsl:text>
    <xsl:value-of select="$HttpSession" />
    <xsl:text>?_action=include&amp;_uri=</xsl:text>
    <xsl:value-of select="encoder:encode($uri)" />
    <xsl:text>&amp;_ref=</xsl:text>
    <xsl:value-of select="$ref" />
    <xsl:text>&amp;_validate=</xsl:text>
    <xsl:value-of select="$validate" />
    <xsl:if test="contains($RequestURL,'?')">
      <xsl:text>&amp;</xsl:text>
      <xsl:copy-of select="substring-after($RequestURL,'?')" />
    </xsl:if>
  </xsl:variable>

  <xsl:apply-templates select="document($url)/editor" />
</xsl:template>

<!-- ======== handles editor included within a static webpage ======== -->

<xsl:template match="editor">
  <!-- ======== build nested panel structure ======== -->
  <xsl:apply-templates select="components" />
</xsl:template>

<!-- ======== handle components ======== -->
<xsl:template match="components">
  <form>
    <xsl:call-template name="editor.set.form.attrib" />    

    <table cellspacing="0" class="editor">

      <!-- ======== if exists, output editor headline ======== -->
      <xsl:apply-templates select="headline" />

      <!-- ======== if validation errors exist, display message ======== -->
      <xsl:apply-templates select="ancestor::editor/failed" />

      <tr>
        <td>
        
          <!-- Workaround for browser behavior: Use a hidden submit button,
               so that when user hits the enter key, really submit the form, 
               instead of executing the [+] button of the first repeater -->
          <xsl:if test="//repeater">
            <input style="width:0px; height:0px; border-width:0px; float:left;" value="submit" type="submit" tabindex="99" />
          </xsl:if>
          
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
    <xsl:value-of select="concat($ServletsBaseURL,'XMLEditor',$HttpSession)"/>
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

  <!-- ======== charset ======== -->
  <xsl:attribute name="accept-charset">UTF-8</xsl:attribute>

  <!-- ======== target type servlet or url or xml display ======== -->
  <xsl:choose>
    <xsl:when test="../target/@type">
      <input type="hidden" name="{$editor.delimiter.internal}target-type" value="{../target/@type}" />
    </xsl:when>
    <xsl:otherwise>
      <input type="hidden" name="{$editor.delimiter.internal}target-type" value="display" />
    </xsl:otherwise>
  </xsl:choose>

  <!-- ======== target url ======== -->
  <xsl:if test="../target/@url">
    <xsl:variable name="url">
        <xsl:call-template name="UrlAddSession">
            <xsl:with-param name="url" select="../target/@url" />
        </xsl:call-template>
    </xsl:variable>
    <input type="hidden" name="{$editor.delimiter.internal}target-url" value="{$url}" />
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
  <input type="hidden" name="{$editor.delimiter.internal}action"  value="submit" />
  <input type="hidden" name="{$editor.delimiter.internal}root"    value="{@var}" />
  <input type="hidden" name="{$editor.delimiter.internal}webpage">
    <xsl:attribute name="value">
      <xsl:value-of select="substring-after($RequestURL,$WebApplicationBaseURL)" />
      <xsl:choose>
        <xsl:when test="contains($RequestURL,'?')">
          <xsl:if test="not(substring($RequestURL,string-length($RequestURL))='&amp;')">
            <xsl:text>&amp;</xsl:text>
          </xsl:if>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>?</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </input>

  <!-- ======== durchreichen der target parameter ======== -->
  <xsl:for-each select="../target-parameters/target-parameter">
    <input type="hidden" name="{@name}" value="{text()}" />
  </xsl:for-each>

  <!-- ======== Cancel URL durchreichen ======== -->
  <input type="hidden" name="_cancelURL" value="{../cancel/@url}" />
</xsl:template>

<!-- ======== headline ======== -->
<xsl:template match="headline">
  <tr> 
    <td class="editorHeadline">

      <xsl:call-template name="editor.set.anchor" />

      <xsl:apply-templates select="text | output">
        <xsl:with-param name="var" select="../@var" />
      </xsl:apply-templates>

    </td>
  </tr>
</xsl:template>

<!-- ======== validation errors exist ======== -->
<xsl:template match="failed">
  <tr>
    <td class="editorValidationMessage">

      <xsl:for-each select="ancestor::editor/validationMessage">
        <xsl:call-template name="output.label">
          <xsl:with-param name="usefont" select="'yes'" />
        </xsl:call-template>
      </xsl:for-each>
      
    </td>
  </tr>
</xsl:template>

<!-- ======== handle repeater ======== -->
<xsl:template match="repeater">
  <xsl:param name="var"      />
  <xsl:param name="pos"      />
  <xsl:param name="num.next" />

  <!-- find number of repeats of related xml input element -->
  <xsl:variable name="num">
    <xsl:choose>
      <xsl:when test="ancestor::editor/repeats/repeat[@path=$var]/@value">
        <xsl:value-of select="ancestor::editor/repeats/repeat[@path=$var]/@value"/>
      </xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="num.visible">
    <xsl:choose>
      <xsl:when test="@min and ($num &lt; @min)">
        <xsl:value-of select="@min" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$num" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <input type="hidden" name="{$editor.delimiter.internal}n-{$var}" value="{$num.visible}" />

  <table cellspacing="0">
  
    <!-- ======== iterate rows in repeater ======== -->
    <xsl:call-template name="repeater.row">
      <xsl:with-param name="var" select="$var" />
      <xsl:with-param name="pos" select="$pos" />
      <xsl:with-param name="num" select="$num" />
    </xsl:call-template>
    
  </table>
</xsl:template>

<!-- ======== handle repeater row ======== -->
<xsl:template name="repeater.row">
  <xsl:param name="row.nr" select="'1'" />
  <xsl:param name="num" />
  <xsl:param name="var" />
  <xsl:param name="pos" />

  <xsl:variable name="min">
    <xsl:choose>
      <xsl:when test="@min">
        <xsl:value-of select="@min" />
      </xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="max">
    <xsl:choose>
      <xsl:when test="@max">
        <xsl:value-of select="@max" />
      </xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <tr>
    <a name="rep{translate($var,'/@[]','____')}" />
    <xsl:if test="@pos = 'left'">
      <xsl:call-template name="repeater.pmud">
        <xsl:with-param name="var"    select="$var" />
        <xsl:with-param name="num"    select="$num" />
        <xsl:with-param name="min"    select="$min" />
        <xsl:with-param name="max"    select="$max" />
        <xsl:with-param name="row.nr" select="$row.nr" />
      </xsl:call-template>
    </xsl:if>
    <xsl:call-template name="repeated.component">
      <xsl:with-param name="var"    select="$var" />
      <xsl:with-param name="pos"    select="$pos" />
      <xsl:with-param name="row.nr" select="$row.nr" />
    </xsl:call-template>
    <xsl:if test="( string-length(@pos) = 0 ) or (@pos = 'right')">
      <xsl:call-template name="repeater.pmud">
        <xsl:with-param name="var"    select="$var" />
        <xsl:with-param name="num"    select="$num" />
        <xsl:with-param name="min"    select="$min" />
        <xsl:with-param name="max"    select="$max" />
        <xsl:with-param name="row.nr" select="$row.nr" />
      </xsl:call-template>
    </xsl:if>
  </tr>

  <!-- ======== output another repeated row ======== -->
  <xsl:if test="($row.nr &lt; $min) or ($row.nr &lt; $num)">
    <xsl:call-template name="repeater.row">
      <xsl:with-param name="row.nr" select="1 + $row.nr" />
      <xsl:with-param name="num" select="$num" />
      <xsl:with-param name="var" select="$var" />
      <xsl:with-param name="pos" select="$pos" />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!-- ======== handle repeated component ======== -->
<xsl:template name="repeated.component">
  <xsl:param name="var" />
  <xsl:param name="pos" />
  <xsl:param name="row.nr" />

  <!-- ======== build the "position number" of this repeated component ======== -->
  <xsl:variable name="pos.new">
    <xsl:value-of select="$pos" />
    <xsl:if test="$pos">
      <xsl:text>.</xsl:text>
    </xsl:if>
    <xsl:value-of select="$row.nr" />
  </xsl:variable>

  <!-- ======== build variable path for repeated component ======== -->
  <xsl:variable name="var.new">
    <xsl:value-of select="$var" />
    <xsl:if test="$row.nr &gt; 1">
      <xsl:value-of select="$editor.delimiter.pos.start" />
      <xsl:value-of select="$row.nr" />
      <xsl:value-of select="$editor.delimiter.pos.end" />
    </xsl:if>
  </xsl:variable>
 
  <td>
  
    <xsl:call-template name="cell">
      <xsl:with-param name="var"   select="$var.new" />
      <xsl:with-param name="pos"   select="$pos.new" />
      <xsl:with-param name="first" select="not(@pos='left')" />
    </xsl:call-template>
    
  </td>
</xsl:template>

<!-- ======== repeater plus minus up down buttons ======== -->
<xsl:template name="repeater.pmud">
  <xsl:param name="var" />
  <xsl:param name="num" />
  <xsl:param name="min" />
  <xsl:param name="max" />
  <xsl:param name="row.nr" />

  <td>
    <!-- ======== set align / valign ======== -->
    <xsl:call-template name="editor.set.anchor" />
  
    <table cellspacing="0">
      <tr>
  
      <td>
        <xsl:choose>
          <xsl:when test="$num &lt; $max">
            <input tabindex="999" type="image" name="{$editor.delimiter.internal}p-{$var}-{$row.nr}" src="{$WebApplicationBaseURL}images/pmud-plus.png"/>
          </xsl:when>
          <xsl:otherwise><img src="{$WebApplicationBaseURL}images/pmud-blank.png" border="0" /></xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="$num &gt; 1">
            <input tabindex="999" type="image" name="{$editor.delimiter.internal}m-{$var}-{$row.nr}" src="{$WebApplicationBaseURL}images/pmud-minus.png"/>
          </xsl:when>
          <xsl:otherwise><img src="{$WebApplicationBaseURL}images/pmud-blank.png" border="0" /></xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="($row.nr &lt; $num) or ($row.nr &lt; $min)">
            <input tabindex="999" type="image" name="{$editor.delimiter.internal}d-{$var}-{$row.nr}" src="{$WebApplicationBaseURL}images/pmud-down.png"/>
          </xsl:when>
          <xsl:otherwise><img src="{$WebApplicationBaseURL}images/pmud-blank.png" border="0" /></xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="$row.nr &gt; 1">
            <input tabindex="999" type="image" name="{$editor.delimiter.internal}u-{$var}-{$row.nr}" src="{$WebApplicationBaseURL}images/pmud-up.png"/>
          </xsl:when>
          <xsl:otherwise><img src="{$WebApplicationBaseURL}images/pmud-blank.png" border="0" /></xsl:otherwise>
        </xsl:choose>
      </td>
      
      </tr>
    </table>
  </td>
</xsl:template>

<!-- ======== handle panel ======== -->
<xsl:template match="panel">
  <xsl:param name="var"      />
  <xsl:param name="pos" select="1" />
  <xsl:param name="num.next" />

  <!-- ======== include cells of other panels by include/@ref ======== -->
  <xsl:variable name="cells" select="ancestor::components/panel[@id = current()/include/@ref]/cell|cell" />

  <table cellspacing="0">
    <xsl:if test="ancestor::editor/failed/field[@sortnr=$pos]">
      <xsl:attribute name="class">editorValidationFailed</xsl:attribute>

      <xsl:variable name="message">
        <xsl:for-each select="//condition[@id=ancestor::editor/failed/field[@sortnr=$pos]/@condition]">
          <xsl:call-template name="output.label" />
        </xsl:for-each>
      </xsl:variable>
      <tr>
        <td>
          <img border="0" align="absbottom" src="{$WebApplicationBaseURL}images/validation-error.png" alt="{$message}" title="{$message}" />
        </td>
      </tr>
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

  <!-- ======== handle panel validation conditions ======== -->
  <xsl:for-each select="ancestor::components/panel[@id = current()/include/@ref]/condition|condition">
    <input type="hidden" name="{$editor.delimiter.internal}cond-{$var}" value="{@id}" />
    <input type="hidden" name="{$editor.delimiter.internal}sortnr-{$var}" value="{$pos}" />
  </xsl:for-each>
  
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
  <xsl:variable name="num.prev" select="count($cells[(@col &lt; number($col.nr)) and (@row = $row.nr)])"/>
  
  <td colspan="{$col.span}">
    <xsl:variable name="the.cell" select="$cells[(@row=$row.nr) and (@col=$col.nr)][1]" />

    <!-- ======== if there is no cell here, add space ======== -->
    <xsl:if test="count($the.cell) = 0">
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
      <xsl:for-each select="$the.cell">
        <xsl:call-template name="cell">
          <xsl:with-param name="var"      select="$var"      />
          <xsl:with-param name="pos"      select="$pos.new"  />
          <xsl:with-param name="num.next" select="$num.next" />
          <xsl:with-param name="first"    select="$num.prev = 0" />
        </xsl:call-template>
      </xsl:for-each>

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

<!-- ======== handle cell or repeater content ======== -->
<xsl:template name="cell">
  <xsl:param name="var" />
  <xsl:param name="pos" />
  <xsl:param name="num.next" select="'0'" />
  <xsl:param name="first" />

  <!-- ======== build new variable path ======== -->
  <xsl:variable name="var.new">
    <xsl:call-template name="editor.build.new.var">
      <xsl:with-param name="var" select="$var" />
    </xsl:call-template>
  </xsl:variable>

  <!-- ======== set align / valign ======== -->
  <xsl:call-template name="editor.set.anchor" />

  <!-- ======== handle referenced or embedded component ======== -->
  <xsl:for-each select="ancestor::components/*[@id = current()/@ref]|*">

    <xsl:if test="(position() = 1) and ancestor::editor/failed/field[@sortnr=$pos] and contains('textfield textarea password file list checkbox display ', concat(name(),' '))">
      <xsl:attribute name="class">editorValidationFailed</xsl:attribute>
    </xsl:if>

    <!-- ======== handle nested component (textfield, textarea, ...) ======== -->
    <xsl:apply-templates select=".">
      <xsl:with-param name="var"      select="$var.new"  />
      <xsl:with-param name="pos"      select="$pos"      />
      <xsl:with-param name="num.next" select="$num.next" />
    </xsl:apply-templates>

    <!-- ======== show failed input validation message ======== -->
    <xsl:if test="contains('textfield textarea password file list checkbox display ', concat(name(),' '))">
      <xsl:if test="ancestor::editor/failed/field[@sortnr=$pos]">
        <xsl:variable name="message">
          <xsl:for-each select="//condition[@id=ancestor::editor/failed/field[@sortnr=$pos]/@condition]">
            <xsl:call-template name="output.label" />
          </xsl:for-each>
        </xsl:variable>
        <img border="0" align="absbottom" src="{$WebApplicationBaseURL}images/validation-error.png" alt="{$message}" title="{$message}" />
      </xsl:if>
    </xsl:if>
  
    <xsl:if test="contains('textfield textarea password file list checkbox display ', concat(name(),' '))">
      <!-- ======== hidden field for sorting the entry ======== -->
      <!-- ======== hidden field for identifying entry ======== -->
      <input type="hidden" name="{$editor.delimiter.internal}sortnr-{$var.new}" value="{$pos}" />
      <input type="hidden" name="{$editor.delimiter.internal}id@{$var.new}" value="{@id}" />
    </xsl:if>
  </xsl:for-each>

</xsl:template>

<xsl:template name="editor.set.anchor">
  <!-- ======== set td/@align ======== -->
  <xsl:attribute name="align">
	<xsl:choose>
	<xsl:when test="$CurrentLang = 'ar'">
    <xsl:choose>
      <xsl:when test="@anchor='NORTH'"    >center</xsl:when>
      <xsl:when test="@anchor='NORTHEAST'">left</xsl:when>
      <xsl:when test="@anchor='EAST'"     >left</xsl:when>
      <xsl:when test="@anchor='SOUTHEAST'">left</xsl:when>
      <xsl:when test="@anchor='SOUTH'"    >center</xsl:when>
      <xsl:when test="@anchor='SOUTHWEST'">right</xsl:when>  
      <xsl:when test="@anchor='WEST'"     >right</xsl:when>
      <xsl:when test="@anchor='NORTHWEST'">right</xsl:when>
      <xsl:when test="@anchor='CENTER'"   >center</xsl:when>   
      <xsl:otherwise>right</xsl:otherwise>
    </xsl:choose>
	</xsl:when>
	<xsl:otherwise>
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
	</xsl:otherwise>
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
  <xsl:call-template name="subst.cond">
    <xsl:with-param name="rest" select="@var" />
  </xsl:call-template>
</xsl:template>

<!--  title[@type='main'] becomes title__type__main -->
<xsl:template name="subst.cond">
  <xsl:param name="rest" />
  
  <xsl:variable name="apos">'</xsl:variable>
  
  <xsl:choose>
    <xsl:when test="contains($rest,'[@')">
      <xsl:value-of select="normalize-space(substring-before($rest,'[@'))" />
      <xsl:variable name="tmp1" select="substring-after($rest,'[@')" />
      <xsl:text>__</xsl:text>
      <xsl:value-of select="normalize-space(substring-before($tmp1,'='))" />
      <xsl:variable name="tmp2" select="substring-after($tmp1,'=')" />
      <xsl:text>__</xsl:text>
      <xsl:value-of select="normalize-space(translate(substring-before($tmp2,']'),$apos,''))" />
      <xsl:call-template name="subst.cond">
        <xsl:with-param name="rest" select="substring-after($tmp2,']')" />
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$rest" />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ========================================================================= -->

<!-- ======== helpPopup ======== -->
<xsl:template match="helpPopup">
  <xsl:variable name="url" select="concat($ServletsBaseURL,'XMLEditor',$HttpSession,'?_action=show.popup&amp;_session=',ancestor::editor/@session,'&amp;_ref=',@id)" />

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

  <xsl:choose>
    <xsl:when test="button">
      <input tabindex="9999" type="button" onClick="window.open('{$url}','help','{$properties}');">
        <xsl:attribute name="value">
          <xsl:choose>
            <xsl:when test="button[lang($CurrentLang)]">
              <xsl:value-of select="button[lang($CurrentLang)]"/>
            </xsl:when>
            <xsl:when test="button[lang($DefaultLang)]">
              <xsl:value-of select="button[lang($DefaultLang)]"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="button"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </input>
    </xsl:when>
    <xsl:otherwise>
      <input tabindex="9999" type="button" value=" ? " onClick="window.open('{$url}','help','{$properties}');" />
    </xsl:otherwise>
  </xsl:choose>
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

  <xsl:choose>
    <!-- ======== copy all elements, attributes and child elements with current xpath to hidden field ======== -->  
    <xsl:when test="@descendants='true'">
      <xsl:for-each select="ancestor::editor/input/var[ (@name = $var.new) or ( starts-with(@name,$var.new) and ( starts-with(substring-after(@name,$var.new),$editor.delimiter.element) or starts-with(substring-after(@name,$var.new),$editor.delimiter.pos.start) ) ) ]">
        <input type="hidden" name="{@name}" value="{@value}" />
        <input type="hidden" name="{$editor.delimiter.internal}sortnr-{@name}" value="{$pos.new}.{position()}" />
      </xsl:for-each>
    </xsl:when>
    <!-- ======== copy single source value to hidden field ======== -->
    <xsl:when test="ancestor::editor/input/var[@name = $var.new]">
      <input type="hidden" name="{$var.new}" value="{ancestor::editor/input/var[@name = $var.new]/@value}" />
      <input type="hidden" name="{$editor.delimiter.internal}sortnr-{$var.new}" value="{$pos.new}" />
    </xsl:when>
    <!-- ======== copy default value to hidden field ======== -->
    <xsl:when test="@default">
      <input type="hidden" name="{$var.new}" value="{@default}" />
      <input type="hidden" name="{$editor.delimiter.internal}sortnr-{$var.new}" value="{$pos.new}" />
    </xsl:when>
  </xsl:choose>

</xsl:template>

<!-- ======== label ======== -->
<xsl:template match="text">
  <xsl:call-template name="output.label">
    <xsl:with-param name="usefont" select="'yes'" />
  </xsl:call-template>
</xsl:template>

<!-- ======== display ======= -->
<xsl:template match="display">
  <xsl:param name="var" />
  
  <xsl:choose>
    <xsl:when test="ancestor::editor/input/var[@name = $var]">
      <input type="hidden" name="{$var}" value="{ancestor::editor/input/var[@name = $var]/@value}" />
      <xsl:value-of select="ancestor::editor/input/var[@name = $var]/@value" />
    </xsl:when>
    <xsl:when test="@default">
      <xsl:value-of select="@default" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:text disable-output-escaping="yes">&amp;nbsp;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>  
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
  <xsl:variable name="selected.cell" 
    select="ancestor::editor/input/var[@name=$var.new]" 
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
</xsl:template>

<!-- ======== textfield and textarea ======== -->
<xsl:template match="textfield | textarea">
  <xsl:param name="var" />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:value-of select="ancestor::editor/input/var[@name=$var]/@value" />  
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
    <input tabindex="1" type="text" size="{@width}" name="{$var}" value="{$value}">
      <xsl:copy-of select="@maxlength" />
    </input>
  </xsl:if>
  <xsl:if test="local-name() = 'textarea'">
  
    <xsl:text disable-output-escaping="yes">&lt;textarea tabindex="1" </xsl:text>
      <xsl:text>cols="</xsl:text><xsl:value-of select="@width"/><xsl:text>" </xsl:text>
      <xsl:text>rows="</xsl:text><xsl:value-of select="@height"/><xsl:text>" </xsl:text>
      <xsl:text>wrap="</xsl:text><xsl:value-of select="@wrap"/><xsl:text>" </xsl:text>
      <xsl:text>name="</xsl:text><xsl:value-of select="$var"/><xsl:text>" </xsl:text>
      <xsl:text disable-output-escaping="yes">&gt;</xsl:text>

      <xsl:value-of select="$value" disable-output-escaping="yes" />
    <xsl:text disable-output-escaping="yes">&lt;/textarea&gt;</xsl:text>
    
    <!-- ======== Use the WYSIWYG HTML Editor? ======== -->
    <xsl:if test="@wysiwygEditor='true'">
      <script type="text/javascript">
        <xsl:text>startFCK('</xsl:text>
        <xsl:value-of select="$var" />
        <xsl:text>',</xsl:text>
        <xsl:value-of select="@editorWidth" />
        <xsl:text>,</xsl:text>
        <xsl:value-of select="@editorHeight" />
        <xsl:text>);</xsl:text>
      </script>
    </xsl:if>
    
  </xsl:if>
</xsl:template>

<!-- ======== file upload ======== -->
<xsl:template match="file">
  <xsl:param name="var" />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:value-of select="ancestor::editor/input/var[@name=$var]/@value" />  
  </xsl:variable>
  
  <!-- ======== if older value exists, display controls to delete old file ======== -->
  <xsl:if test="string-length($source) != 0">
    <xsl:text>Existierende Datei auf dem Server: </xsl:text>
    <strong><xsl:value-of select="$source" /></strong>
    <br/>
    <input tabindex="1" type="checkbox" name="{$editor.delimiter.internal}delete-{$var}" value="true" />
    <xsl:text> löschen </xsl:text>
    <input type="hidden" name="{$var}" value="{$source}" />
    <xsl:text>und/oder ersetzen durch diese Datei: </xsl:text>
    <br/>
  </xsl:if>

  <input tabindex="1" type="file" size="{@width}" name="{$var}">
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
    <xsl:value-of select="ancestor::editor/input/var[@name=$var]/@value" />  
  </xsl:variable>

  <input tabindex="1" type="password" size="{@width}" value="{$source}" name="{$var}" />
</xsl:template>

<!-- ======== subselect ======== -->
<xsl:template match="subselect">
  <xsl:param name="var" />

  <xsl:variable name="label">
    <xsl:call-template name="output.label" />
  </xsl:variable>

  <input tabindex="1" type="submit" value="{$label}" name="{$editor.delimiter.internal}s-{@id}-{$var}">
    <xsl:if test="@width">
      <xsl:attribute name="style">width:<xsl:value-of select="@width" /></xsl:attribute>
    </xsl:if>
  </input>
</xsl:template>

<!-- ======== cancel ======== -->
<xsl:template match="cancelButton">

  <!-- ======== build url for cancel button ======== -->
  <xsl:variable name="url">
    <xsl:call-template name="build.editor.url">
      <xsl:with-param name="url" select="ancestor::editor/cancel/@url" />
    </xsl:call-template>
  </xsl:variable>

  <!-- ======== output cancel button ======== -->
  <xsl:call-template name="editor.button">
    <xsl:with-param name="url"   select="$url"   />
    <xsl:with-param name="width" select="@width" />
  </xsl:call-template>
</xsl:template>

<!-- ======== button ======== -->
<xsl:template match="button">
  <xsl:variable name="url">
    <xsl:call-template name="UrlAddSession">
      <xsl:with-param name="url" select="@url" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:call-template name="editor.button">
    <xsl:with-param name="url"   select="$url"   />
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

  <input tabindex="999" type="button" value="{$label}" onClick="self.location.href='{$url}'">
    <xsl:if test="$width">
      <xsl:attribute name="style">width:<xsl:value-of select="$width" /></xsl:attribute>
    </xsl:if>
  </input>
</xsl:template>

<!-- ======== submitButton ======== -->
<xsl:template match="submitButton">
  <xsl:variable name="label">
    <xsl:call-template name="output.label" />
  </xsl:variable>

  <input tabindex="1" type="submit" value="{$label}">
    <xsl:if test="@width">
      <xsl:attribute name="style">width:<xsl:value-of select="@width" /></xsl:attribute>
    </xsl:if>
  </input>
</xsl:template>

<!-- ======== list ======== -->
<xsl:template match="list">
  <xsl:param name="var" />

  <!-- When any value exists, do not use the given default value -->
  <xsl:variable name="default">
    <xsl:choose>
      <xsl:when test="(string-length(@default)=0) or (@default and ancestor::editor/input/var[(@name=$var) or starts-with(@name,concat($var,'['))])">
        <xsl:text>--DuMmY--</xsl:text>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="@default" /></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- ====== type multirow ======== -->
  <xsl:if test="@type='multirow'">
    <xsl:call-template name="editor.list">
      <xsl:with-param name="var"     select="$var"      />
      <xsl:with-param name="default" select="$default"  />
      <xsl:with-param name="rows"    select="@rows"     />
      <xsl:with-param name="multi"   select="@multiple" />
    </xsl:call-template>
  </xsl:if>
  <!-- ====== type dropdown ======== -->
  <xsl:if test="@type='dropdown'">
    <xsl:call-template name="editor.list">
      <xsl:with-param name="var"     select="$var"      />
      <xsl:with-param name="default" select="$default"  />
    </xsl:call-template>
  </xsl:if>
  <!-- ====== type radio ======== -->
  <xsl:if test="(@type='radio') or (@type='checkbox')">
    <xsl:call-template name="editor.list.radio.cb">
      <xsl:with-param name="var"     select="$var"      />
      <xsl:with-param name="default" select="$default"  />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!-- ======== handle list of radio ======== -->
<xsl:template name="editor.list.radio.cb">
  <xsl:param name="var"     />
  <xsl:param name="default" />

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

  <table cellspacing="0">
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
            <xsl:with-param name="var"     select="$var"     />
            <xsl:with-param name="default" select="$default" />
            <xsl:with-param name="item"    select="."        />
          </xsl:call-template>
        </xsl:if>
        <xsl:if test="$type='checkbox'">
          <xsl:call-template name="editor.checkbox">
            <xsl:with-param name="var"     select="$var"     />
            <xsl:with-param name="default" select="$default" />
            <xsl:with-param name="item"    select="."        />
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
  <xsl:param name="var"     />
  <xsl:param name="default" />
  <xsl:param name="item"    />

  <input tabindex="1" type="radio" name="{$var}" value="{$item/@value}">
    <xsl:choose>
      <xsl:when test="ancestor::editor/input/var[((@name=$var) or starts-with(@name,concat($var,'['))) and (@value=$item/@value)]">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
      <xsl:when test="$default = $item/@value">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
    </xsl:choose>
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
  <xsl:param name="var"     />
  <xsl:param name="default" />
  <xsl:param name="item"    />

  <input tabindex="1" type="checkbox" name="{$var}" value="{$item/@value}">
    <xsl:choose>
      <xsl:when test="ancestor::editor/input/var[((@name=$var) or starts-with(@name,concat($var,'['))) and (@value=$item/@value)]">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
      <xsl:when test="$default = $item/@value">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
    </xsl:choose>
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
    <xsl:value-of select="ancestor::editor/input/var[@name=$var]/@value" />  
  </xsl:variable>

  <input tabindex="1" type="checkbox" name="{$var}" value="{@value}">
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
        <xsl:text>padding-left: </xsl:text>
        <xsl:value-of select="@width" />
        <xsl:text>; </xsl:text>
      </xsl:if>
      <xsl:if test="@height">
        <xsl:text>padding-bottom: </xsl:text>
        <xsl:value-of select="@height" />
        <xsl:text>; </xsl:text>
      </xsl:if>
    </xsl:attribute> 
  </div>
</xsl:template>

<!-- ======== handle multirow or dropdown ======== -->
<xsl:template name="editor.list">
  <xsl:param name="var"     />
  <xsl:param name="default" />
  <xsl:param name="rows"  select="'1'"/>
  <xsl:param name="multi" select="'false'"/>

  <!-- ======== html select list ======== -->
  <select tabindex="1" name="{$var}" size="{$rows}">
    <xsl:if test="$multi = 'true'">
      <xsl:attribute name="multiple">multiple</xsl:attribute>
    </xsl:if>

    <xsl:if test="@width">
      <xsl:attribute name="style">
        <xsl:value-of select="concat('width:',@width)"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:if test="@disabled='true'">
      <xsl:attribute name="disabled">disabled</xsl:attribute>
    </xsl:if>
    
    <xsl:choose>
      <xsl:when test="$multi = 'true'">
        <xsl:apply-templates select="item" mode="editor.list">
          <xsl:with-param name="vars"    select="ancestor::editor/input/var[(@name=$var) or starts-with(@name,concat($var,'['))]" />
          <xsl:with-param name="default" select="$default" />
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="item" mode="editor.list">
          <xsl:with-param name="vars"    select="ancestor::editor/input/var[@name=$var]" />
          <xsl:with-param name="default" select="$default" />
        </xsl:apply-templates>
      </xsl:otherwise>
    </xsl:choose>
  </select>
</xsl:template>

<!-- ======== If url is relative, add WebApplicationBaseURL and make it absolute ======== -->
<xsl:template name="build.editor.url">
  <xsl:param name="url" />
  <xsl:variable name="return">
    <xsl:choose>
        <xsl:when test="starts-with($url,'http://') or starts-with($url,'https://') or starts-with($url,'file://')">
            <xsl:value-of select="$url" />
        </xsl:when>
        <xsl:when test="starts-with($url,'classification:')">
            <xsl:value-of select="$url" />
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="concat($WebApplicationBaseURL,$url)" />
        </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:call-template name="UrlAddSession">
    <xsl:with-param name="url" select="$return"/>
  </xsl:call-template>
</xsl:template>

<!-- ======== html select list option ======== -->
<xsl:template match="item" mode="editor.list">
  <xsl:param name="vars"     />
  <xsl:param name="default" />
  <xsl:param name="indent" select="''"/>

  <option value="{@value}">
  
    <xsl:choose>
      <xsl:when test="$vars[@value=current()/@value]">
        <xsl:attribute name="selected">selected</xsl:attribute>
      </xsl:when>
      <xsl:when test="$default=current()/@value">
        <xsl:attribute name="selected">selected</xsl:attribute>
      </xsl:when>
    </xsl:choose>
    
    <xsl:value-of select="$indent" disable-output-escaping="yes"/>
    <xsl:call-template name="output.label" />
  </option>

  <!-- ======== handle nested items ======== -->
  <xsl:apply-templates select="item" mode="editor.list">
    <xsl:with-param name="vars"    select="$vars"    />
    <xsl:with-param name="default" select="$default" />
    <xsl:with-param name="indent"  select="concat($editor.list.indent,$indent)" />
  </xsl:apply-templates>
</xsl:template>

<!-- ========================================================================= -->

</xsl:stylesheet>

