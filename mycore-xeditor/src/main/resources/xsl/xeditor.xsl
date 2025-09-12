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
    <xsl:apply-templates select="node()" mode="xeditor" />
  </xsl:template>

  <!-- ========== <xed:form /> ========== -->

  <xsl:template match="xed:form">
    <xsl:call-template name="callTransformerHelper" />
    <form action="{$ServletsBaseURL}XEditor">
      <xsl:call-template name="replaceXPathsInAttributes" />

      <!-- method="post" is default, may be overwritten by xed:form/@method -->
      <xsl:if test="not(@method)">
        <xsl:attribute name="method">
          <xsl:text>post</xsl:text>
        </xsl:attribute>
      </xsl:if>

      <xsl:apply-templates select="node()" mode="xeditor" />

      <xsl:call-template name="callTransformerHelper">
        <xsl:with-param name="method" select="'getAdditionalParameters'" />
      </xsl:call-template>
    </form>
  </xsl:template>

  <!-- ========== xed:source et al ========== -->

  <xsl:template match="xed:source|xed:cancel|xed:param|xed:cleanup-rule|xed:load-resource|xed:output|xed:post-processor" mode="xeditor">
    <xsl:call-template name="callTransformerHelper" />
  </xsl:template>

  <!-- ========== <xed:preload uri="" static="true|false" /> ========== -->

  <xsl:template match="xed:preload" mode="xeditor">
    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI" />
    </xsl:variable>
    <xsl:variable name="result" select="document($uri)/result" /> 
    <xsl:value-of select="includer:preloadFromURIs($includer,$result/@uri,@static)" />
  </xsl:template>

  <!-- ========== <xed:include uri="" ref="" static="true|false" /> ========== -->

  <xsl:template match="xed:include" mode="xeditor">
    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI" />
    </xsl:variable>
    <xsl:variable name="result" select="document($uri)/result" />
    
    <xsl:choose>
      <xsl:when test="@uri and @ref">
        <xsl:apply-templates select="includer:resolve($includer,$result/@uri,@static)/descendant::*[@id=$result/@ref]/node()" mode="xeditor" />
      </xsl:when>
      <xsl:when test="@uri">
        <xsl:apply-templates select="includer:resolve($includer,$result/@uri,@static)/node()" mode="xeditor" />
      </xsl:when>
      <xsl:when test="@ref">
        <xsl:variable name="resolved" select="includer:resolve($includer,$result/@ref)" />
        <xsl:choose>
          <xsl:when test="count($resolved) &gt; 0">
            <xsl:apply-templates select="$resolved/node()" mode="xeditor" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="/*/descendant-or-self::*[@id=$result/@ref]/node()" mode="xeditor" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- ========== Ignore <xed:template /> ========== -->

  <xsl:template match="xed:template" mode="xeditor" />
  <xsl:template match="xed:template" />

  <!-- ========== Text ========== -->

  <xsl:template name="replaceXPathsInAttributes">
    <xsl:choose>
      <xsl:when test="@*[contains(.,'{')]">
        <xsl:variable name="uri">
          <xsl:call-template name="callTransformerHelperURI">
            <xsl:with-param name="method" select="'replaceXPaths'" />
          </xsl:call-template>
        </xsl:variable>
        <xsl:copy-of select="document($uri)/result/@*" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="@*" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="text()" mode="xeditor">
    <xsl:copy-of select="." />
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
      <xsl:call-template name="replaceXPathsInAttributes" />
      <xsl:apply-templates select="node()" mode="xeditor" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="*" mode="add-attributes" />

  <!-- ========== <input|button xed:target="" xed:href="" /> ========== -->

  <xsl:template match="input[@xed:target]|button[@xed:target]" mode="add-attributes">
    <xsl:call-template name="callTransformerHelper">
      <xsl:with-param name="method" select="'button'" />
    </xsl:call-template>        
  </xsl:template>

  <!-- ========== <input /> ========== -->

  <xsl:template match="input" mode="add-attributes">
    <xsl:call-template name="callTransformerHelper" />
  </xsl:template>

  <!-- ========== <select /> ========== -->
  
  <xsl:template match="select" mode="xeditor">
    <xsl:copy>
      <xsl:call-template name="callTransformerHelper" />
      <xsl:call-template name="replaceXPathsInAttributes" />
      <xsl:apply-templates select="node()" mode="xeditor" />
      <xsl:call-template name="callTransformerHelper" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="option" mode="add-attributes">
    <xsl:call-template name="callTransformerHelper">
      <xsl:with-param name="addText" select="true()" />
    </xsl:call-template>
  </xsl:template>

  <!-- ========== <textarea /> ========== -->

  <xsl:template match="textarea" mode="xeditor">
    <xsl:copy>
      <xsl:call-template name="callTransformerHelper" />
      <xsl:apply-templates select="node()" mode="xeditor" />
    </xsl:copy>
  </xsl:template>

  <!-- ========== <xed:repeat xpath="" min="" max="" method="build|clone" /> ========== -->

  <xsl:template match="xed:repeat" mode="xeditor">
    <xsl:variable name="repeatedNodes" select="node()" />

    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI" />
    </xsl:variable>

    <xsl:for-each select="document($uri)/result/repeat">
      <xsl:call-template name="callTransformerHelper">
        <xsl:with-param name="method" select="'bindRepeatPosition'" />
      </xsl:call-template>
      <xsl:apply-templates select="$repeatedNodes" mode="xeditor" />
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
    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI" />
    </xsl:variable>
    <xsl:copy>
      <xsl:copy-of select="document($uri)/result/@baseXPath" />
      <xsl:copy-of select="@*|node()" />
    </xsl:copy>
    
    <xsl:if test="contains(@display,'here')">
      <xsl:if test="@xpath">
        <xsl:call-template name="callTransformerHelper">
          <xsl:with-param name="method" select="'bind'" />
        </xsl:call-template>
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
    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI" />
    </xsl:variable>
    <xsl:apply-templates select="document($uri)/result/xed:validate" mode="message" />
  </xsl:template>

  <!-- ========== <xed:display-validation-messages /> (global) ========== -->

  <xsl:template match="xed:display-validation-messages" mode="xeditor">
    <xsl:variable name="uri">
      <xsl:call-template name="callTransformerHelperURI" />
    </xsl:variable>
    <xsl:for-each select="document($uri)/result/xed:validate">
      <xsl:if test="@xpath">
        <xsl:call-template name="callTransformerHelper">
          <xsl:with-param name="method" select="'bind'" />
        </xsl:call-template>
      </xsl:if>
      <xsl:apply-templates select="." mode="message" />
      <xsl:if test="@xpath">
        <xsl:call-template name="unbind" />
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
  
    <xsl:variable name="matchingIDs">
      <xsl:for-each select="xed:when">
        <xsl:variable name="uri">
          <xsl:call-template name="callTransformerHelperURI" />
        </xsl:variable>
        <xsl:if test="document($uri)/result='true'">
          <xsl:value-of select="generate-id()" />
          <xsl:text>|</xsl:text>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable name="matchingID" select="substring-before($matchingIDs,'|')" />
  
    <xsl:choose>
      <xsl:when test="string-length($matchingID) &gt; 0">
        <xsl:apply-templates select="xed:when[generate-id()=$matchingID]/node()" mode="xeditor" />
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
      <xsl:value-of select="concat(name(),'=',encoder:encode(.,'UTF-8'),'&amp;')" />
    </xsl:for-each>
    <xsl:for-each select="namespace::*">
      <xsl:value-of select="concat('xmlns:',name(),'=',encoder:encode(.,'UTF-8'),'&amp;')" />
    </xsl:for-each>
    <xsl:if test="$addText">
      <xsl:value-of select="concat('text=',.,'&amp;')" />
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
