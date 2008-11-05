<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.74 $ $Date: 2007-10-10 13:32:28 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:encoder="xalan://java.net.URLEncoder"
  exclude-result-prefixes="xsl xalan encoder i18n"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
>

<!-- ========================================================================= -->

<xsl:include href="editor-common.xsl" />

<xsl:key name="vars"       match="editor/input/var" use="@name" />
<xsl:key name="repeats"    match="editor/repeats/repeat" use="@path" />
<xsl:key name="failed"     match="editor/failed/field" use="@sortnr" />
<xsl:key name="conditions" match="condition[@id]" use="@id" />

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

<!-- ======== handles editor ======== -->

<xsl:template match="editor">
  <form action="{$ServletsBaseURL}XMLEditor{$HttpSession}" accept-charset="UTF-8">
    <xsl:call-template name="editor.set.css" />
    <xsl:call-template name="editor.set.form.attrib" />    
    <table cellspacing="0">
      <xsl:call-template name="editor.set.css" />

      <!-- ======== if exists, output editor headline ======== -->
      <xsl:apply-templates select="components/headline" />

      <!-- ======== if validation errors exist, display message ======== -->
      <xsl:apply-templates select="failed" />

      <!-- Workaround for browser behavior: Use a hidden submit button,
      so that when user hits the enter key, really submit the form, 
      instead of executing the [+] button of the first repeater -->
      <input style="width:0px; height:0px; border-width:0px; float:left;" value="submit" type="submit" tabindex="99" />

      <!-- ======== start at the root panel ======== -->
      <tr>
        <td>
          <xsl:apply-templates select="components/panel">
            <xsl:with-param name="var" select="components/@var" />
          </xsl:apply-templates>
        </td>
      </tr>
    </table>
  </form>
</xsl:template>

<!-- ======== set form attributes ======== -->
<xsl:template name="editor.set.form.attrib">    

  <!-- ======== method ======== -->
  <xsl:attribute name="method">
    <xsl:choose>
      <xsl:when test="descendant::file">
        <xsl:text>post</xsl:text>
      </xsl:when>
      <xsl:when test="target/@method">
        <xsl:value-of select="target/@method" />
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
    <xsl:when test="target/@type">
      <input type="hidden" name="_target-type" value="{target/@type}" />
    </xsl:when>
    <xsl:otherwise>
      <input type="hidden" name="_target-type" value="display" />
    </xsl:otherwise>
  </xsl:choose>

  <!-- ======== target url ======== -->
  <xsl:if test="target/@url">
    <xsl:variable name="url">
        <xsl:call-template name="UrlAddSession">
            <xsl:with-param name="url" select="target/@url" />
        </xsl:call-template>
    </xsl:variable>
    <input type="hidden" name="_target-url" value="{$url}" />
  </xsl:if>

  <!-- ======== target servlet name ======== -->
  <xsl:if test="target/@name">
    <input type="hidden" name="_target-name" value="{target/@name}" />
  </xsl:if>

  <!-- ======== target output format xml or name=value ======== -->
  <xsl:choose>
    <xsl:when test="target/@format">
      <input type="hidden" name="_target-format" value="{target/@format}" />
    </xsl:when>
    <xsl:otherwise>
      <input type="hidden" name="_target-format" value="xml" />
    </xsl:otherwise>
  </xsl:choose>

  <!-- ======== send editor session ID to servlet ======== -->
  <input type="hidden" name="_session" value="{@session}" />
  <input type="hidden" name="_action"  value="submit" />
  <input type="hidden" name="_root"    value="{@var}" />
  <input type="hidden" name="_webpage">
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
  <xsl:for-each select="target-parameters/target-parameter">
    <input type="hidden" name="{@name}" value="{text()}" />
  </xsl:for-each>

  <!-- ======== Cancel URL durchreichen ======== -->
  <input type="hidden" name="_cancelURL" value="{cancel/@url}" />
</xsl:template>

<!-- ======== headline ======== -->
<xsl:template match="headline">
  <tr> 
    <td>
      <xsl:call-template name="editor.set.css" />
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
        <xsl:call-template name="output.label" />
      </xsl:for-each>
    </td>
  </tr>
</xsl:template>

<!-- ======== handle repeater ======== -->

<xsl:variable name="nodes.tmp"> <!-- This determines max repeats for repeater -->
  <a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/>
  <a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/>
  <a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/>
  <a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/>
  <a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/><a/>
</xsl:variable>
<xsl:variable name="nodes" select="xalan:nodeset($nodes.tmp)" />

<xsl:template match="repeater">
  <xsl:param name="var" />
  <xsl:param name="pos" />

  <!-- find number of repeats of related xml input element -->
  <xsl:variable name="num">
    <xsl:choose>
      <xsl:when test="key('repeats',$var)">
        <xsl:value-of select="key('repeats',$var)/@value"/>
      </xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="num.visible">
    <xsl:choose>
      <xsl:when test="number($num) &lt; number(@min)">
        <xsl:value-of select="@min" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$num" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <input type="hidden" name="_n-{$var}" value="{$num.visible}" />

  <xsl:variable name="rep" select="." />

  <table>
    <xsl:call-template name="editor.set.css" />
  
    <xsl:for-each select="$nodes/a[(position() &lt;= number($rep/@min)) or (position() &lt;= number($num))]">
      <tr>
        <xsl:if test="$rep/@pos = 'left'">
          <xsl:call-template name="repeater.pmud">
            <xsl:with-param name="var" select="$var" />
            <xsl:with-param name="num" select="$num" />
            <xsl:with-param name="rep" select="$rep" />
          </xsl:call-template>
        </xsl:if>
        <xsl:call-template name="repeated.component">
          <xsl:with-param name="var"    select="$var" />
          <xsl:with-param name="pos"    select="$pos" />
          <xsl:with-param name="rep"    select="$rep" />
        </xsl:call-template>
        <xsl:if test="( string-length($rep/@pos) = 0 ) or ($rep/@pos = 'right')">
          <xsl:call-template name="repeater.pmud">
            <xsl:with-param name="var" select="$var" />
            <xsl:with-param name="num" select="$num" />
            <xsl:with-param name="rep" select="$rep" />
          </xsl:call-template>
        </xsl:if>
      </tr>
    </xsl:for-each>
  </table>
  
</xsl:template>

<!-- ======== handle repeated component ======== -->
<xsl:template name="repeated.component">
  <xsl:param name="var" />
  <xsl:param name="pos" />
  <xsl:param name="rep" />

  <!-- ======== build the "position number" of this repeated component ======== -->
  <xsl:variable name="pos.new">
    <xsl:value-of select="$pos" />
    <xsl:if test="$pos">
      <xsl:text>.</xsl:text>
    </xsl:if>
    <xsl:value-of select="position()" />
  </xsl:variable>

  <!-- ======== build variable path for repeated component ======== -->
  <xsl:variable name="var.new">
    <xsl:value-of select="$var" />
    <xsl:if test="position() &gt; 1">
      <xsl:text>[</xsl:text>
      <xsl:value-of select="position()" />
      <xsl:text>]</xsl:text>
    </xsl:if>
  </xsl:variable>
 
  <td>
    <a class="editorAnchor" name="rep{translate($var.new,'/@[]','____')}" />
    <xsl:for-each select="$rep">  
      <xsl:call-template name="cell">
        <xsl:with-param name="var"   select="$var.new" />
        <xsl:with-param name="pos"   select="$pos.new" />
      </xsl:call-template>
    </xsl:for-each>
  </td>
</xsl:template>

<!-- ======== repeater plus minus up down buttons ======== -->
<xsl:template name="repeater.pmud">
  <xsl:param name="var" />
  <xsl:param name="num" />
  <xsl:param name="rep" />

  <td class="editorPMUD">
    <xsl:if test="number($num) &lt; number($rep/@max)">
      <input tabindex="999" type="image" name="_p-{$var}-{position()}" src="{$WebApplicationBaseURL}images/pmud-plus.png"/>
    </xsl:if>
  </td>
  <td class="editorPMUD">
    <xsl:if test="number($num) &gt; 1">
      <input tabindex="999" type="image" name="_m-{$var}-{position()}" src="{$WebApplicationBaseURL}images/pmud-minus.png"/>
    </xsl:if>
  </td>
  <xsl:choose>
    <xsl:when test="$rep/@arrows='false'" />
    <xsl:otherwise>
      <td class="editorPMUD">
        <xsl:if test="(position() &lt; number($num)) or (position() &lt; number($rep/@min))">
          <input tabindex="999" type="image" name="_d-{$var}-{position()}" src="{$WebApplicationBaseURL}images/pmud-down.png"/>
        </xsl:if>
      </td>
      <td class="editorPMUD">
        <xsl:if test="position() &gt; 1">
          <input tabindex="999" type="image" name="_u-{$var}-{position()}" src="{$WebApplicationBaseURL}images/pmud-up.png"/>
        </xsl:if>
      </td>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ======== handle panel ======== -->
<xsl:template match="panel">
  <xsl:param name="var" />
  <xsl:param name="pos" select="1" />

  <xsl:variable name="combined" select="*" />
  <xsl:variable name="combined.cellpos.tmp">
    <xsl:for-each select="$combined[name()='cell']">
      <xsl:sort select="@row" data-type="number" />
      <xsl:sort select="@col" data-type="number" />
      <cell row="{@row}" col="{@col}" />
    </xsl:for-each>
  </xsl:variable>
  <xsl:variable name="combined.cellpos" select="xalan:nodeset($combined.cellpos.tmp)" />

  <table>
    <xsl:call-template name="editor.set.css" />
    <xsl:if test="key('failed',$pos)">
      <xsl:attribute name="class">editorValidationFailed</xsl:attribute>

      <xsl:variable name="message">
        <xsl:for-each select="key('conditions',key('failed',$pos)/@condition)">
          <xsl:call-template name="output.label" />
        </xsl:for-each>
      </xsl:variable>
      <tr>
        <td>
          <img border="0" align="absbottom" src="{$WebApplicationBaseURL}images/validation-error.png" alt="{$message}" title="{$message}" />
        </td>
      </tr>
    </xsl:if>
    
    <xsl:variable name="rows.tmp">
      <xsl:for-each select="$combined.cellpos/cell">
        <xsl:if test="not(preceding-sibling::cell/@row = current()/@row)">
          <row nr="{@row}" />
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable name="rows" select="xalan:nodeset($rows.tmp)" />

    <xsl:variable name="cols.tmp">
      <xsl:for-each select="$combined.cellpos/cell">
        <xsl:sort select="@col" data-type="number" />
        <xsl:if test="not(preceding-sibling::cell/@col = current()/@col)">
          <col nr="{@col}" />
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable name="cols" select="xalan:nodeset($cols.tmp)" />

    <xsl:for-each select="$rows/row">
      <xsl:variable name="row" select="@nr" />
      <tr>
        <xsl:for-each select="$cols/col">
          <xsl:variable name="col" select="@nr" />
          <xsl:variable name="cell" select="$combined[(@row=$row) and (@col=$col)]" />
          
          <xsl:choose>
            <xsl:when test="count($cell) &gt; 0">
              <!-- There is a cell defined for this row and col -->
              <xsl:for-each select="$cell">
              <td>
                <xsl:copy-of select="@colspan" />
                <xsl:call-template name="cell">
                  <xsl:with-param name="var" select="$var" />
                  <xsl:with-param name="pos">
                    <xsl:value-of select="$pos" />
                    <xsl:text>.</xsl:text>
                    <xsl:choose>
                      <xsl:when test="@sortnr">
                        <xsl:value-of select="@sortnr"/>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:for-each select="$combined.cellpos/cell">
                          <xsl:if test="(@row=$row) and (@col=$col)">
                            <xsl:value-of select="position()" />
                          </xsl:if>
                        </xsl:for-each> 
                      </xsl:otherwise>
                    </xsl:choose>
                  </xsl:with-param>
                </xsl:call-template>
              </td>
              </xsl:for-each>
            </xsl:when>
            <xsl:when test="$combined[ (@row=$row) and (number(@col) &lt; number($col)) and (number(@col)+number(@colspan) &gt;= number($col))]">
              <!-- There is a cell in this row before this col with a colspan spanning over this col, nothing to do -->
            </xsl:when>
            <xsl:otherwise>
              <!-- We have to insert an empty td cell here -->
              <td/>
            </xsl:otherwise>
          </xsl:choose>          
        </xsl:for-each>
      </tr>
    </xsl:for-each>
  </table>

  <!-- ======== handle hidden fields ======== -->
  <xsl:apply-templates select="$combined[name()='hidden']">
    <xsl:with-param name="numcells" select="count($combined.cellpos/cell)" />
    <xsl:with-param name="var" select="$var" />
    <xsl:with-param name="pos" select="$pos" />
  </xsl:apply-templates>

  <!-- ======== handle panel validation conditions ======== -->
  <xsl:for-each select="$combined[name()='condition']">
    <input type="hidden" name="_cond-{$var}" value="{@id}" />
    <input type="hidden" name="_sortnr-{$var}" value="{$pos}" />
  </xsl:for-each>
  
</xsl:template>

<!-- ======== handle cell or repeater content ======== -->
<xsl:template name="cell">
  <xsl:param name="var" />
  <xsl:param name="pos" />

  <!-- ======== build new variable path ======== -->
  <xsl:variable name="var.new">
    <xsl:call-template name="editor.build.new.var">
      <xsl:with-param name="var" select="$var" />
    </xsl:call-template>
  </xsl:variable>

  <!-- ======== set align / valign / css ======== -->
  <xsl:call-template name="editor.set.anchor" />
  <xsl:call-template name="editor.set.css" />

  <!-- ======== handle embedded component ======== -->
  <xsl:for-each select="*">

    <xsl:if test="(position() = 1) and key('failed',$pos) and contains('textfield textarea password file list checkbox display ', concat(name(),' '))">
      <xsl:attribute name="class">editorValidationFailed</xsl:attribute>
    </xsl:if>

    <!-- ======== handle nested component (textfield, textarea, ...) ======== -->
    <xsl:apply-templates select=".">
      <xsl:with-param name="var" select="$var.new" />
      <xsl:with-param name="pos" select="$pos"     />
    </xsl:apply-templates>

    <!-- ======== show failed input validation message ======== -->
    <xsl:if test="contains('textfield textarea password file list checkbox display ', concat(name(),' '))">
      <xsl:if test="key('failed',$pos)">
        <xsl:variable name="message">
          <xsl:for-each select="key('conditions',key('failed',$pos)/@condition)">
            <xsl:call-template name="output.label" />
          </xsl:for-each>
        </xsl:variable>
        <img border="0" align="absbottom" src="{$WebApplicationBaseURL}images/validation-error.png" alt="{$message}" title="{$message}" />
      </xsl:if>
    </xsl:if>
  
    <xsl:if test="contains('textfield textarea password file list checkbox display bdo ', concat(name(),' '))">
      <!-- ======== hidden field for sorting the entry ======== -->
      <!-- ======== hidden field for identifying entry ======== -->
      <input type="hidden" name="_sortnr-{$var.new}" value="{$pos}" />
      <input type="hidden" name="_id@{$var.new}" value="{@id}" />
    </xsl:if>
  </xsl:for-each>

</xsl:template>

<!-- ======== set CSS class|style|id attributes ======== -->
<xsl:template name="editor.set.css">
  <xsl:copy-of select="@class" />
  <xsl:copy-of select="@style" />
  
  <xsl:if test="@cssid">
    <xsl:attribute name="id">
      <xsl:value-of select="@cssid" />
    </xsl:attribute>
  </xsl:if>
</xsl:template>

<xsl:template name="editor.set.anchor">
  <!-- ======== set td/@align ======== -->
  <xsl:choose>
    <xsl:when test="$CurrentLang = 'ar'">
      <xsl:choose>
        <xsl:when test="contains(@anchor,'WEST')">
          <xsl:attribute name="align">right</xsl:attribute>
        </xsl:when>
        <xsl:when test="contains(@anchor,'EAST')" />
        <xsl:when test="string-length(@anchor) &gt; 0">
          <xsl:attribute name="align">center</xsl:attribute>
        </xsl:when>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="contains(@anchor,'EAST')">
          <xsl:attribute name="align">right</xsl:attribute>
        </xsl:when>
        <xsl:when test="contains(@anchor,'WEST')" />
        <xsl:when test="string-length(@anchor) &gt; 0">
          <xsl:attribute name="align">center</xsl:attribute>
        </xsl:when>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
  
  <!-- ======== set td/@valign ======== -->
  <xsl:choose>
    <xsl:when test="contains(@anchor,'NORTH')">
      <xsl:attribute name="valign">top</xsl:attribute>
    </xsl:when>
    <xsl:when test="contains(@anchor,'SOUTH')">
      <xsl:attribute name="valign">bottom</xsl:attribute>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<!-- ======== build new variable path ======== -->
<xsl:template name="editor.build.new.var">
  <xsl:param name="var" />

  <xsl:value-of select="$var" />
  <xsl:if test="(string-length($var) &gt; 0) and (string-length(@var) &gt; 0)">
    <xsl:text>/</xsl:text>
  </xsl:if>
  <xsl:value-of select="@var" />
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

  <input tabindex="9999" type="button" onClick="window.open('{$url}','help','{$properties}');">
    <xsl:call-template name="editor.set.css" />
    <xsl:attribute name="value">
      <xsl:choose>
        <xsl:when test="button[lang($CurrentLang)]">
          <xsl:value-of select="button[lang($CurrentLang)]"/>
        </xsl:when>
        <xsl:when test="button[lang($DefaultLang)]">
          <xsl:value-of select="button[lang($DefaultLang)]"/>
        </xsl:when>
        <xsl:when test="button">
          <xsl:value-of select="button"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="i18n:translate('editor.helpButton')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </input>
  
</xsl:template>

<!-- ======== hidden ======== -->
<xsl:template match="hidden">
  <xsl:param name="numcells" select="'0'" />
  <xsl:param name="var" />
  <xsl:param name="pos" />

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
        <xsl:value-of select="number($numcells) + position()" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  
  <xsl:choose>
    <!-- ======== copy all elements, attributes and child elements with current xpath to hidden field ======== -->  
    <xsl:when test="@descendants='true'">
      <xsl:for-each select="ancestor::editor/input/var[ (@name = $var.new) or ( starts-with(@name,$var.new) and ( starts-with(substring-after(@name,$var.new),'/') or starts-with(substring-after(@name,$var.new),'[') ) ) ]">
        <input type="hidden" name="{@name}" value="{@value}" />
        <input type="hidden" name="_sortnr-{@name}" value="{$pos.new}.{position()}" />
      </xsl:for-each>
    </xsl:when>
    <!-- ======== copy single source value to hidden field ======== -->
    <xsl:when test="key('vars',$var.new)">
      <input type="hidden" name="{$var.new}" value="{key('vars',$var.new)/@value}" />
      <input type="hidden" name="_sortnr-{$var.new}" value="{$pos.new}" />
    </xsl:when>
    <!-- ======== copy default value to hidden field ======== -->
    <xsl:when test="@default">
      <input type="hidden" name="{$var.new}" value="{@default}" />
      <input type="hidden" name="_sortnr-{$var.new}" value="{$pos.new}" />
    </xsl:when>
  </xsl:choose>

</xsl:template>

<!-- ======== label text ======== -->
<xsl:template match="text">
  <label class="editorText">
    <xsl:call-template name="output.label" />
  </label>
</xsl:template>

<!-- ======== display ======= -->
<xsl:template match="display">
  <xsl:param name="var" />
  
  <span class="editorOutput">
    <xsl:choose>
      <xsl:when test="key('vars',$var)">
        <input type="hidden" name="{$var}" value="{key('vars',$var)/@value}" />
        <xsl:value-of select="key('vars',$var)/@value" />
      </xsl:when>
      <xsl:when test="@default">
        <xsl:value-of select="@default" />
      </xsl:when>
    </xsl:choose>
  </span>  
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
  <xsl:variable name="selected.cell" select="key('vars',$var.new)" />
  
  <span class="editorOutput">
    <xsl:choose>
      <xsl:when test="$selected.cell">
        <xsl:value-of select="$selected.cell/@value" disable-output-escaping="yes" />
      </xsl:when>
      <xsl:when test="@default">
        <xsl:value-of select="@default" />
      </xsl:when>
    </xsl:choose>
  </span>  
</xsl:template>

<!-- ======== textfield and textarea ======== -->
<xsl:template match="textfield | textarea">
  <xsl:param name="var" />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:if test="key('vars',$var)">
      <xsl:value-of select="key('vars',$var)/@value" />  
    </xsl:if>
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
      <xsl:call-template name="editor.set.css" />
    </input>
  </xsl:if>
  <xsl:if test="local-name() = 'textarea'">
  
    <xsl:text disable-output-escaping="yes">&lt;textarea tabindex="1" </xsl:text>
      <xsl:text>cols="</xsl:text><xsl:value-of select="@width"/><xsl:text>" </xsl:text>
      <xsl:text>rows="</xsl:text><xsl:value-of select="@height"/><xsl:text>" </xsl:text>
      <xsl:text>wrap="</xsl:text><xsl:value-of select="@wrap"/><xsl:text>" </xsl:text>
      <xsl:text>name="</xsl:text><xsl:value-of select="$var"/><xsl:text>" </xsl:text>
      <xsl:text>class="</xsl:text><xsl:value-of select="@class"/><xsl:text>" </xsl:text>
      <xsl:if test="@style">
        <xsl:text>style="</xsl:text><xsl:value-of select="@style"/><xsl:text>" </xsl:text>
      </xsl:if>
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
    <xsl:if test="key('vars',$var)">
      <xsl:value-of select="key('vars',$var)/@value" />  
    </xsl:if>
  </xsl:variable>
  
  <!-- ======== if older value exists, display controls to delete old file ======== -->
  <xsl:if test="string-length($source) != 0">
    <label class="editorLabel">
      <xsl:value-of select="i18n:translate('editor.fileExists')" />:<xsl:text />
    </label>
    <span class="editorOutput"><xsl:value-of select="$source" /></span>
    <br/>
    <input type="hidden" name="{$var}" value="{$source}" />
    <input id="del-{$var}" tabindex="1" type="checkbox" class="editorCheckbox" name="_delete-{$var}" value="true" />
    <label class="editorLabel" for="del-{$var}">
      <xsl:value-of select="i18n:translate('editor.fileDelete')" />:<xsl:text />
    </label>
    <br/>
  </xsl:if>

  <input tabindex="1" type="file" size="{@width}" name="{$var}">
    <xsl:copy-of select="@accept|@maxlength" />
    <xsl:call-template name="editor.set.css" />
  </input>
</xsl:template>

<!-- ======== password ======== -->
<xsl:template match="password">
  <xsl:param name="var" />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:if test="key('vars',$var)">
      <xsl:value-of select="key('vars',$var)/@value" />  
    </xsl:if>
  </xsl:variable>

  <input tabindex="1" type="password" size="{@width}" value="{$source}" name="{$var}">
    <xsl:call-template name="editor.set.css" />
  </input>
</xsl:template>

<!-- ======== subselect ======== -->
<xsl:template match="subselect">
  <xsl:param name="var" />
  <input tabindex="1" type="submit" name="_s-{@id}-{$var}">
    <xsl:call-template name="editor.set.css" />
    <xsl:attribute name="value">
      <xsl:call-template name="output.label" />
    </xsl:attribute>
  </input>
</xsl:template>

<!-- ======== cancel ======== -->
<xsl:template match="cancelButton">
  <xsl:variable name="url">
    <xsl:call-template name="build.editor.url">
      <xsl:with-param name="url" select="ancestor::editor/cancel/@url" />
    </xsl:call-template>
  </xsl:variable>
  <xsl:call-template name="editor.button">
    <xsl:with-param name="url" select="$url" />
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
    <xsl:with-param name="url" select="$url" />
  </xsl:call-template>
</xsl:template>

<!-- ======== output any button ======== -->
<xsl:template name="editor.button">
  <xsl:param name="url"   />
  <input tabindex="999" type="button" onClick="self.location.href='{$url}'">
    <xsl:call-template name="editor.set.css" />
    <xsl:attribute name="value">
      <xsl:call-template name="output.label" />
    </xsl:attribute>
  </input>
</xsl:template>

<!-- ======== submitButton ======== -->
<xsl:template match="submitButton">
  <input tabindex="1" type="submit">
    <xsl:call-template name="editor.set.css" />
    <xsl:attribute name="value">
      <xsl:call-template name="output.label" />
    </xsl:attribute>
  </input>
</xsl:template>

<!-- ======== dropdown list ======== -->
<xsl:template match="list[@type='dropdown']">
  <xsl:param name="var" />

  <select tabindex="1" name="{$var}" size="1">
    <xsl:if test="@disabled='true'">
      <xsl:attribute name="disabled">disabled</xsl:attribute>
    </xsl:if>
    <xsl:call-template name="editor.set.css" />
   
    <xsl:apply-templates select="item">
      <xsl:with-param name="vars"    select="key('vars',$var)" />
      <xsl:with-param name="default" select="@default" />
    </xsl:apply-templates>
  </select>
</xsl:template>

<!-- ======== multirow list ======== -->
<xsl:template match="list[@type='multirow']">
  <xsl:param name="var" />

  <select tabindex="1" name="{$var}" size="{@rows}" multiple="multiple">
    <xsl:call-template name="editor.set.css" />
    
    <xsl:apply-templates select="item">
      <xsl:with-param name="vars"    select="ancestor::editor/input/var[(@name=$var) or starts-with(@name,concat($var,'['))]" />
      <xsl:with-param name="default" select="@default" />
    </xsl:apply-templates>
  </select>
</xsl:template>

<!-- ======== list of radio or checkbox ======== -->
<xsl:template match="list[(@type='radio') or (@type='checkbox')]">
  <xsl:param name="var" />
  <xsl:variable name="default" select="@default" />

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

  <table>
    <xsl:call-template name="editor.set.css" />

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
          </xsl:call-template>
        </xsl:if>
        <xsl:if test="$type='checkbox'">
          <xsl:call-template name="editor.checkbox">
            <xsl:with-param name="var"     select="$var"     />
            <xsl:with-param name="default" select="$default" />
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

  <input id="radio-{$var}" tabindex="1" type="radio" name="{$var}" value="{@value}">
    <xsl:choose>
      <xsl:when test="ancestor::editor/input/var[((@name=$var) or starts-with(@name,concat($var,'['))) and (@value=current()/@value)]">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
      <xsl:when test="ancestor::editor/input/var[(@name=$var) or starts-with(@name,concat($var,'['))]">
        <!-- do not use default value(s) if there is any value given in source data -->
      </xsl:when>
      <xsl:when test="(@value = $default) or (@checked = 'true')">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
    </xsl:choose>
    <xsl:for-each select="parent::list">
      <xsl:call-template name="editor.set.css" />
    </xsl:for-each>    
  </input>
  <label for="radio-{$var}">
    <xsl:for-each select="parent::list">
      <xsl:call-template name="editor.set.css" />
    </xsl:for-each>    
    <xsl:call-template name="output.label" />
  </label>
</xsl:template>

<!-- ======== output checkbox ======== -->
<xsl:template name="editor.checkbox">
  <xsl:param name="var"     />
  <xsl:param name="default" />

  <input id="check-{$var}" tabindex="1" type="checkbox" name="{$var}" value="{@value}">
    <xsl:choose>
      <xsl:when test="ancestor::editor/input/var[((@name=$var) or starts-with(@name,concat($var,'['))) and (@value=current()/@value)]">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
      <xsl:when test="ancestor::editor/input/var[(@name=$var) or starts-with(@name,concat($var,'['))]">
        <!-- do not use default value(s) if there is any value given in source data -->
      </xsl:when>
      <xsl:when test="(@value = $default) or (@checked = 'true')">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
    </xsl:choose>
    <xsl:for-each select="parent::list">
      <xsl:call-template name="editor.set.css" />
    </xsl:for-each>    
  </input>
  <label for="check-{$var}">
    <xsl:for-each select="parent::list">
      <xsl:call-template name="editor.set.css" />
    </xsl:for-each>    
    <xsl:call-template name="output.label" />
  </label>
</xsl:template>

<!-- ======== checkbox ======== -->
<xsl:template match="checkbox">
  <xsl:param name="var"   />

  <!-- ======== get the value of this field from xml source ======== -->
  <xsl:variable name="source">
    <xsl:if test="key('vars',$var)">
      <xsl:value-of select="key('vars',$var)/@value" />  
    </xsl:if>
  </xsl:variable>

  <input id="check-{$var}" tabindex="1" type="checkbox" name="{$var}" value="{@value}">
    <xsl:choose>
      <xsl:when test="@value = $source">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
      <xsl:when test="@checked = 'true'">
        <xsl:attribute name="checked">checked</xsl:attribute>
      </xsl:when>
    </xsl:choose>
    <xsl:call-template name="editor.set.css" />
  </input>
  <label for="check-{$var}">
    <xsl:call-template name="editor.set.css" />
    <xsl:call-template name="output.label" />
  </label>
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

<xsl:variable name="editor.list.indent">
  <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;&amp;nbsp;</xsl:text>
</xsl:variable>

<xsl:template match="item">
  <xsl:param name="vars"    />
  <xsl:param name="default" />
  <xsl:param name="indent" select="''"/>

  <option value="{@value}">
  
    <xsl:choose>
      <xsl:when test="$vars[@value=current()/@value]">
        <xsl:attribute name="selected">selected</xsl:attribute>
      </xsl:when>
      <xsl:when test="count($vars) &gt; 0">
        <!-- do not use default value(s) if there is any value given in source data -->
      </xsl:when>
      <xsl:when test="(@value = $default) or (@checked = 'true')">
        <xsl:attribute name="selected">selected</xsl:attribute>
      </xsl:when>
    </xsl:choose>
    
    <xsl:value-of select="$indent" disable-output-escaping="yes"/>
    <xsl:call-template name="output.label" />
  </option>

  <!-- ======== handle nested items ======== -->
  
  <xsl:apply-templates select="item">
    <xsl:with-param name="vars"    select="$vars"    />
    <xsl:with-param name="default" select="$default" />
    <xsl:with-param name="indent"  select="concat($editor.list.indent,$indent)" />
  </xsl:apply-templates>
</xsl:template>

<!-- ======== bidirectional overwrite ======== -->

<xsl:template match="bdo">
  <xsl:param name="var" />

  <bdo>
    <xsl:copy-of select="@*" />
    <xsl:apply-templates select="*">
      <xsl:with-param name="var" select="$var" />
    </xsl:apply-templates>
  </bdo>
</xsl:template>

<!-- ========================================================================= -->

</xsl:stylesheet>
