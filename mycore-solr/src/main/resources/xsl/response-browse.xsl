<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mcr="http://www.mycore.org/" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:encoder="xalan://java.net.URLEncoder"
  exclude-result-prefixes="xlink mcr i18n xsl">
  <xsl:output method="html" />
  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:include href="response-utils.xsl" />
  <xsl:include href="xslInclude:solrResponse" />
  <!-- include custom templates for supported objecttypes -->
  <xsl:include href="xslInclude:objectTypes" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:variable name="PageTitle" />

  <!-- Trefferliste ausgeben -->
  <xsl:template match="/response">
    <xsl:variable name="ResultPages">
      <xsl:call-template name="solr.Pagination">
      	<xsl:with-param name="href" select="concat($proxyBaseURL,$HttpSession,$solrParams)" />
        <xsl:with-param name="size" select="$rows" />
        <xsl:with-param name="currentpage" select="$currentPage" />
        <xsl:with-param name="totalpage" select="$totalPages" />
      </xsl:call-template>
    </xsl:variable>
    <!-- table header -->
    <table class="resultHeader" cellspacing="0" cellpadding="0">
      <tr>
        <td class="resultPages">
          <xsl:copy-of select="$ResultPages" />
        </td>
        <td>
        	<xsl:variable name="params">
				<xsl:for-each
					select="/response/lst[@name='responseHeader']/lst[@name='params']/str">
					<xsl:choose>
						<xsl:when test="@name='rows' or @name='XSL.Style' or @name='fl' or @name='start'">
							<!-- skip them -->
						</xsl:when>
						<xsl:when test="@name='origrows' or @name='origXSL.Style' or @name='origfl'">
							<!-- ParameterName=origParameterValue -->
							<xsl:value-of select="concat(substring-after(@name, 'orig'),'=', encoder:encode(., 'UTF-8'))" />
							<xsl:if test="not (position() = last())">
								<xsl:value-of select="'&amp;'" />
							</xsl:if>
						</xsl:when>
						<xsl:otherwise>
							<!-- parameterName=parameterValue -->
							<xsl:value-of select="concat(@name,'=', encoder:encode(., 'UTF-8'))" />
							<xsl:if test="not (position() = last())">
								<xsl:value-of select="'&amp;'" />
							</xsl:if>
						</xsl:otherwise>
					</xsl:choose>
					
				</xsl:for-each>
			</xsl:variable>
			
			<xsl:variable name="origRows" select="/response/lst[@name='responseHeader']/lst[@name='params']/str[@name='origrows']" />
			<xsl:variable name="newStart" select="$start - $start mod $origRows" />
     	  	<xsl:variable name="href" select="concat($proxyBaseURL,'?', $HttpSession, $params, '&amp;start=', $newStart)" />
        
          <a href="{$href}">
          	<xsl:value-of select="i18n:translate('component.solr.searchresult.back')" />
          </a>
        </td>
      </tr>
    </table>
    <xsl:apply-templates select="result/doc[1]"  />
    <!-- table footer -->
    <xsl:copy-of select="$ResultPages" />
  </xsl:template>

  <xsl:template match="doc">
  	<div id="permalink">
  		<span class="linklabel">
  			<xsl:value-of select="concat(i18n:translate('component.solr.searchresult.objectlink'), ' : ')" />
  		</span>
  		<span class="linktext">
  			<xsl:variable name="linkToDocument">
  				<xsl:value-of select="concat($WebApplicationBaseURL, 'receive/', str[@name='id'])" />
  			</xsl:variable>
  			<a href="{$linkToDocument}">
  				<xsl:value-of select="$linkToDocument" />
  			</a>
  		</span>
  	</div>
  	<!-- change url in browser -->
	<script type="text/javascript">
		<xsl:value-of select="concat('var pageurl = &quot;', $WebApplicationBaseURL, 'receive/', str[@name='id'], '&quot;;')" />	
		if(typeof window.history.replaceState == &quot;function&quot;){
			window.history.replaceState({path:pageurl},&quot; <xsl:value-of select="i18n:translate('component.solr.searchresult.resultList')" /> &quot;,pageurl);
			document.getElementById(&quot;permalink&quot;).style.display = &quot;none&quot;;
			
		}
	</script>
    <xsl:apply-templates select="document(concat('mcrobject:',str[@name='id']))/mycoreobject" mode="present" />
  </xsl:template>

</xsl:stylesheet>