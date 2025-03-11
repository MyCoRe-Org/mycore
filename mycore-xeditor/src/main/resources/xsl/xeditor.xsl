<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:transformer="xalan://org.mycore.frontend.xeditor.MCRXEditorTransformer"
                xmlns:includer="xalan://org.mycore.frontend.xeditor.MCRIncludeHandler"
                exclude-result-prefixes="xsl xed xalan transformer includer">

  <xsl:strip-space elements="xed:*" />

  <xsl:include href="resource:xsl/copynodes.xsl" />
  <xsl:include href="xslInclude:xeditor" />

  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="transformer" />

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
        <xsl:value-of select="concat($ServletsBaseURL,'XEditor')" />
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
      <xsl:copy-of select="transformer:getAdditionalParameters($transformer)" />
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
    <xsl:if test="@class">
      <xsl:value-of select="transformer:setPostProcessor($transformer,@class)" />
    </xsl:if>
    <xsl:value-of select="transformer:initializePostprocessor($transformer,.)" />
  </xsl:template>

  <!-- ========== <xed:preload uri="" static="true|false" /> ========== -->

  <xsl:template match="xed:preload" mode="xeditor">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:value-of select="includer:preloadFromURIs($includer,$uri,@static)" />
  </xsl:template>

  <!-- ========== <xed:include uri="" ref="" static="true|false" /> ========== -->

  <xsl:template match="xed:include[@uri and @ref]" mode="xeditor">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:variable name="ref" select="transformer:replaceParameters($transformer,@ref)" />
    <xsl:apply-templates select="includer:resolve($includer,$uri,@static)/descendant::*[@id=$ref]" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@uri and not(@ref)]" mode="xeditor">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:apply-templates select="includer:resolve($includer,$uri,@static)" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@ref and not(@uri)]" mode="xeditor">
    <xsl:variable name="ref" select="transformer:replaceParameters($transformer,@ref)" />
    <xsl:variable name="resolved" select="includer:resolve($includer,$ref)" />
    <xsl:choose>
      <xsl:when test="count($resolved) &gt; 0">
        <xsl:apply-templates select="$resolved" mode="included" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="/*/descendant-or-self::*[@id=$ref]" mode="included" />
      </xsl:otherwise>
    </xsl:choose>
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
    <xsl:call-template name="registerAdditionalNamespaces" />
    
    <xsl:value-of select="transformer:bind($transformer,@xpath,@initially,@name)" />
    <xsl:apply-templates select="@set|@default" mode="xeditor" />
    <xsl:apply-templates select="*" mode="xeditor" />
    <xsl:value-of select="transformer:unbind($transformer)" />
  </xsl:template>

  <xsl:template match="xed:bind/@set" mode="xeditor">
    <xsl:value-of select="transformer:setValues($transformer,.)" />
  </xsl:template>

  <xsl:template match="xed:bind/@default" mode="xeditor">
    <xsl:value-of select="transformer:setDefault($transformer,.)" />
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

  <xsl:template match="input[@type='checkbox']" mode="add-attributes">
    <xsl:call-template name="setXPathOneAsName" />
    <xsl:if test="transformer:hasValue($transformer,@value)">
      <xsl:attribute name="checked">checked</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <!-- There may be multiple checkboxes or a select multiple bound to 1-n elements: MCR-2140 -->
  <xsl:template name="setXPathOneAsName">
    <xsl:attribute name="name">
      <xsl:variable name="xPath" select="transformer:getAbsoluteXPath($transformer)" />
      <xsl:choose>
        <!-- If we are bound to the first element, it means we are bound to all elements -->
        <xsl:when test="substring($xPath,string-length($xPath)-2)='[1]'">
          <xsl:value-of select="substring($xPath,0,string-length($xPath)-2)" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$xPath" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="input[@type='radio']" mode="add-attributes">
    <xsl:attribute name="name">
      <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
    </xsl:attribute>
    <xsl:if test="transformer:hasValue($transformer,@value)">
      <xsl:attribute name="checked">checked</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template match="select" mode="xeditor">
    <xsl:value-of select="transformer:toggleWithinSelectElement($transformer,@multiple)" />
    <xsl:copy>
      <xsl:apply-templates select="." mode="add-attributes" />
      <xsl:apply-templates select="@*|text()|*" mode="xeditor" />
    </xsl:copy>
    <xsl:value-of select="transformer:toggleWithinSelectElement($transformer,@multiple)" />
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

  <xsl:template match="select[transformer:isWithinSelectMultiple($transformer)]" mode="add-attributes">
    <xsl:call-template name="setXPathOneAsName" />
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
    <xsl:call-template name="registerAdditionalNamespaces" />

    <xsl:variable name="xed_repeat" select="." />

    <xsl:for-each select="xalan:tokenize(transformer:repeat($transformer,@xpath,@min,@max,@method))">
      <xsl:variable name="anchorID" select="transformer:bindRepeatPosition($transformer)" />
      <a id="rep-{$anchorID}" />
      <xsl:apply-templates select="$xed_repeat/node()" mode="xeditor" />
      <xsl:value-of select="transformer:unbind($transformer)" />
    </xsl:for-each>
    <xsl:value-of select="transformer:unbind($transformer)" />
  </xsl:template>

  <!-- ========== <xed:controls /> ========== -->

  <xsl:template match="xed:controls" mode="xeditor">
    <xsl:for-each select="transformer:buildControls($transformer,.)">
      <xsl:apply-templates select="text()" mode="xed.control">
        <xsl:with-param name="name" select="@name" />
      </xsl:apply-templates>
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
    <xsl:variable name="matchingWhens" select="xed:when[transformer:test($transformer,@test)]" />
    <xsl:choose>
      <xsl:when test="count($matchingWhens) &gt; 0">
        <xsl:apply-templates select="$matchingWhens[1]/node()" mode="xeditor" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="xed:otherwise/node()" mode="xeditor" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ========== <xed:output i18n="" value="" /> ========== -->

  <xsl:template match="xed:output[@i18n and not(@value)]" mode="xeditor">
    <xsl:value-of select="transformer:output($transformer,@value,@i18n)" disable-output-escaping="yes" />
  </xsl:template>

  <xsl:template match="xed:output" mode="xeditor">
    <xsl:value-of select="transformer:output($transformer,@value,@i18n)" />
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
    <xsl:value-of select="transformer:loadResource($transformer,@uri,@name)" />
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
