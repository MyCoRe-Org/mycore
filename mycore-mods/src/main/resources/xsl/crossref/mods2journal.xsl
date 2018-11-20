<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:cr="http://www.crossref.org/schema/4.4.1"
                xmlns:jats="http://www.ncbi.nlm.nih.gov/JATS1"
                version="3.0"
>

  <xsl:template
      match="mods:mods[count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and contains(@valueURI, '#journal')])&gt;0]">
    <xsl:call-template name="crossrefContainer">
      <xsl:with-param name="content">
        <cr:journal>
          <cr:journal_metadata>
            <xsl:call-template name="journalContent">
              <xsl:with-param name="mods" select="."/>
            </xsl:call-template>

            <xsl:call-template name="doiData">
              <xsl:with-param name="id" select="/mycoreobject/@ID" />
            </xsl:call-template>
          </cr:journal_metadata>
        </cr:journal>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <!-- TODO: change to classification and authorityURI -->
  <xsl:template
      match="mods:mods[count(mods:genre[ends-with(@authorityURI, 'mir_genres') and contains(@valueURI, '#issue')])&gt;0]">
    <cr:journal>
      <xsl:variable name="parent"
                    select="mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and contains(@valueURI, '#journal')])&gt;0]"/>

      <xsl:if test="$parent">
        <cr:journal_metadata>
          <xsl:call-template name="journalContent">
            <xsl:with-param name="mods" select="$parent"/>
          </xsl:call-template>
        </cr:journal_metadata>
      </xsl:if>

      <cr:journal_issue>
        <xsl:for-each
            select="mods:name[count(mods:role/mods:roleTerm[@authorithy='marcrelator' and @type='code' and @type='corporate' and mods:displayForm]) &gt; 0]">
          <cr:contributors>
            <cr:organization>
              <xsl:value-of select="mods:displayForm"/>
            </cr:organization>
          </cr:contributors>
        </xsl:for-each>
        <xsl:call-template name="publicationYear">
          <xsl:with-param name="modsNode" select="."/>
        </xsl:call-template>
      </cr:journal_issue>
    </cr:journal>
  </xsl:template>

  <xsl:template
      match="mods:mods[count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and contains(@valueURI, '#article')])&gt;0]">
    <xsl:call-template name="crossrefContainer">
      <xsl:with-param name="content">
        <cr:journal>
          <xsl:choose>
            <xsl:when
                test="mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and contains(@valueURI, '#journal')])]">
              <!-- Case 1: the parent is a journal and the issue data is present in related item-->
              <xsl:variable name="parent"
                            select="mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and contains(@valueURI, '#journal')])]"/>
              <cr:journal_metadata>
                <xsl:call-template name="journalContent">
                  <xsl:with-param name="mods" select="$parent"/>
                </xsl:call-template>
              </cr:journal_metadata>
              <xsl:if test="mods:part">
                <cr:journal_issue>
                  <xsl:if test="$parent/mods:detail[@type='volume']">
                    <cr:journal_volume>
                      <cr:volume>
                        <xsl:value-of select="$parent/mods:detail[@type='volume']"/>
                      </cr:volume>
                    </cr:journal_volume>
                  </xsl:if>
                  <xsl:if test="$parent/mods:detail[@type='issue']">
                    <cr:issue>
                      <xsl:value-of select="$parent/mods:detail[@type='issue']"/>
                    </cr:issue>
                  </xsl:if>
                </cr:journal_issue>
              </xsl:if>
            </xsl:when>

            <xsl:otherwise>
              <xsl:message terminate="yes">No host present!</xsl:message>
            </xsl:otherwise>
          </xsl:choose>


          <cr:journal_article>
            <xsl:call-template name="articleTitle"/>
            <xsl:call-template name="articleAbstract"/>
            <xsl:call-template name="publicationYear">
              <xsl:with-param name="modsNode" select="."/>
            </xsl:call-template>

            <!-- TODO: pages -->


            <xsl:call-template name="doiData">
              <xsl:with-param name="id" select="/mycoreobject/@ID"/>
            </xsl:call-template>
          </cr:journal_article>
        </cr:journal>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="articleTitle">
    <cr:titles>
      <xsl:for-each select="mods:titleInfo[not(@type)]">
        <!-- convert normal titles -->
        <cr:title>
          <xsl:value-of select="mods:title/text()"/>
        </cr:title>
        <xsl:if test="mods:subTitle">
          <cr:subtitle>
            <xsl:value-of select="mods:subTitle"/>
          </cr:subtitle>
        </xsl:if>
      </xsl:for-each>
      <!-- convert translated titles-->
      <xsl:for-each select="mods:titleInfo[@type='translated']">
        <cr:original_language_title>
          <xsl:if test="@xml:lang">
            <xsl:attribute name="language">
              <xsl:value-of select="@xml:lang"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:value-of select="mods:title"/>
        </cr:original_language_title>
        <xsl:if test="mods:subTitle">
          <cr:subtitle>
            <xsl:value-of select="mods:subTitle"/>
          </cr:subtitle>
        </xsl:if>
      </xsl:for-each>
    </cr:titles>
  </xsl:template>

  <xsl:template name="articlePublicationDate">
    <xsl:if test="mods:originInfo[@type='publication']">
      <cr:publication_date>
        <cr:year>
          <!-- TODO: split into y d m -->
          <xsl:value-of select="mods:originInfo[@type='publication']/text()"/>
        </cr:year>
      </cr:publication_date>
    </xsl:if>
  </xsl:template>

  <xsl:template name="articleAbstract">
    <xsl:for-each select="mods:abstract">
      <jats:abstract>
        <xsl:if test="@xml:lang">
          <xsl:copy-of select="@xml:lang"/>
        </xsl:if>
        <jats:p>
          <xsl:copy-of select="text()"/>
        </jats:p>
      </jats:abstract>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="journalContent">
    <xsl:param name="mods"/>

    <!-- TODO: check compatibility of ISO 639 and RFC 5646 -->
    <xsl:variable name="language" select="$mods/mods:language/mods:languageTerm[@authority='rfc5646']"/>
    <xsl:if test="$language">
      <xsl:attribute name="language">
        <xsl:value-of select="$language"/>
      </xsl:attribute>
    </xsl:if>

    <!-- TODO: evaluate: use mimetype attribute ?-->
    <xsl:attribute name="metadata_distribution_opts">
      <!-- TODO: property switch to 'query' -->
      <xsl:value-of select="'any'"/>
    </xsl:attribute>

    <cr:full_title>
      <xsl:call-template name="printFullTitle">
        <xsl:with-param name="titleInfoNode" select="$mods/mods:titleInfo[1]"/>
      </xsl:call-template>
    </cr:full_title>

    <xsl:if test="not($mods/mods:identifier[@type='issn'])">
      <xsl:message terminate="yes">A issn is required in the journal</xsl:message>
    </xsl:if>
    <cr:issn>
      <xsl:value-of select="$mods/mods:identifier[@type='issn']"/>
    </cr:issn>

    <xsl:call-template name="archive_locations"/>
  </xsl:template>

</xsl:stylesheet>