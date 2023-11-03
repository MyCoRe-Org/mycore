<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:cr="http://www.crossref.org/schema/4.4.1"
                xmlns:jats="http://www.ncbi.nlm.nih.gov/JATS1"
                version="3.0"
>

  <xsl:variable name="marcrelator" select="document('classification:metadata:-1:children:marcrelator')"/>

  <xsl:template
      match="mods:mods[count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and contains(@valueURI, '#journal')])&gt;0]">
    <xsl:message>Match journal</xsl:message>
    <xsl:call-template name="crossrefContainer">
      <xsl:with-param name="content">
        <cr:journal>
          <cr:journal_metadata>
            <xsl:call-template name="journalContent">
              <xsl:with-param name="journal" select="."/>
            </xsl:call-template>

            <xsl:call-template name="doiData">
              <xsl:with-param name="id" select="/mycoreobject/@ID"/>
            </xsl:call-template>
          </cr:journal_metadata>
        </cr:journal>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template
      match="mods:mods[count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and contains(@valueURI, '#journal_issue')])&gt;0]">
    <xsl:message>Match journal_issue</xsl:message>
    <xsl:call-template name="crossrefContainer">
      <xsl:with-param name="content">
        <cr:journal>
          <xsl:variable name="parent"
                        select="mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and contains(@valueURI, '#journal')])&gt;0]"/>

          <xsl:if test="$parent">
            <cr:journal_metadata>
              <xsl:call-template name="journalContent">
                <xsl:with-param name="journal" select="$parent"/>
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
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template
      match="mods:mods[count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and ends-with(@valueURI, '#journal_article')])&gt;0]">
    <xsl:call-template name="crossrefContainer">
      <xsl:with-param name="content">
        <cr:journal>
          <xsl:choose>
            <xsl:when
                test="mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and ends-with(@valueURI, '#journal_issue')]) &gt;0 ]">
              <!-- Case 1: the parent is a issue -->
              <xsl:variable
                  select="mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and ends-with(@valueURI, '#journal_issue')]) &gt;0]"
                  name="issue"/>

              <xsl:if
                  test="not($issue/mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and ends-with(@valueURI, '#journal')]) &gt;0])">
                <xsl:message terminate="yes">Article has Issue, but Issue has no Journal</xsl:message>
              </xsl:if>

              <xsl:variable
                  select="$issue/mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and ends-with(@valueURI, '#journal')]) &gt;0]"
                  name="journal"/>

              <xsl:call-template name="journalContent">
                <xsl:with-param name="journal" select="$journal"/>
              </xsl:call-template>
              <xsl:call-template name="issueContent">
                <xsl:with-param name="journal" select="$journal"/>
                <xsl:with-param name="issue" select="$issue"/>
              </xsl:call-template>
            </xsl:when>
            <xsl:when
                test="mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and ends-with(@valueURI, '#journal')]) &gt;0]">
              <!-- Case 2: the parent is a journal and the issue data is present in related item-->
              <xsl:variable name="journal"
                            select="mods:relatedItem[@type='host' and count(mods:classification[ends-with(@authorityURI, 'crossrefTypes') and ends-with(@valueURI, '#journal')]) &gt;0]"/>
              <xsl:call-template name="journalContent">
                <xsl:with-param name="journal" select="$journal"/>
              </xsl:call-template>
              <xsl:if test="$journal/mods:part">
                <xsl:call-template name="issueContent">
                  <xsl:with-param name="journal" select="$journal"/>
                  <xsl:with-param name="issue" select="/@my_EmPtY_NoDeSeT"/>
                </xsl:call-template>
              </xsl:if>
            </xsl:when>
            <xsl:otherwise>
              <xsl:message terminate="yes">No host present!</xsl:message>
            </xsl:otherwise>
          </xsl:choose>

          <cr:journal_article>
            <xsl:variable name="article" select="."/>

            <xsl:call-template name="modsTitle">
              <xsl:with-param name="mods" select="$article"/>
            </xsl:call-template>

            <xsl:call-template name="createContributors">
              <xsl:with-param name="modsNode" select="$article"/>
            </xsl:call-template>

            <xsl:call-template name="articleAbstract">
                <xsl:with-param name="articleMods" select="$article"/>
            </xsl:call-template>

            <xsl:call-template name="publicationYear">
              <xsl:with-param name="modsNode" select="$article"/>
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

  <xsl:template name="modsTitle">
    <xsl:param name="mods" select="."/>

    <cr:titles>
      <xsl:for-each select="$mods/mods:titleInfo[not(@type)]">
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
      <xsl:for-each select="$mods/mods:titleInfo[@type='translated']">
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
    <xsl:param name="articleMods" select="." />
    <xsl:for-each select="$articleMods/mods:abstract">
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
    <xsl:param name="journal"/>

    <cr:journal_metadata>
      <!-- TODO: check compatibility of ISO 639 and RFC 5646 -->
      <xsl:variable name="language" select="$journal/mods:language/mods:languageTerm[@authority='rfc5646']"/>
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
          <xsl:with-param name="titleInfoNode" select="$journal/mods:titleInfo[1]"/>
        </xsl:call-template>
      </cr:full_title>

      <cr:issn>
        <xsl:value-of select="$journal/mods:identifier[@type='issn']"/>
      </cr:issn>

      <xsl:call-template name="archive_locations"/>
    </cr:journal_metadata>

  </xsl:template>

  <xsl:template name="issueContent">
    <xsl:param name="issue"/>
    <xsl:param name="journal"/>

    <cr:journal_issue>
      <xsl:variable name="contributors">
        <xsl:call-template name="createContributors">
          <xsl:with-param name="modsNode" select="$issue"/>
        </xsl:call-template>
        <xsl:call-template name="createContributors">
          <xsl:with-param name="modsNode" select="$journal"/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:if test="count($contributors)&gt;0">
        <cr:contributors>
          <xsl:for-each select="$contributors/*[string-length(@contributor_role)&gt;0]">
            <xsl:copy>
              <xsl:attribute name="sequence">
                <xsl:choose>
                  <xsl:when test="position() = 1">first</xsl:when>
                  <xsl:otherwise>additional</xsl:otherwise>
                </xsl:choose>
              </xsl:attribute>
              <xsl:copy-of select="@*|node()"/>
            </xsl:copy>
          </xsl:for-each>
        </cr:contributors>
      </xsl:if>

      <xsl:call-template name="modsTitle">
        <xsl:with-param name="mods" select="$issue"/>
      </xsl:call-template>

      <xsl:call-template name="publicationYear">
        <xsl:with-param name="modsNode" select="$issue"/>
      </xsl:call-template>

      <xsl:if test="$journal/mods:part">
        <xsl:call-template name="printModsPart">
          <xsl:with-param name="mods" select="$journal"/>
        </xsl:call-template>
      </xsl:if>
    </cr:journal_issue>
  </xsl:template>

  <xsl:template name="createContributors">
    <xsl:param name="modsNode"/>
    <xsl:for-each
        select="$modsNode/mods:name[count(mods:displayForm)&gt;0]">
      <xsl:choose>
        <xsl:when test="@type='corporate'">
          <xsl:call-template name="createOrganisationContributor">
            <xsl:with-param name="modsName" select="."/>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="@type='personal'">
          <xsl:variable name="modsName" select="."/>
          <xsl:call-template name="createPersonContributor">
            <xsl:with-param name="modsName" select="."/>
          </xsl:call-template>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="createPersonContributor">
    <xsl:param name="modsName"/>
    <cr:person_name>
      <xsl:call-template name="createRoleAttribute">
        <xsl:with-param name="modsName" select="$modsName"/>
      </xsl:call-template>
      <xsl:for-each select="$modsName/mods:namePart[@type='given']">
        <cr:given_name>
          <xsl:value-of select="text()"/>
        </cr:given_name>
      </xsl:for-each>
      <xsl:for-each select="$modsName/mods:namePart[@type='family']">
        <cr:surname>
          <xsl:value-of select="text()"/>
        </cr:surname>
      </xsl:for-each>
    </cr:person_name>
  </xsl:template>

  <xsl:template name="createOrganisationContributor">
    <xsl:param name="modsName"/>
    <cr:organization>
      <xsl:call-template name="createRoleAttribute">
        <xsl:with-param name="modsName" select="$modsName"/>
      </xsl:call-template>
      <xsl:value-of select="$modsName/mods:displayForm"/>
    </cr:organization>
  </xsl:template>

  <xsl:template name="createRoleAttribute">
    <xsl:param name="modsName"/>
    <xsl:variable name="mappedRole">
      <xsl:call-template name="mapMarcRelator">
        <xsl:with-param name="roleTerms"
                        select="$modsName/mods:role/mods:roleTerm[@authority='marcrelator' and @type='code']/text()"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:if test="string-length($mappedRole)&gt;0">
      <xsl:attribute name="contributor_role">
        <xsl:value-of select="$mappedRole"/>
      </xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template name="mapMarcRelator">
    <xsl:param name="roleTerms"/>
    <!-- chair??? ,review-assistant???,stats-reviewer ???,reviewer-external ???,reader ???-->
    <xsl:variable name="mapping">
      <aut>author</aut>
      <edt>editor</edt>
      <rev>reviewer</rev>
      <trl>translator</trl>
    </xsl:variable>
    <xsl:if test="count($mapping/*[local-name()=$roleTerms])&gt;0">
      <xsl:value-of select="$mapping/*[local-name()=$roleTerms][1]"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="printModsPart">
    <xsl:param name="mods"/>

    <xsl:if test="$mods/mods:part/mods:detail[@type='volume']">
      <cr:journal_volume>
        <cr:volume>
          <xsl:value-of select="$mods/mods:part/mods:detail[@type='volume']/mods:number"/>
        </cr:volume>
      </cr:journal_volume>
    </xsl:if>
    <xsl:if test="$mods/mods:part/mods:detail[@type='issue']">
      <cr:issue>
        <xsl:value-of select="$mods/mods:part/mods:detail[@type='issue']/mods:number"/>
      </cr:issue>
    </xsl:if>

  </xsl:template>

</xsl:stylesheet>