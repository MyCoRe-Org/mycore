<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:mcruser="http://www.mycore.de/components/mcruser" exclude-result-prefixes="xsl i18n mcruser"
>

  <xsl:include href="copynodes.xsl" />

  <xsl:param name="MCR.user2.Layout.inputSize" />
  <xsl:param name="MCR.user2.Layout.inputWidth" />

  <xsl:variable name="grid-width" select="12" />
  <xsl:variable name="input-size">
    <xsl:choose>
      <xsl:when test="string-length($MCR.user2.Layout.inputSize) &gt; 0">
        <xsl:value-of select="$MCR.user2.Layout.inputSize" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>md</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="input-width">
    <xsl:choose>
      <xsl:when test="string-length($MCR.user2.Layout.inputWidth) &gt; 0">
        <xsl:value-of select="$MCR.user2.Layout.inputWidth" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="9" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="label-width" select="$grid-width - $input-width" />

  <xsl:template match="mcruser:template[contains('textInput|passwordInput|selectInput|checkboxList|radioList|textArea', @name)]">
    <xed:bind xpath="{@xpath}">
      <xsl:if test="string-length(@default) &gt; 0">
        <xsl:attribute name="default">
          <xsl:value-of select="@default" />
        </xsl:attribute>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="contains('textInput|passwordInput|selectInput', @name) and (@inline = 'true')">
          <xsl:apply-templates select="." mode="inline" />
        </xsl:when>
        <xsl:otherwise>
          <div>
            <xsl:variable name="rclass"> 
              <xsl:if test="@required = 'true'"> required </xsl:if>
            </xsl:variable>
            <xsl:attribute name="class">form-group {$xed-validation-marker} <xsl:value-of select="$rclass"/> </xsl:attribute>
            <xsl:apply-templates select="." mode="formline" />
          </div>
        </xsl:otherwise>
      </xsl:choose>
    </xed:bind>
  </xsl:template>

  <xsl:template match="mcruser:template[@name='submitButton']">
    <xsl:variable name="target">
      <xsl:choose>
        <xsl:when test="@target">
          <xsl:value-of select="@target" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'debug'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <button type="submit" xed:target="{$target}" class="btn btn-primary btn-{$input-size}">
      <xsl:if test="string-length(@href) &gt; 0">
        <xsl:attribute name="xed:href">
          <xsl:value-of select="@href" />
        </xsl:attribute>
      </xsl:if>
      <xsl:copy-of select="@id" />
      <xed:output i18n="{@i18n}" />
    </button>
  </xsl:template>

  <xsl:template match="mcruser:template[@name='cancelButton']">
    <button type="submit" xed:target="cancel" class="btn btn-default btn-{$input-size}">
      <xed:output i18n="{@i18n}" />
    </button>
  </xsl:template>

  <!-- MODE=formline -->

  <xsl:template match="mcruser:template[contains('textInput|passwordInput|selectInput|checkboxList|radioList|textArea', @name)]" mode="formline">
    <xsl:if test="string-length(@i18n) &gt; 0">
      <xsl:apply-templates select="." mode="label" />
    </xsl:if>
    <div>
      <xsl:attribute name="class">
        <xsl:if test="string-length(@i18n) = 0">
          <xsl:value-of select="concat('col-md-offset-',$label-width, ' ')" />
        </xsl:if>
        <xsl:if test="@tooltip">
          <xsl:value-of select="'input-group '" />
        </xsl:if>
        <xsl:value-of select="concat('col-md-',$input-width, ' ')" />
        <xsl:value-of select="'{$xed-validation-marker}'" />
      </xsl:attribute>

      <xsl:apply-templates select="." mode="widget" />
      <xsl:apply-templates select="." mode="inputTooltip" />
      <xsl:apply-templates select="." mode="validation" />
    </div>
  </xsl:template>

  <!-- MODE=inline -->

  <xsl:template match="mcruser:template[contains('textInput|passwordInput|selectInput', @name)]" mode="inline">
    <xsl:variable name="colsize">
      <xsl:choose>
        <xsl:when test="string-length(@colsize) &gt; 0">
          <xsl:value-of select="@colsize" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$input-size" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="colwidth">
      <xsl:choose>
        <xsl:when test="string(number(@colwidth)) != 'NaN'">
          <xsl:value-of select="@colwidth" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$input-width" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <div>
      <xsl:attribute name="class">
        <xsl:if test="@tooltip">
          <xsl:value-of select="'input-group '" />
        </xsl:if>
        <xsl:value-of select="concat('col-',$colsize,'-',$colwidth,' ')" />
        <xsl:value-of select="'{$xed-validation-marker}'" />
      </xsl:attribute>

      <xsl:apply-templates select="." mode="widget" />
      <xsl:apply-templates select="." mode="inputTooltip" />
      <xsl:apply-templates select="." mode="validation" />
    </div>
  </xsl:template>

  <!-- MODE=validation -->

  <xsl:template match="mcruser:template[contains('textInput|passwordInput|selectInput|checkboxList|radioList|textArea', @name)]" mode="validation">
    <xsl:if test="@required = 'true' or @validate = 'true'">
      <xed:if test="contains($xed-validation-marker, 'has-error')">
        <span class="glyphicon glyphicon-warning-sign form-control-feedback" data-toggle="tooltip" data-placement="top" title="{concat('{i18n:', @i18n.error, '}')}"></span>
      </xed:if>
      <xed:validate display="local" required="{@required}">
        <xsl:copy-of select="@*[contains('matches|test|format|type', name())]" />
        <xed:output i18n="{@i18n.error}" />
      </xed:validate>
    </xsl:if>
  </xsl:template>

  <!-- MODE=widget -->

  <xsl:template match="mcruser:template[@name='textInput']" mode="widget">
    <input type="text" class="form-control input-{$input-size} {@class}" id="{@id}">
      <xsl:if test="string-length(@placeholder) &gt; 0">
        <xsl:attribute name="placeholder">
        <xsl:value-of select="concat('{i18n:', @placeholder, '}')" />
      </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="." mode="inputOptions" />
    </input>
  </xsl:template>

  <xsl:template match="mcruser:template[@name='passwordInput']" mode="widget">
    <input type="password" class="form-control input-{$input-size} {@class}" id="{@id}">
      <xsl:if test="string-length(@placeholder) &gt; 0">
        <xsl:attribute name="placeholder">
        <xsl:value-of select="concat('{i18n:', @placeholder, '}')" />
      </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="." mode="inputOptions" />
    </input>
  </xsl:template>

  <xsl:template match="mcruser:template[@name='selectInput']" mode="widget">
    <select class="form-control input-{$input-size} {@class}" id="{@id}">
      <xsl:apply-templates select="." mode="inputOptions" />
      <option value="">
        <xed:multi-lang>
          <xed:lang xml:lang="de">(bitte wählen)</xed:lang>
          <xed:lang xml:lang="en">(please select)</xed:lang>
        </xed:multi-lang>
      </option>
      <xed:include uri="{@uri}" />
    </select>
  </xsl:template>

  <xsl:template match="mcruser:template[@name='textArea']" mode="widget">
    <textarea class="form-control input-{$input-size} {@class}" id="{@id}">
      <xsl:attribute name="rows">
        <xsl:choose>
          <xsl:when test="@rows">
            <xsl:value-of select="@rows" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>3</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:if test="string-length(@placeholder) &gt; 0">
        <xsl:attribute name="placeholder">
        <xsl:value-of select="concat('{i18n:',@placeholder,'}')" />
      </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="." mode="inputOptions" />
    </textarea>
  </xsl:template>

  <xsl:template match="mcruser:template[@name='checkboxList' or @name='radioList']" mode="widget">
    <xsl:apply-templates select="option" mode="optionList">
      <xsl:with-param name="inputType">
        <xsl:if test="@name='radioList'">
          <xsl:text>radio</xsl:text>
        </xsl:if>
        <xsl:if test="@name='checkboxList'">
          <xsl:text>checkbox</xsl:text>
        </xsl:if>
      </xsl:with-param>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="option" mode="optionList">
    <xsl:param name="inputType" select="'checkbox'" />

    <div>
      <xsl:attribute name="class">
        <xsl:value-of select="$inputType" />
        <xsl:if test="../@inline = 'true'">
          <xsl:value-of select="concat(' ', $inputType, '-inline')" />
        </xsl:if>
      </xsl:attribute>
      <label>
        <input type="{$inputType}" id="{../@id}" value="{@value}">
          <xsl:apply-templates select="." mode="inputOptions" />
        </input>
        <xsl:if test="string-length(@i18n) &gt; 0">
          <xed:output i18n="{@i18n}" />
        </xsl:if>
      </label>
    </div>
  </xsl:template>

  <xsl:template match="mcruser:template" mode="inputOptions">
    <xsl:if test="string(number(@maxlength)) != 'NaN'">
      <xsl:attribute name="maxlength">
        <xsl:value-of select="@maxlength" />
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="@disabled = 'true'">
      <xsl:attribute name="disabled">
        <xsl:text>disabled</xsl:text>
      </xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mcruser:template" mode="label">
    <label for="{@id}" class="col-md-{$label-width} control-label">
      <xed:output i18n="{@i18n}" />
    </label>
  </xsl:template>

  <xsl:template match="mcruser:template" mode="inputTooltip">
    <xsl:if test="@tooltip">
      <span class="input-group-addon" data-toggle="tooltip">
        <xsl:attribute name="title">
              <xsl:value-of select="concat('{i18n:',@tooltip,'}')" />
            </xsl:attribute>
        <i class="icon-info-sign"></i>
      </span>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>