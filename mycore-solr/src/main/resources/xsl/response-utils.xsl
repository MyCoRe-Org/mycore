<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:acl="xalan://org.mycore.access.MCRAccessManager"
  xmlns:xalan="http://xml.apache.org/xalan" xmlns:encoder="xalan://java.net.URLEncoder" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" exclude-result-prefixes="acl mcrxsl encoder xalan i18n">
  <xsl:param name="RequestURL" />

  <xsl:variable name="response" select="/response|/mycoreobject/response" />
  <xsl:variable name="loginURL"
    select="concat( $ServletsBaseURL, 'MCRLoginServlet',$HttpSession,'?url=', encoder:encode( string( $RequestURL ) ) )" />
  <xsl:key name="derivate" match="response/response[@subresult='derivate']/result/doc" use="str[@name='returnId']" />
  <xsl:key name="files-by-object"
    match="response/response[@subresult='unmerged']/result/doc|response/lst[@name='grouped']/lst[@name='returnId']/arr[@name='groups']/lst/result/doc[str[@name='objectType']='data_file']"
    use="str[@name='returnId']" />
  <xsl:key name="files-by-derivate"
    match="response/response[@subresult='unmerged']/result/doc|response/lst[@name='grouped']/lst[@name='returnId']/arr[@name='groups']/lst/result/doc[str[@name='objectType']='data_file']"
    use="str[@name='derivateID']" />
  <xsl:key name="groupOwner" match="response/response[@subresult='groupOwner']/result/doc" use="str[@name='id']" />
  <xsl:variable name="params" select="$response/lst[@name='responseHeader']/lst[@name='params']" />
  <xsl:variable name="result" select="$response/result[@name='response']" />
  <xsl:variable name="groups" select="$response/lst[@name='grouped']/lst[@name='returnId']/arr[@name='groups']" />
  <xsl:variable name="hits">
    <xsl:choose>
      <xsl:when test="$result/@numFound">
        <xsl:value-of select="number($result/@numFound)" />
      </xsl:when>
      <xsl:when test="$response/lst[@name='grouped']/lst[@name='returnId']/int[@name='ngroups']">
        <xsl:value-of select="number($response/lst[@name='grouped']/lst[@name='returnId']/int[@name='ngroups'])" />
      </xsl:when>
      <xsl:when test="$response/lst[@name='grouped']/lst[@name='returnId']/int[@name='matches']">
        <xsl:value-of select="number($response/lst[@name='grouped']/lst[@name='returnId']/int[@name='matches'])" />
      </xsl:when>
      <xsl:when test="$groups">
        <xsl:value-of select="count($groups/lst)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="0" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="start">
    <xsl:choose>
      <xsl:when test="$result/@start">
        <xsl:value-of select="number($result/@start)" />
      </xsl:when>
      <xsl:when test="xalan:nodeset($params)/str[@name='start']">
        <xsl:value-of select="number(xalan:nodeset($params)/str[@name='start'])" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="0" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="rowTemp">
    <xsl:if test="xalan:nodeset($params)/arr[@name='rows']">
      <xsl:message terminate="yes">
        <xsl:value-of select="'Too many rows parameter!'" />
      </xsl:message>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="xalan:nodeset($params)/str[@name='rows']">
        <xsl:value-of select="number(xalan:nodeset($params)/str[@name='rows'])" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="docCount" select="count($result/doc|$groups/lst)" />
        <xsl:choose>
          <xsl:when test="not($result/@numFound) or $result/@numFound &gt; $docCount">
            <xsl:value-of select="$docCount" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="number($result/@numFound)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="rows" select="number($rowTemp)" />
  <xsl:variable name="currentPage" select="ceiling((($start + 1) - $rows) div $rows)+1" />
  <xsl:variable name="query" select="xalan:nodeset($params)/str[@name='q']" />
  <xsl:variable name="proxyBaseURL">
    <xsl:choose>
      <xsl:when test="string-length($HttpSession) &gt; 0 and contains($RequestURL, $HttpSession)">
        <xsl:value-of select="substring-before($RequestURL, $HttpSession)" />
      </xsl:when>
      <xsl:when test="contains($RequestURL, '?')">
        <xsl:value-of select="substring-before($RequestURL, '?')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$RequestURL" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="totalPages">
    <xsl:choose>
      <xsl:when test="$hits = 0">
        <xsl:value-of select="1" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="ceiling($hits div $rows )" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- retain the original query parameters, for attaching them to a url -->
  <xsl:variable name="solrParams">
    <xsl:value-of select="'?'" />
    <xsl:for-each select="$params/*[not(@name='start' or @name='rows')]">
      <xsl:choose>
        <xsl:when test="local-name(.)='arr'">
          <xsl:variable name="pName" select="@name" />
          <xsl:for-each select="str">
            <xsl:value-of select="concat($pName,'=', encoder:encode(., 'UTF-8'))" />
            <xsl:if test="not (position() = last())">
              <xsl:value-of select="'&amp;'" />
            </xsl:if>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <!-- local-name()='str' -->
          <!-- parameterName=parameterValue -->
          <xsl:value-of select="concat(@name,'=', encoder:encode(., 'UTF-8'))" />
        </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="not (position() = last())">
        <xsl:value-of select="'&amp;'" />
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:template name="solr.Pagination">
    <xsl:param name="i" select="1" />
    <xsl:param name="href" select="concat($proxyBaseURL,$HttpSession,$solrParams)" />
    <xsl:param name="size" />
    <xsl:param name="currentpage" />
    <xsl:param name="totalpage" />
    <xsl:param name="class" select="''" />
    <xsl:variable name="prev" select="'«'" />
    <xsl:variable name="next" select="'»'" />

    <div class="pagination_box text-center">
      <ul>
        <xsl:attribute name="class">
          <xsl:value-of select="concat('pagination ',$class)" />
        </xsl:attribute>
        <li>
          <xsl:choose>
            <xsl:when test="$currentpage = 1">
              <xsl:attribute name="class">
                <xsl:value-of select="'disabled'" />
              </xsl:attribute>
              <a href="#">
                <xsl:value-of select="$prev" />
              </a>
            </xsl:when>
            <xsl:otherwise>
              <a href="{concat($href, '&amp;start=',(($currentpage -2) * $size), '&amp;rows=', $size)}">
                <xsl:value-of select="$prev" />
              </a>
            </xsl:otherwise>
          </xsl:choose>
        </li>
        <xsl:call-template name="solr.PageGen">
          <xsl:with-param name="i" select="$i" />
          <xsl:with-param name="href" select="$href" />
          <xsl:with-param name="size" select="$size" />
          <xsl:with-param name="currentpage" select="$currentpage" />
          <xsl:with-param name="totalpage" select="$totalpage" />
        </xsl:call-template>
        <li>
          <xsl:choose>
            <xsl:when test="$currentpage = $totalpage">
              <xsl:attribute name="class">
                <xsl:value-of select="'disabled'" />
              </xsl:attribute>
              <a href="#">
                <xsl:value-of select="$next" />
              </a>
            </xsl:when>
            <xsl:otherwise>
              <a href="{concat($href, '&amp;start=',($currentpage * $size), '&amp;rows=', $size)}">
                <xsl:value-of select="$next" />
              </a>
            </xsl:otherwise>
          </xsl:choose>
        </li>
      </ul>
    </div>
  </xsl:template>

  <xsl:template name="solr.PageGen">
    <xsl:param name="i" select="1" />
    <xsl:param name="href" select="concat($proxyBaseURL,$HttpSession,$solrParams)" />
    <xsl:param name="size" />
    <xsl:param name="currentpage" />
    <xsl:param name="totalpage" />
    <xsl:variable name="PageWindowSize">
      <xsl:choose>
        <xsl:when test="$currentpage &gt; 999">
          <xsl:value-of select="4" />
        </xsl:when>
        <xsl:when test="$currentpage &gt; 99">
          <xsl:value-of select="6" />
        </xsl:when>
        <xsl:when test="$currentpage &gt; 9">
          <xsl:value-of select="7" />
        </xsl:when>
        <xsl:when test="$totalpage &gt; 999">
          <xsl:value-of select="8" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="9" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- jumpSize is to determine the pages to be skipped -->
    <xsl:variable name="jumpSize">
      <xsl:choose>
        <!-- current printed page number is smaller than current displayed page -->
        <xsl:when test="$i &lt; $currentpage">
          <xsl:choose>
            <!-- This is to support a bigger PageWindow at the end of page listing and to skip a jump of 2 -->
            <xsl:when
              test="(($totalpage - $PageWindowSize - 1) &lt;= $i) or
                                  (($currentpage - floor(($PageWindowSize -1) div 2) - 1) = 2)">
              <xsl:value-of select="1" />
            </xsl:when>
            <!-- This is to support a bigger PageWindow at the begin of page listing -->
            <xsl:when test="($totalpage - $currentpage) &lt; $PageWindowSize">
              <xsl:value-of select="($totalpage - $PageWindowSize - 1)" />
            </xsl:when>
            <xsl:when test="(($currentpage - $i) &lt;= floor(($PageWindowSize -1) div 2))">
              <xsl:value-of select="1" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="($currentpage - floor(($PageWindowSize -1) div 2) - 1)" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="$i &gt; $currentpage">
          <xsl:choose>
            <!-- jump only one if your near currentpage, or at last page or to support bigger window at beginning or to skip a jump of 2 -->
            <xsl:when
              test="( (($i - $currentpage) &lt; round(($PageWindowSize -1) div 2)) or ($i = $totalpage) or ($currentpage &lt;=$PageWindowSize and $i &lt;= $PageWindowSize) or ($totalpage - $i = 2))">
              <xsl:value-of select="1" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="($totalpage - $i)" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="1" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="running">
      <xsl:if test="$i &lt;= $totalpage">
        <xsl:text>true</xsl:text>
      </xsl:if>
    </xsl:variable>
    <xsl:if test="$running='true'">
      <li>
        <xsl:if test="$i=$currentpage">
          <xsl:attribute name="class">
            <xsl:value-of select="'active'" />
          </xsl:attribute>
        </xsl:if>
        <!-- XSL.Style parameter in order to view search-results in a list -->
        <a href="{concat($href, '&amp;start=',(($i -1) * $size), '&amp;rows=', $size)}">
          <xsl:value-of select="$i" />
        </a>
      </li>
      <xsl:if test="$jumpSize &gt; 1">
        <li class="disabled">
          <a href="#">...</a>
        </li>
      </xsl:if>
      <xsl:call-template name="solr.PageGen">
        <xsl:with-param name="i" select="$i + $jumpSize" />
        <xsl:with-param name="href" select="$href" />
        <xsl:with-param name="size" select="$size" />
        <xsl:with-param name="currentpage" select="$currentpage" />
        <xsl:with-param name="totalpage" select="$totalpage" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template match="doc" mode="displayText">
    <xsl:variable name="identifier" select="@id" />
    <xsl:choose>
      <xsl:when test="./str[@name='search_result_link_text']">
        <xsl:value-of select="./str[@name='search_result_link_text']" />
      </xsl:when>
      <xsl:when test="./str[@name='fileName']">
        <xsl:value-of select="./str[@name='fileName']" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$identifier" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="doc" mode="linkTo">
    <xsl:variable name="identifier" select="@id" />
    <xsl:variable name="linkTo">
      <xsl:choose>
        <xsl:when test="str[@name='objectType'] = 'data_file'">
          <xsl:value-of select="concat($WebApplicationBaseURL, 'receive/', str[@name='returnId'])" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:choose>
            <xsl:when test="acl:checkPermission($identifier,'read')">
              <xsl:value-of select="concat($WebApplicationBaseURL, 'receive/',$identifier,$HttpSession)" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$loginURL" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="displayText">
      <xsl:apply-templates mode="displayText" select="." />
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="$linkTo = $loginURL">
        <span itemprop="name">
          <xsl:value-of select="$displayText" />
        </span>
        &#160;
        <a href="{$linkTo}">
          <img src="{concat($WebApplicationBaseURL,'images/paper_lock.gif')}" />
        </a>
      </xsl:when>
      <xsl:otherwise>
        <a href="{$linkTo}" itemprop="url">
          <span itemprop="name">
            <xsl:value-of select="$displayText" />
          </span>
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="doc" mode="hitInFiles">
    <xsl:variable name="mcrid" select="@id" />
    <xsl:variable name="objectType" select="@objectType" />
    <xsl:variable name="files" select="key('files-by-object', $mcrid)" />
    <xsl:if test="$files">
      <div class="hitInFile">
        <span class="hitInFileLabel">
          <xsl:value-of select="concat(i18n:translate('results.file'),' ')" />
        </span>
        <ul>
          <!-- check read permission once per derivate id -->
          <xsl:for-each select="$files[count(.|key('files-by-derivate',str[@name='derivateID'])[1]) = 1]">
            <xsl:sort select="str[@name='derivateID']" />
            <xsl:variable name="derivateId" select="str[@name='derivateID']" />
            <xsl:variable name="object-view-derivate" select="acl:checkPermission($mcrid,'view-derivate')" />
            <xsl:variable name="isDisplayedEnabled" select="mcrxsl:isDisplayedEnabledDerivate($derivateId)" />
            <xsl:variable name="mayWriteDerivate" select="acl:checkPermission($derivateId,'writedb')" />
            <xsl:choose>
              <xsl:when
                test="acl:checkPermissionForReadingDerivate($derivateId) and $object-view-derivate and $isDisplayedEnabled = 'true' or $mayWriteDerivate">
                <!-- for every hit in derivate list files -->
                <xsl:for-each select="key('files-by-derivate',$derivateId)">
                  <li>
                    <xsl:apply-templates select="." mode="fileLink" />
                  </li>
                </xsl:for-each>
              </xsl:when>
              <xsl:otherwise>
                <xsl:if test="$isDisplayedEnabled = 'true'">
                  <span>
                    <!-- Zugriff auf 'Abbildung' gesperrt -->
                    <xsl:value-of select="i18n:translate('metaData.derivateLocked',i18n:translate(concat('metaData.',$objectType,'.[derivates]')))" />
                  </span>
                </xsl:if>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </ul>
      </div>
    </xsl:if>
  </xsl:template>

  <xsl:template match="doc" mode="fileLink">
    <xsl:param name="mcrid" select="str[@name='returnId']" />
    <xsl:param name="derivateId" select="str[@name='derivateID']" />
    <xsl:param name="fileNodeServlet" select="concat($ServletsBaseURL,'MCRFileNodeServlet/')" />
    <!-- doc element of 'unmerged' response -->
    <xsl:variable name="filePath" select="str[@name='filePath']" />
    <xsl:variable name="fileName" select="str[@name='fileName']" />
    <a href="{concat($fileNodeServlet,$derivateId,mcrxsl:encodeURIPath($filePath),$HttpSession)}">
      <xsl:value-of select="$fileName" />
    </a>
  </xsl:template>

</xsl:stylesheet>