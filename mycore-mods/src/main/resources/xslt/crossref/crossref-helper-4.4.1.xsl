<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:cr="http://www.crossref.org/schema/4.4.1"
                version="3.0">

  <xsl:template name="crossrefContainer">
    <xsl:param name="content" />
    <cr:doi_batch version="4.4.1">
      <cr:head>
        <!-- these will be filled by java code -->
        <cr:doi_batch_id></cr:doi_batch_id>
        <cr:timestamp></cr:timestamp>
        <cr:depositor>
          <cr:depositor_name></cr:depositor_name>
          <cr:email_address></cr:email_address>
        </cr:depositor>
        <cr:registrant></cr:registrant>
      </cr:head>
      <cr:body>
        <xsl:copy-of select="$content" />
      </cr:body>
    </cr:doi_batch>
  </xsl:template>

  <xsl:template name="doiData">
    <xsl:param name="id" select="/mycoreobject/@ID"/>
    <cr:doi_data_replace>
      <!-- the javacode will replace the object id with the real DOI data (doi, timestamp, resource, collection)-->
      <xsl:value-of select="$id"/>
    </cr:doi_data_replace>
  </xsl:template>

  <!-- produces the archive_locations elements-->
  <xsl:template name="archive_locations">
    <xsl:param name="CLOCKSS" select="true()"/>
    <xsl:param name="LOCKSS" select="true()"/>
    <xsl:param name="Portico" select="true()"/>
    <xsl:param name="KB" select="true()"/>
    <xsl:param name="Internet_Archive" select="true()"/>
    <xsl:param name="DWT" select="true()"/>

    <cr:archive_locations>
      <xsl:if test="$CLOCKSS">
        <cr:archive name="CLOCKSS"/>
      </xsl:if>
      <xsl:if test="$LOCKSS">
        <cr:archive name="LOCKSS"/>
      </xsl:if>
      <xsl:if test="$Portico">
        <cr:archive name="Portico"/>
      </xsl:if>
      <xsl:if test="$KB">
        <cr:archive name="KB"/>
      </xsl:if>
      <xsl:if test="$Internet_Archive">
        <cr:archive name="Internet Archive"/>
      </xsl:if>
      <xsl:if test="$DWT">
        <cr:archive name="DWT"/>
      </xsl:if>
    </cr:archive_locations>
  </xsl:template>

</xsl:stylesheet>