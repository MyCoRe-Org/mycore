<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:encoder="xalan://java.net.URLEncoder"
                xmlns:helper="xalan://org.mycore.frontend.xeditor.MCRTransformerHelper"
                xmlns:includer="xalan://org.mycore.frontend.xeditor.MCRIncludeHandler"
                exclude-result-prefixes="xsl xed xalan encoder helper includer">

  <xsl:strip-space elements="xed:*" />

  <xsl:include href="resource:xsl/copynodes.xsl" />
  <xsl:include href="xslInclude:xeditor" />

  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="helper" />

  <xsl:variable name="includer" select="includer:new()" />

  <!-- ========== <xed:form /> output-only ========== -->

  <xsl:template match="xed:form[@method='output']">
    <xsl:call-template name="callTransformerHelper" />
    <xsl:apply-templates select="@*|node()" mode="xeditor" />
  </xsl:template>

  <!-- ========== <xed:form /> ========== -->

  <xsl:template match="xed:form">
    <xsl:call-template name="callTransformerHelper" />
    <form action="{$ServletsBaseURL}XEditor">
      <xsl:apply-templates select="@*" mode="xeditor" />

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

  <!-- ========== pass request parameters ========== -->

  <xsl:template name="passAdditionalParameters">
    <div style="visibility:hidden">
      <xsl:copy-of select="helper:getAdditionalParameters($helper)" />
    </div>
  </xsl:template>

  <!-- ========== xed:source et al ========== -->

  <xsl:template match="xed:source|xed:cancel|xed:param|xed:cleanup-rule|xed:load-resource|xed:output|xed:post-processor" mode="xeditor">
    <xsl:call-template name="callTransformerHelper" />
  </xsl:template>

  <!-- ========== <xed:preload uri="" static="true|false" /> ========== -->

  <xsl:template match="xed:preload" mode="xeditor">
    <xsl:variable name="uri" select="helper:replaceParameters($helper,@uri)" />
    <xsl:value-of select="includer:preloadFromURIs($includer,$uri,@static)" />
  </xsl:template>

  <!-- ========== <xed:include uri="" ref="" static="true|false" /> ========== -->

  <xsl:template match="xed:include[@uri and @ref]" mode="xeditor">
    <xsl:variable name="uri" select="helper:replaceParameters($helper,@uri)" />
    <xsl:variable name="ref" select="helper:replaceParameters($helper,@ref)" />
    <xsl:apply-templates select="includer:resolve($includer,$uri,@static)/descendant::*[@id=$ref]" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@uri and not(@ref)]" mode="xeditor">
    <xsl:variable name="uri" select="helper:replaceParameters($helper,@uri)" />
    <xsl:apply-templates select="includer:resolve($includer,$uri,@static)" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@ref and not(@uri)]" mode="xeditor">
    <xsl:variable name="ref" select="helper:replaceParameters($helper,@ref)" />
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
      <xsl:value-of select="helper:replaceXPaths($helper,.)" />
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="text()" mode="xeditor">
    <xsl:value-of select="." />
  </xsl:template>

  <!-- ========== <xed:bind xpath="" initially="value"|default="value"|set="value" name="" /> ========== -->

  <xsl:template match="xed:bind" mode="xeditor">
    <xsl:call-template name="callTransformerHelper" />
    <xsl:apply-templates select="*" mode="xeditor" />
    <xsl:call-template name="unbind" />
  </xsl:template>
  
  <xsl:template name="unbind">
    <xsl:call-template name="callTransformerHelper">
      <xsl:with-param name="method" select="'unbind'" />
    </xsl:call-template>
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
          <xsl:value-of select="concat(':',helper:getSubselectParam($helper,@xed:href))" />
        </xsl:when>
        <xsl:when test="@xed:href">
          <xsl:value-of select="concat(':',@xed:href)" />
        </xsl:when>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>

  <!-- ========== <input /> ========== -->

  <xsl:template
    match="input[contains(',,text,password,radio,checkbox,hidden,file,color,date,datetime,datetime-local,email,month,number,range,search,tel,time,url,week,',concat(',',@type,','))]"
    mode="add-attributes">
    <xsl:call-template name="callTransformerHelper" />
  </xsl:template>

  <xsl:template match="select" mode="xeditor">
    <xsl:copy>
      <xsl:call-template name="callTransformerHelper" />
      <xsl:apply-templates select="@*|text()|*" mode="xeditor" />
    </xsl:copy>
    <xsl:call-template name="callTransformerHelper" />
  </xsl:template>

  <xsl:template match="option" mode="add-attributes">
    <xsl:call-template name="callTransformerHelper">
      <xsl:with-param name="addText" select="true()" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="textarea" mode="add-attributes">
    <xsl:call-template name="callTransformerHelper" />
  </xsl:template>

  <xsl:template match="textarea" mode="add-content">
    <xsl:value-of select="helper:getValue($helper)" />
  </xsl:template>

  <!-- ========== <xed:repeat xpath="" min="" max="" method="build|clone" /> ========== -->

  <xsl:template match="xed:repeat" mode="xeditor">
    <xsl:variable name="xed_repeat" select="." />

    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI" />
    </xsl:variable>

    <xsl:for-each select="document($uri)/result/repeat">
      <xsl:variable name="anchorID" select="helper:bindRepeatPosition($helper)" />
      <a id="rep-{$anchorID}" />
      <xsl:apply-templates select="$xed_repeat/node()" mode="xeditor" />
      <xsl:call-template name="unbind" />
    </xsl:for-each>
    <xsl:call-template name="unbind" />
  </xsl:template>

  <!-- ========== <xed:controls /> ========== -->

  <xsl:template match="xed:controls" mode="xeditor">
    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI">
        <xsl:with-param name="addText" select="true()" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:for-each select="document($uri)/result/control">
      <xsl:apply-templates select="text()" mode="xed.control">
        <xsl:with-param name="name" select="@name" />
      </xsl:apply-templates>
    </xsl:for-each>
  </xsl:template>

  <!-- ========== <xed:validate xpath="" display="here|local|global" i18n="key" required="true" matches="regExp" test="xPathExpression" ... /> ========== -->

  <xsl:template match="xed:validate" mode="xeditor">
    <xsl:value-of select="helper:addValidationRule($helper,.)" />
    
    <xsl:if test="contains(@display,'here')">
      <xsl:if test="@xpath">
        <xsl:value-of select="helper:bind($helper,@xpath,@null,@null)" />
      </xsl:if>
      <xsl:if test="helper:hasValidationError($helper)">
        <xsl:apply-templates select="." mode="message" />
      </xsl:if>
      <xsl:if test="@xpath">
        <xsl:call-template name="unbind" />
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <!-- ========== <xed:display-validation-message /> (local) ========== -->

  <xsl:template match="xed:display-validation-message" mode="xeditor">
    <xsl:if test="helper:hasValidationError($helper)">
      <xsl:for-each select="helper:getFailedValidationRule($helper)">
        <xsl:if test="contains(@display,'local')">
          <xsl:apply-templates select="." mode="message" />
        </xsl:if>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>

  <!-- ========== <xed:display-validation-messages /> (global) ========== -->

  <xsl:template match="xed:display-validation-messages" mode="xeditor">
    <xsl:for-each select="helper:getFailedValidationRules($helper)">
      <xsl:if test="contains(@display,'global')">
        <xsl:if test="@xpath">
          <xsl:value-of select="helper:bind($helper,@xpath,@null,@null)" />
        </xsl:if>
        <xsl:apply-templates select="." mode="message" />
        <xsl:if test="@xpath">
          <xsl:call-template name="unbind" />
        </xsl:if>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- ========== <xed:if test="" /> ========== -->

  <xsl:template match="xed:if" mode="xeditor">
    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI" />
    </xsl:variable>
    <xsl:if test="document($uri)/result='true'">
      <xsl:apply-templates select="node()" mode="xeditor" />
    </xsl:if>
  </xsl:template>

  <!-- ========== <xed:choose> <xed:when test=""/> <xed:otherwise /> </xed:choose> ========== -->

  <xsl:template match="xed:choose" mode="xeditor">
    <xsl:variable name="firstMatchingTest" select="xed:when[helper:test($helper,@test)][1]/@test" />

    <xsl:choose>
      <xsl:when test="string-length($firstMatchingTest) &gt; 0">
        <xsl:apply-templates select="xed:when[@test=$firstMatchingTest]/node()" mode="xeditor" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="xed:otherwise/node()" mode="xeditor" />
      </xsl:otherwise>
    </xsl:choose>
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

  <!-- ========== call transformer helper ========== -->
  
  <xsl:param name="SessionID" />
  
  <xsl:template name="callTransformerHelperURI">
    <xsl:param name="method" select="local-name()" />
    <xsl:param name="addText" select="false()" />
  
    <xsl:value-of select="concat('xedTransformerHelper:',$SessionID,':',$method,':')" />
    
    <xsl:for-each select="@*">
      <xsl:value-of select="concat(local-name(),'=',encoder:encode(.,'UTF-8'),'&amp;')" />
    </xsl:for-each>
    <xsl:for-each select="namespace::*">
      <xsl:value-of select="concat('xmlns:',name(),'=',encoder:encode(.,'UTF-8'),'&amp;')" />
    </xsl:for-each>
    <xsl:if test="$addText">
      <xsl:value-of select="concat('text=',text(),'&amp;')" />
    </xsl:if>
  </xsl:template>
  
  <xsl:template name="callTransformerHelper">
    <xsl:param name="method" select="local-name()" />
    <xsl:param name="addText" select="false()" />
  
    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI">
        <xsl:with-param name="method" select="$method" />
        <xsl:with-param name="addText" select="$addText" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:for-each select="document($uri)/result">
      <xsl:copy-of select="@*|node()" />
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
