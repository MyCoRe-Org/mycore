<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor"
  xmlns:xalan="http://xml.apache.org/xalan" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:transformer="xalan://org.mycore.frontend.xeditor.MCRXEditorTransformer" xmlns:includer="xalan://org.mycore.frontend.xeditor.MCRIncludeHandler"
  exclude-result-prefixes="xsl xed xalan transformer includer i18n">

  <xsl:strip-space elements="xed:*" />

  <xsl:include href="copynodes.xsl" />
  <xsl:include href="xslInclude:xeditor" />

  <xsl:param name="XEditorTransformerKey" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="HttpSession" />

  <xsl:variable name="transformer" select="transformer:getTransformer($XEditorTransformerKey)" />
  <xsl:variable name="includer" select="includer:new()" />

  <!-- ========== <xed:form /> output-only ========== -->

  <xsl:template match="xed:form[@method='output']">
    <xsl:call-template name="registerAdditionalNamespaces" />
    <xsl:apply-templates select="@*|node()" mode="xeditor" />
  </xsl:template>

  <!-- ========== <xed:form /> ========== -->

  <xsl:template match="xed:form">
    <xsl:call-template name="registerAdditionalNamespaces" />
    <form>
      <xsl:apply-templates select="@*" mode="xeditor" />
      <xsl:attribute name="action">
        <xsl:value-of select="concat($ServletsBaseURL,'XEditor',$HttpSession)" />
      </xsl:attribute>
      
      <!-- method="post" is default, may be overwritten by xed:form/@method -->
      <xsl:if test="not(@method)"> 
        <xsl:attribute name="method">
          <xsl:text>post</xsl:text>
        </xsl:attribute>
      </xsl:if>
      
      <xsl:apply-templates select="node()" mode="xeditor" />
      <xsl:call-template name="passAdditionalParameters" />
    </form>
  </xsl:template>

  <!-- ========== register additional namespaces ========== -->

  <xsl:template name="registerAdditionalNamespaces">
    <xsl:for-each select="namespace::*">
      <xsl:value-of select="transformer:addNamespace($transformer,name(),.)" />
    </xsl:for-each>
  </xsl:template>

  <!-- ========== pass request parameters ========== -->

  <xsl:template name="passAdditionalParameters">
    <div style="visibility:hidden">
      <xsl:for-each select="transformer:getAdditionalParameters($transformer)">
        <input type="hidden" name="{@name}" value="{text()}" />
      </xsl:for-each>
    </div>
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
    <xsl:apply-templates select="includer:resolve($includer,$uri,@static)/descendant::*[@id=$ref]"
      mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@uri and not(@ref)]" mode="xeditor">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:apply-templates select="includer:resolve($includer,$uri,@static)" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@ref and not(@uri)]" mode="xeditor">
    <xsl:variable name="ref" select="transformer:replaceParameters($transformer,@ref)" />
    <xsl:apply-templates select="/*/descendant-or-self::*[@id=$ref]" mode="included" />
  </xsl:template>

  <xsl:template match="*|text()" mode="included">
    <xsl:apply-templates mode="xeditor" />
  </xsl:template>

  <!-- ========== Ignore <xed:template /> ========== -->

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

  <!-- ========== <xed:bind xpath="" initially="value"|default="value"|set="value" name="" /> ========== -->

  <xsl:template match="xed:bind" mode="xeditor">
    <xsl:variable name="initialValue" select="transformer:replaceXPaths($transformer,@initially)" />
    <xsl:value-of select="transformer:bind($transformer,@xpath,$initialValue,@name)" />
    <xsl:apply-templates select="@set|@default" mode="xeditor" />
    <xsl:apply-templates select="*" mode="xeditor" />
    <xsl:value-of select="transformer:unbind($transformer)" />
  </xsl:template>

  <xsl:template match="xed:bind/@set" mode="xeditor">
    <xsl:variable name="value" select="transformer:replaceXPaths($transformer,.)" />
    <xsl:value-of select="transformer:setValues($transformer,$value)" />
  </xsl:template>

  <xsl:template match="xed:bind/@default" mode="xeditor">
    <xsl:variable name="value" select="transformer:replaceXPaths($transformer,.)" />
    <xsl:value-of select="transformer:setDefault($transformer,$value)" />
  </xsl:template>

  <!-- ========== Default templates ========== -->

  <xsl:template match="@xed:*|xed:*" mode="xeditor" />

  <xsl:template match="*" mode="xeditor">
    <xsl:copy>
      <xsl:apply-templates select="." mode="add-attributes" />
      <xsl:apply-templates select="@*|node()" mode="xeditor" />
      <xsl:apply-templates select="." mode="add-content" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="*" mode="add-attributes" />

  <xsl:template match="*" mode="add-content" />
       
  <!-- ========== <input|button xed:target="" xed:href="" /> ========== -->

  <xsl:template match="input[contains('submit image',@type)][@xed:target]|button[@type='submit'][@xed:target]"
    mode="add-attributes">
    <xsl:attribute name="name">
      <xsl:value-of select="concat('_xed_submit_',@xed:target)" />
      <xsl:choose>
        <xsl:when test="@xed:target='subselect'">
          <xsl:value-of select="concat(':',transformer:getSubselectParam($transformer,@xed:href))" />
        </xsl:when>
        <xsl:when test="@xed:href">
          <xsl:value-of select="concat(':',@xed:href)" />
        </xsl:when>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>
  
  <!-- ========== <input /> ========== -->

  <xsl:template
    match="input[contains(',,text,password,hidden,file,color,date,datetime,datetime-local,email,month,number,range,search,tel,time,url,week,',concat(',',@type,','))]"
    mode="add-attributes">
    <xsl:attribute name="name">
      <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
    </xsl:attribute>
    <xsl:attribute name="value">
      <xsl:value-of select="transformer:getValue($transformer)" />
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="input[contains(',checkbox,radio,',concat(',',@type,','))]" mode="add-attributes">
    <xsl:attribute name="name">
      <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
    </xsl:attribute>
    <xsl:if test="transformer:hasValue($transformer,@value)">
      <xsl:attribute name="checked">checked</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template match="select" mode="xeditor">
    <xsl:value-of select="transformer:toggleWithinSelectElement($transformer)" />
    <xsl:copy>
      <xsl:apply-templates select="." mode="add-attributes" />
      <xsl:apply-templates select="@*|text()|*" mode="xeditor" />
    </xsl:copy>
    <xsl:value-of select="transformer:toggleWithinSelectElement($transformer)" />
  </xsl:template>

  <xsl:template match="option[transformer:isWithinSelectElement($transformer)]" mode="add-attributes">
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
  </xsl:template>

  <xsl:template match="textarea" mode="add-content">
    <xsl:value-of select="transformer:getValue($transformer)" />
  </xsl:template>

  <!-- ========== <xed:repeat xpath="" min="" max="" method="build|clone" /> ========== -->

  <xsl:template match="xed:repeat" mode="xeditor">
    <xsl:variable name="xed_repeat" select="." />

    <xsl:for-each select="xalan:tokenize(transformer:repeat($transformer,@xpath,@min,@max,@method))">
      <a id="rep-{transformer:nextAnchorID($transformer)}" />
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
      <xsl:if test="string-length(.) = 0">
        insert remove up down
      </xsl:if>
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
                <xsl:when test="(. = 'append') or (. = 'insert')">
                  <xsl:value-of select="transformer:getInsertParameter($transformer)" />
                </xsl:when>
                <xsl:when test="(. = 'remove')">
                  <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
                </xsl:when>
                <xsl:when test="(. = 'up')">
                  <xsl:value-of select="transformer:getSwapParameter($transformer,'up')" />
                </xsl:when>
                <xsl:when test="(. = 'down')">
                  <xsl:value-of select="transformer:getSwapParameter($transformer,'down')" />
                </xsl:when>
              </xsl:choose>
              <xsl:text>|rep-</xsl:text>
              <xsl:choose>
                <xsl:when test="(. = 'remove') and ($pos &gt; 1)"> <!-- redirect to anchor of preceding, since this one will be removed -->
                  <xsl:value-of select="transformer:previousAnchorID($transformer)" />
                </xsl:when>
                <xsl:otherwise> 
                  <xsl:value-of select="transformer:getAnchorID($transformer)" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:with-param>
          </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <!-- ========== <xed:validate xpath="" display="here|local|global" i18n="key" required="true" matches="regExp" test="xPathExpression" ... /> ========== -->

  <xsl:template match="xed:validate" mode="xeditor">
    <xsl:value-of select="transformer:addValidationRule($transformer,.)" />
    <xsl:if test="contains(@display,'here')">
      <xsl:if test="@xpath">
        <xsl:value-of select="transformer:bind($transformer,@xpath,@null,@null)" />
      </xsl:if>
      <xsl:if test="transformer:hasValidationError($transformer)">
        <xsl:apply-templates select="." mode="message" />
      </xsl:if>
      <xsl:if test="@xpath">
        <xsl:value-of select="transformer:unbind($transformer)" />
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <!-- ========== <xed:display-validation-message /> (local) ========== -->

  <xsl:template match="xed:display-validation-message" mode="xeditor">
    <xsl:if test="transformer:hasValidationError($transformer)">
      <xsl:for-each select="transformer:getFailedValidationRule($transformer)">
        <xsl:if test="contains(@display,'local')">
          <xsl:apply-templates select="." mode="message" />
        </xsl:if>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>

  <!-- ========== <xed:display-validation-messages /> (global) ========== -->

  <xsl:template match="xed:display-validation-messages" mode="xeditor">
    <xsl:for-each select="transformer:getFailedValidationRules($transformer)">
      <xsl:if test="contains(@display,'global')">
        <xsl:if test="@xpath">
          <xsl:value-of select="transformer:bind($transformer,@xpath,@null,@null)" />
        </xsl:if>
        <xsl:apply-templates select="." mode="message" />
        <xsl:if test="@xpath">
          <xsl:value-of select="transformer:unbind($transformer)" />
        </xsl:if>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- ========== <xed:if test="" /> ========== -->

  <xsl:template match="xed:if" mode="xeditor">
    <xsl:if test="transformer:test($transformer,@test)">
      <xsl:apply-templates select="node()" mode="xeditor" />
    </xsl:if>
  </xsl:template>

  <!-- ========== <xed:choose> <xed:when test=""/> <xed:otherwise /> </xed:choose> ========== -->

  <xsl:template match="xed:choose" mode="xeditor">
    <xsl:choose>
      <xsl:when test="xed:when[transformer:test($transformer,@test)]">
        <xsl:apply-templates select="xed:when[transformer:test($transformer,@test)][1]/node()" mode="xeditor" />
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
    <xsl:variable name="i18n" select="transformer:replaceParameters($transformer,@i18n)" />
    <xsl:value-of select="i18n:translate($i18n)" disable-output-escaping="yes" />
  </xsl:template>

  <xsl:template match="xed:output[@i18n and @value]" mode="xeditor">
    <xsl:variable name="i18n" select="transformer:replaceParameters($transformer,@i18n)" />
    <xsl:value-of select="i18n:translate($i18n,transformer:evaluateXPath($transformer,@value))" />
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

  <!-- ========== <xed:load-resource name="" uri="" ========== -->

  <xsl:template match="xed:load-resource" mode="xeditor">
    <xsl:variable name="uri" select="transformer:replaceXPaths($transformer,@uri)" />
    <xsl:value-of select="transformer:loadResource($transformer,$uri,@name)" />
  </xsl:template>

  <!-- ========== <xed:cleanup-rule xpath="" relevant-if="" ========== -->

  <xsl:template match="xed:cleanup-rule" mode="xeditor">
    <xsl:value-of select="transformer:addCleanupRule($transformer,@xpath,@relevant-if)" />
  </xsl:template>

  <!-- ========== <xed:param name="" default="" ========== -->
  <xsl:template match="xed:param" mode="xeditor">
    <xsl:value-of select="transformer:declareParameter($transformer,@name,@default)" />
  </xsl:template>

</xsl:stylesheet>
