<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan encoder">

	<!-- 
		This Stylesheet checks for a join filterquery and creates a new request to the solr server and adds the result.
		It adds additional information of the joined objects.

		If you got a query like this
		<code>
			<str name="fq">{!join from=returnId to=id}+file_name:*.jpg</str>
		</code>
		
		And a normal result like this
		
		<code>
			<doc id="DocPortal_document_00410901" objectType="document" />
			<doc id="DocPortal_document_00410902" objectType="document" />
			<doc id="DocPortal_document_00410904" objectType="document" />
		</code>
		
		The Stylesheet creates a additional request
		
		<code>
			+file_name:*.jpg +returnId:(DocPortal_document_00410901 OR DocPortal_document_00410902 OR DocPortal_document_00410904)
			
		</code>
		
		The result of this query will be added to the response of the current result
		<code>
			<response>
				<lst name="responseHeader">
					...
					<str name="fq">{!join from=returnId to=id}+file_name:*.jpg</str>
					...
					</lst>
				</lst>
				<result name="response" numFound="5" start="0">...</result>
				<response>...</response>
			</response>
		<code/>
	 -->

  <xsl:variable name="JoinPattern">
    <xsl:value-of select="'{!join from=returnId to=id}'" />
  </xsl:variable>

  <xsl:template match="/response">
    <xsl:copy>
      <xsl:copy-of select="@*|node()" />
      <xsl:variable name="fq" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='fq' and starts-with(.,$JoinPattern)]" />
      <!-- query extends by about 36 bytes per MCROBjectID, limit to 100 results  -->
      <xsl:if test="$fq and result/doc and not(result/doc[101]) ">
        <xsl:variable name="orChain">
          <xsl:apply-templates mode="query" select="result/doc/@id" />
        </xsl:variable>
        <xsl:variable name="query">
          <xsl:value-of select="substring-after($fq, $JoinPattern)" />
          <xsl:value-of select="' +returnId:('" />
          <xsl:value-of select="substring-after($orChain, 'OR ')" />
          <xsl:value-of select="')'" />
        </xsl:variable>
        <xsl:apply-templates select="document(concat('solr:q=', encoder:encode($query)))" mode="join"/>
      </xsl:if>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="response" mode="join">
    <xsl:copy>
      <xsl:attribute name="subresult">
        <xsl:value-of select="'unmerged'" />
      </xsl:attribute>
      <xsl:copy-of select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <xsl:template mode="query" match="@id">
    <xsl:value-of select="concat(' OR ',.)" />
  </xsl:template>

</xsl:stylesheet>