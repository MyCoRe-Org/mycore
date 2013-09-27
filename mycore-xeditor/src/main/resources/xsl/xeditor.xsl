<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xed="http://www.mycore.de/xeditor"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:transformer="xalan://org.mycore.frontend.xeditor.MCRXEditorTransformer"
  xmlns:includer="xalan://org.mycore.frontend.xeditor.MCRIncludeHandler"
  exclude-result-prefixes="xsl xed xalan transformer includer i18n">

  <xsl:strip-space elements="xed:*" />

  <xsl:include href="copynodes.xsl" />
  <xsl:include href="xslInclude:xeditor" />

  <xsl:param name="XEditorTransformerKey" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />

  <xsl:variable name="transformer" select="transformer:getTransformer($XEditorTransformerKey)" />
  <xsl:variable name="includer" select="includer:new()" />

  <!-- ========== <xed:form /> ========== -->

  <xsl:template match="xed:form">
    <xsl:call-template name="registerAdditionalNamespaces" />
    <form>
      <xsl:apply-templates select="@*" mode="xeditor" />
      <xsl:attribute name="action">
        <xsl:value-of select="concat($ServletsBaseURL,'XEditor')" />
      </xsl:attribute>
      <xsl:apply-templates select="node()" mode="xeditor" />
      <xsl:call-template name="passRequestParameters" />
      <xsl:call-template name="submitXPaths2CheckResubmission" />
      <input type="hidden" name="_xed_session" value="{transformer:getCombinedSessionStepID($transformer)}" />
    </form>
  </xsl:template>

  <!-- ========== register additional namespaces ========== -->
  
  <xsl:template name="registerAdditionalNamespaces">
    <xsl:for-each select="namespace::*">
      <xsl:value-of select="transformer:addNamespace($transformer,name(),.)" />
    </xsl:for-each>
  </xsl:template>

  <!-- ========== pass request parameters ========== -->

  <xsl:template name="passRequestParameters">
    <xsl:for-each select="transformer:getRequestParameters($transformer)">
      <input type="hidden" name="{@name}" value="{text()}" />
    </xsl:for-each>
  </xsl:template>

  <!-- ========== pass resubmit fields ========== -->

  <xsl:template name="submitXPaths2CheckResubmission">
    <xsl:for-each select="transformer:getXPaths2CheckResubmission($transformer)">
      <input type="hidden" name="_xed_check" value="{text()}" />
    </xsl:for-each>
  </xsl:template>

  <!-- ========== <xed:source uri="" /> ========== -->

  <xsl:template match="xed:source" mode="xeditor">
    <xsl:value-of select="transformer:readSourceXML($transformer,@uri)" />
  </xsl:template>

  <!-- ========== <xed:cancel url="" /> ========== -->

  <xsl:template match="xed:cancel" mode="xeditor">
    <xsl:value-of select="transformer:setCancelURL($transformer,@url)" />
  </xsl:template>

  <!-- ========== <xed:post-processor xsl="" /> ========== -->
  
  <xsl:template match="xed:post-processor" mode="xeditor">
    <xsl:value-of select="transformer:setPostProcessorXSL($transformer,@xsl)" />
  </xsl:template>

  <!-- ========== <xed:include uri="" ref="" static="true|false" /> ========== -->

  <xsl:template match="xed:include[@uri and @ref]" mode="xeditor">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:variable name="ref" select="transformer:replaceParameters($transformer,@ref)" />
    <xsl:apply-templates select="includer:resolve($includer,@uri,@static)/descendant::*[@id=$ref]" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@uri and not(@ref)]" mode="xeditor">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:apply-templates select="includer:resolve($includer,@uri,@static)" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@ref and not(@uri)]" mode="xeditor">
    <xsl:variable name="ref" select="transformer:replaceParameters($transformer,@ref)" />
    <xsl:apply-templates select="/*/descendant-or-self::*[@id=$ref]" mode="included" />
  </xsl:template>

  <xsl:template match="*|text()" mode="included">
    <xsl:apply-templates mode="xeditor" />
  </xsl:template>

  <!-- ========== <xed:template /> ========== -->

  <xsl:template match="xed:template" mode="xeditor" />
  <xsl:template match="xed:template" />

  <!-- ========== Text ========== -->

  <xsl:template match="@*" mode="xeditor">
    <xsl:attribute name="{name()}">
      <xsl:value-of select="transformer:replaceXPaths($transformer,.)" />
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="text()" mode="xeditor">
    <xsl:value-of select="." />
  </xsl:template>

  <!-- ========== <xed:bind xpath="" default="" name="" /> ========== -->

  <xsl:template match="xed:bind" mode="xeditor">
    <xsl:value-of select="transformer:bind($transformer,@xpath,@default,@name)" />
    <xsl:apply-templates select="*" mode="xeditor" />
    <xsl:value-of select="transformer:unbind($transformer)" />
  </xsl:template>

  <xsl:template match="@xed:*|xed:*" mode="xeditor" />

  <xsl:template match="*" mode="xeditor">
    <xsl:copy>
      <xsl:apply-templates select="." mode="add-attributes" />
      <xsl:apply-templates select="@*|text()|*" mode="xeditor" />
      <xsl:apply-templates select="." mode="add-content" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="*" mode="add-attributes" />

  <xsl:template match="*" mode="add-content" />

  <!-- ========== <input /> ========== -->

  <xsl:template match="input[contains('submit image',@type)]|button[@type='submit']" mode="add-attributes">
    <xsl:attribute name="name">
      <xsl:text>_xed_submit_</xsl:text>
      <xsl:value-of select="@xed:target" />
      <xsl:for-each select="@xed:href">
        <xsl:text>:</xsl:text>
        <xsl:value-of select="." />
      </xsl:for-each>
    </xsl:attribute>
    <xsl:call-template name="set.class.if.validation.failed" />
  </xsl:template>

  <xsl:template
    match="input[contains('text,password,hidden,file,color,date,datetime,datetime-local,email,month,number,range,search,tel,time,url,week',@type)]"
    mode="add-attributes">
    <xsl:attribute name="name">
      <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
    </xsl:attribute>
    <xsl:attribute name="value">
      <xsl:value-of select="transformer:getValue($transformer)" />
    </xsl:attribute>
    <xsl:call-template name="set.class.if.validation.failed" />
  </xsl:template>

  <xsl:template match="input[contains('checkbox,radio',@type)]" mode="add-attributes">
    <xsl:attribute name="name">
      <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
    </xsl:attribute>
    <xsl:if test="transformer:hasValue($transformer,@value)">
      <xsl:attribute name="checked">checked</xsl:attribute>
    </xsl:if>
    <xsl:call-template name="set.class.if.validation.failed" />
  </xsl:template>

  <xsl:template match="option[ancestor::select]" mode="add-attributes">
    <xsl:choose>
      <xsl:when test="@value and (string-length(@value) &gt; 0)">
        <xsl:if test="transformer:hasValue($transformer,@value)">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:when test="string-length(text()) &gt; 0">
        <xsl:if test="transformer:hasValue($transformer,text())">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="textarea|select" mode="add-attributes">
    <xsl:attribute name="name">
      <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
    </xsl:attribute>
    <xsl:call-template name="set.class.if.validation.failed" />
  </xsl:template>

  <xsl:template match="textarea" mode="add-content">
    <xsl:value-of select="transformer:getValue($transformer)" />
  </xsl:template>

  <!-- ========== <xed:repeat min="" max="" /> ========== -->

  <xsl:template match="xed:repeat" mode="xeditor">
    <xsl:variable name="xed_repeat" select="." />

    <xsl:for-each select="xalan:tokenize(transformer:repeat($transformer,@xpath,@min,@max))">
      <xsl:value-of select="transformer:bindRepeatPosition($transformer)" />
      <xsl:apply-templates select="$xed_repeat/node()" mode="xeditor" />
      <xsl:value-of select="transformer:unbind($transformer)" />
    </xsl:for-each>
    <xsl:value-of select="transformer:unbind($transformer)" />  
  </xsl:template>

  <!-- ========== <xed:controls /> ========== -->

  <xsl:template match="xed:controls" mode="xeditor">
    <xsl:variable name="pos" select="transformer:getRepeatPosition($transformer)" />
    <xsl:variable name="num" select="transformer:getNumRepeats($transformer)" />
    <xsl:variable name="max" select="transformer:getMaxRepeats($transformer)" />

    <xsl:variable name="controls">
      <xsl:if test="string-length(.) = 0">insert remove up down</xsl:if>
      <xsl:value-of select="." />
    </xsl:variable>

    <xsl:for-each select="xalan:tokenize($controls)">
      <xsl:choose>
        <xsl:when test="(. = 'append') and ($pos &lt; $num)" />
        <xsl:when test="(. = 'up') and ($pos = 1)" />
        <xsl:when test="(. = 'down') and ($pos = $num)" />
        <xsl:when test="(. = 'insert') and ($max = $num)" />
        <xsl:when test="(. = 'append') and ($max = $num)" />
        <xsl:otherwise>
          <xsl:apply-templates select="." mode="xed.control">
            <xsl:with-param name="name">
              <xsl:value-of select="concat('_xed_submit_',.,':')" />
              <xsl:choose>
                <xsl:when test="(. = 'remove') or (. = 'append') or (. = 'insert')">
                  <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
                </xsl:when>
                <xsl:when test="(. = 'up')">
                  <xsl:value-of select="transformer:getSwapParameter($transformer,$pos,$pos - 1)" />
                </xsl:when>
                <xsl:when test="(. = 'down')">
                  <xsl:value-of select="transformer:getSwapParameter($transformer,$pos,$pos + 1)" />
                </xsl:when>
              </xsl:choose>
            </xsl:with-param>
          </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <!-- ========== <xed:validate /> ========== -->

  <xsl:template match="xed:validate" mode="xeditor">
    <xsl:value-of select="transformer:addValidationRule($transformer,@*)" />
  </xsl:template>

  <!-- ========== <xed:if-validation-failed /> ========== -->

  <xsl:template match="xed:if-validation-failed" mode="xeditor">
    <xsl:if test="transformer:validationFailed($transformer)">
      <xsl:apply-templates select="node()" mode="xeditor" />
    </xsl:if>
  </xsl:template>

  <!-- ========== mark input controls where validation failed ========== -->

  <xsl:template match="input/@class|textarea/@class|select/@class" mode="xeditor">
    <xsl:attribute name="class">
      <xsl:value-of select="." />
      <xsl:if test="transformer:currentIsInvalid($transformer)">
        <xsl:text> xed-validation-failed</xsl:text>
      </xsl:if>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="set.class.if.validation.failed">
    <xsl:if test="not(@class) and transformer:currentIsInvalid($transformer)">
      <xsl:attribute name="class">
        <xsl:text>xed-validation-failed</xsl:text>
      </xsl:attribute>
    </xsl:if>
  </xsl:template>

  <!-- ========== <xed:if test="" /> ========== -->

  <xsl:template match="xed:if" mode="xeditor">
    <xsl:if test="transformer:evaluateXPath($transformer,@test)='true'">
      <xsl:apply-templates select="node()" mode="xeditor" />
    </xsl:if>
  </xsl:template>

  <!-- ========== <xed:choose> <xed:when test=""/> <xed:otherwise /> </xed:choose> ========== -->

  <xsl:template match="xed:choose" mode="xeditor">
    <xsl:choose>
      <xsl:when test="xed:when[transformer:evaluateXPath($transformer,@test)='true']">
        <xsl:apply-templates select="xed:when[transformer:evaluateXPath($transformer,@test)='true'][1]/node()" mode="xeditor" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="xed:otherwise/node()" mode="xeditor" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- ========== <xed:output i18n="" value="" /> ========== -->

  <xsl:template match="xed:output[not(@value) and not(@i18n)]" mode="xeditor">
    <xsl:value-of select="transformer:getValue($transformer)" />
  </xsl:template>

  <xsl:template match="xed:output[@value and not(@i18n)]" mode="xeditor">
    <xsl:value-of select="transformer:replaceXPathOrI18n($transformer,@value)" />
  </xsl:template>

  <xsl:template match="xed:output[@i18n and not(@value)]" mode="xeditor">
    <xsl:value-of select="i18n:translate(@i18n)" />
  </xsl:template>

  <xsl:template match="xed:output[@i18n and @value]" mode="xeditor">
    <xsl:value-of select="i18n:translate(@i18n,transformer:evaluateXPath($transformer,@value))" />
  </xsl:template>

  <!-- ========== <xed:multi-lang> <xed:lang xml:lang="" /> </xed:multi-lang> ========== -->

  <xsl:template match="xed:multi-lang" mode="xeditor">
    <xsl:choose>
      <xsl:when test="xed:lang[lang($CurrentLang)]">
        <xsl:apply-templates select="xed:lang[lang($CurrentLang)]/node()" mode="xeditor" />
      </xsl:when>
      <xsl:when test="xed:lang[lang($DefaultLang)]">
        <xsl:apply-templates select="xed:lang[lang($DefaultLang)]/node()" mode="xeditor" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="xed:lang[1]/node()" mode="xeditor" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ========== <xed:cleanup-rule xpath="" relevant-if="" ========== -->

  <xsl:template match="xed:cleanup-rule" mode="xeditor">
    <xsl:value-of select="transformer:addCleanupRule($transformer,@xpath,@relevant-if)" />
  </xsl:template>

</xsl:stylesheet>
