<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision: 1.50 $ $Date: 2007-12-05 16:11:02 $ -->
<!-- ============================================== -->
<!-- Authors: Thomas Scheffler (yagee) -->
<!-- Authors: Andreas Trappe (lezard) -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:xlink="http://www.w3.org/1999/xlink" exclude-result-prefixes="xlink i18n layoutUtils mcrxml"
  xmlns:layoutUtils="xalan://org.mycore.frontend.MCRLayoutUtilities">
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="HttpSession" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="loaded_navigation_xml" />
  <xsl:param name="browserAddress" />
  <xsl:param name="readAccess" />
  <xsl:param name="RequestURL" />
  <xsl:param name="WebApplicationBaseURL" />

    <!--
        Template: UrlSetParam
        synopsis: Inserts a $HttpSession to an URL
        param:

        url: URL to include the session
    -->
  <xsl:template name="UrlAddSession">
    <xsl:param name="url" />
        <!-- There are two possibility for a parameter to appear in an URL:
            1.) after a ? sign
            2.) after a & sign
            In both cases the value is either limited by a & sign or the string end
            //-->
    <xsl:choose>
      <xsl:when test="starts-with($url,$WebApplicationBaseURL)">
                <!--The document is on our server-->
        <xsl:variable name="pathPart">
          <xsl:choose>
            <xsl:when test="contains($url,'?')">
              <xsl:value-of select="substring-before($url,'?')" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$url" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:variable name="queryPart">
          <xsl:value-of select="substring-after($url,$pathPart)" />
        </xsl:variable>
        <xsl:value-of select="concat($pathPart,$HttpSession,$queryPart)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$url" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
    <!--
        Template: UrlDeleteParam
        synopsis: removes a jsessionID parameter from a URL
        param:

        url: URL to remove the session
    -->
  <xsl:template name="UrlDeleteSession">
    <xsl:param name="url" />
    <xsl:value-of select="mcrxml:regexp($url, ';jsessionid=[^?#]+', '')" />
  </xsl:template>
    <!--
        Template: UrlSetParam
        synopsis: Replaces a parameter value or adds a parameter to an URL
        param:

        url: URL to contain the parameter and value
        par: name of the parameter
        value: new value
    -->
  <xsl:template name="UrlSetParam">
    <xsl:param name="url" />
    <xsl:param name="par" />
    <xsl:param name="value" />
        <!-- There are two possibility for a parameter to appear in an URL:
            1.) after a ? sign
            2.) after a & sign
            In both cases the value is either limited by a & sign or the string end
            //-->
    <xsl:variable name="asFirstParam">
      <xsl:value-of select="concat('?',$par,'=')" />
    </xsl:variable>
    <xsl:variable name="asOtherParam">
      <xsl:value-of select="concat('&amp;',$par,'=')" />
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="contains($url,$asFirstParam) or contains($url,$asOtherParam)">
                <!-- Parameter is present -->
        <xsl:variable name="asParam">
          <xsl:choose>
            <xsl:when test="contains($url,$asFirstParam)">
                            <!-- Parameter is right after a question mark //-->
              <xsl:value-of select="$asFirstParam" />
            </xsl:when>
            <xsl:when test="contains($url,asOtherParam)">
                            <!-- Parameter is right after a & sign //-->
              <xsl:value-of select="$asOtherParam" />
            </xsl:when>
          </xsl:choose>
        </xsl:variable>
        <xsl:variable name="newurl">
          <xsl:value-of select="substring-before($url,$asParam)" />
          <xsl:value-of select="$asParam" />
          <xsl:value-of select="$value" />
          <xsl:if test="contains(substring-after($url,$asParam),'&amp;')">
                        <!--OK now we know that there are parameters left //-->
            <xsl:value-of select="concat('&amp;',substring-after(substring-after($url,$asParam),'&amp;'))" />
          </xsl:if>
          <xsl:if test="contains($url, '#')">
            <xsl:value-of select="concat('#',substring-after($url, '#'))" />
          </xsl:if>
        </xsl:variable>
        <xsl:value-of select="$newurl" />
      </xsl:when>
      <xsl:otherwise>
                <!-- The parameter is not yet specified //-->
        <xsl:choose>
          <xsl:when test="contains($url,'?')">
                        <!-- Other parameters are present //-->
            <xsl:value-of select="concat($url,'&amp;',$par,'=',$value)" />
          </xsl:when>
          <xsl:otherwise>
                        <!-- No other parameter are present //-->
            <xsl:value-of select="concat($url,'?',$par,'=',$value)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

    <!--
        Template: UrlGetParam
        synopsis: Gets the value of a given parameter from a specific URL
        param:

        url: URL containing the parameter and value
        par: name of the parameter
    -->
  <xsl:template name="UrlGetParam">
    <xsl:param name="url" />
    <xsl:param name="par" />
        <!-- There are two possibility for a parameter to appear in an URL:
            1.) after a ? sign
            2.) after a & sign
            In both cases the value is either limited by a & sign or the string end
            //-->
    <xsl:variable name="afterParam">
      <xsl:choose>
        <xsl:when test="contains($url,concat('?',$par,'='))">
                    <!-- Parameter is right after a question mark //-->
          <xsl:value-of select="substring-after($url,concat('?',$par,'='))" />
        </xsl:when>
        <xsl:when test="contains($url,concat('&amp;',$par,'='))">
                    <!-- Parameter is right after a & sign //-->
          <xsl:value-of select="substring-after($url,concat('&amp;',$par,'='))" />
        </xsl:when>
        <xsl:otherwise>
                    <!-- The parameter is not specified //-->
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="contains($afterParam,'&amp;')">
                <!-- cut off other parameters -->
        <xsl:value-of select="substring-before($afterParam,'&amp;')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$afterParam" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

    <!--
        Template: UrlDelParam
        synopsis: Removes the parameter and value of a given parameter from a specific URL

        url: URL containing the parameter and value
        par: name of the parameter
    -->
  <xsl:template name="UrlDelParam">
    <xsl:param name="url" />
    <xsl:param name="par" />

    <xsl:choose>
      <xsl:when test="contains($url,concat($par,'='))">
                <!-- get value of par's value -->
        <xsl:variable name="valueOfPar">
                    <!-- cut off everything before value -->
          <xsl:variable name="valueBlured">
            <xsl:choose>
              <xsl:when test="contains($url,concat('?',$par,'=')) ">
                <xsl:value-of select="substring-after($url,concat('?',$par,'='))" />
              </xsl:when>
              <xsl:when test="contains($url,concat('&amp;',$par,'='))">
                <xsl:value-of select="substring-after($url,concat('&amp;',$par,'='))" />
              </xsl:when>
            </xsl:choose>
          </xsl:variable>
          <xsl:choose>
                        <!-- found value is not the last one in $url -->
            <xsl:when test="contains($valueBlured,'&amp;')">
              <xsl:value-of select="substring-before($valueBlured,'&amp;')" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$valueBlured" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:variable name="parAndVal">
          <xsl:value-of select="concat($par,'=',$valueOfPar)" />
        </xsl:variable>

        <xsl:choose>
                    <!-- more params append afterwards -->
          <xsl:when test="contains(substring-after($url,$parAndVal),'&amp;')">
            <xsl:choose>
                            <!-- $par is not the first in list -->
              <xsl:when
                test="contains(substring($url,string-length(substring-before($url,$parAndVal)+1),string-length(substring-before($url,$parAndVal)+1)),'&amp;')">
                <xsl:value-of select="concat(substring-before($url,concat('&amp;',$parAndVal)),substring-after($url,$parAndVal))" />
              </xsl:when>
                            <!-- $par is logical the first one in $url-->
              <xsl:otherwise>
                <xsl:value-of select="concat(substring-before($url,$parAndVal),substring-after($url,concat($parAndVal,'&amp;')))" />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
                    <!-- no more params append afterwards -->
          <xsl:otherwise>
            <xsl:value-of select="substring($url,1, (string-length($url)-(string-length($parAndVal)+1))) " />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$url" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

    <!--
        Template: ShortenText
        synopsis: Cuts text after a maximum of given chars but at end of the word that would be affected. If the text is shortened "..." is appended.
        param:

        text: the text to be shorten
        length: the number of chars
    -->
  <xsl:template name="ShortenText">
    <xsl:param name="text" />
    <xsl:param name="length" />
    <xsl:value-of select="mcrxml:shortenText($text, $length)" />
  </xsl:template>

    <!--
        Template: shortenPersonLabel
        synopsis: removes all Chrakters behind a '(' in label of persons (also works with al otrher Strings)
        param:

        text: the text to be shorten
    -->
  <xsl:template name="shortenPersonLabel">
    <xsl:param name="text" />
    <xsl:value-of select="mcrxml:shortenPersonLabel($text)" />
  </xsl:template>

    <!--
        Template: ClassCategLink
        synopsis: Generates a link to get a classification
        param:

        classid: classification id
        categid: category id
        host: host to query
    -->
  <xsl:template name="ClassCategLink">
    <xsl:param name="classid" />
    <xsl:param name="categid" />
    <xsl:param name="host" select="'local'" />
    <xsl:choose>
      <xsl:when test="$host != 'local' and string-length($host) &gt; 0">
        <xsl:value-of
          select="concat('mcrws:operation=MCRDoRetrieveClassification&amp;level=0&amp;type=children&amp;classid=',$classid,'&amp;categid=',$categid,'&amp;format=metadata','&amp;host=',$host)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat('classification:metadata:0:children:',$classid,':',$categid)" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

    <!--
        Template: ClassLink
        synopsis: Generates a link to get a classification
        param:

        classid: classification id
        host: host to query
    -->
  <xsl:template name="ClassLink">
    <xsl:param name="classid" />
    <xsl:param name="host" select="'local'" />
    <xsl:choose>
      <xsl:when test="$host != 'local'">
        <xsl:value-of
          select="concat('mcrws:operation=MCRDoRetrieveClassification&amp;level=-1&amp;type=children&amp;classid=',$classid,'&amp;format=metadata','&amp;host=',$host)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat('classification:metadata:-1:children:',$classid)" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
    <!--
        Template: PageGen
        synopsis: returns a list of links to access other pages of a result list

        parameters:
        i: running indicator - leave untouched
        id: editorID
        href: baselink to access resultlists
        size: how many results per page
        offset: start at which result offset
        currentpage: what is the current page displayed?
        totalpage: how many pages exist?
    -->
  <xsl:template name="PageGen">
        <!--
            MCRSearchServlet?mode=results&amp;id={@id}&amp;numPerPage={@numPerPage}&amp;page={number(@page)+1}
        -->
    <xsl:param name="i" select="1" />
    <xsl:param name="href" select="concat($ServletsBaseURL, 'MCRSearchServlet',$HttpSession,'?mode=results')" />
    <xsl:param name="id" />
    <xsl:param name="size" />
    <xsl:param name="currentpage" />
    <xsl:param name="totalpage" />
    <xsl:param name="style" />
    <xsl:variable name="PageWindowSize" select="10" />
        <!-- jumpSize is to determine the pages to be skipped -->
    <xsl:variable name="jumpSize">
      <xsl:choose>
                <!-- current printed page number is smaller than current displayed page
-->
        <xsl:when test="$i &lt; $currentpage">
          <xsl:choose>
                        <!-- This is to support a bigger PageWindow at the end of page listing and
                            to skip a jump of 2
-->
            <xsl:when
              test="(($totalpage - $PageWindowSize - 1) &lt;= $i) or
                        (($currentpage - floor(($PageWindowSize -1) div 2) - 1) = 2)">
              <xsl:value-of select="1" />
            </xsl:when>
                        <!-- This is to support a bigger PageWindow at the begin of page listing
-->
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
                        <!-- jump only one if your near currentpage,
                            or at last page
                            or to support bigger window at beginning
                            or to skip a jump of 2
-->
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
      <xsl:if test="$i=$currentpage">
        <xsl:text>[</xsl:text>
      </xsl:if>
             <!-- XSL.Style parameter in order to view search-results in a list -->
      <xsl:variable name="xslstyle">
        <xsl:if test="$style!=''">
          <xsl:value-of select="concat('&amp;XSL.Style=', $style)" />
        </xsl:if>
      </xsl:variable>
      <a href="{concat($href, '&amp;id=',$id,'&amp;page=',$i, '&amp;numPerPage=', $size, $xslstyle)}">
        <xsl:value-of select="$i" />
      </a>
      <xsl:if test="$i=$currentpage">
        <xsl:text>]</xsl:text>
      </xsl:if>
      <xsl:if test="$jumpSize &gt; 1">
        <xsl:text>&#160;...</xsl:text>
      </xsl:if>
      <xsl:text>&#160;
            </xsl:text>
      <xsl:call-template name="PageGen">
        <xsl:with-param name="i" select="$i + $jumpSize" />
        <xsl:with-param name="id" select="$id" />
        <xsl:with-param name="href" select="$href" />
        <xsl:with-param name="size" select="$size" />
        <xsl:with-param name="currentpage" select="$currentpage" />
        <xsl:with-param name="totalpage" select="$totalpage" />
        <xsl:with-param name="style" select="$style" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>
    <!-- Template typeOfObjectID
        synopsis: returns the type of the ObjectID submitted usally the second part of the ID

        parameters:
        id: MCRObjectID
    -->
  <xsl:template name="typeOfObjectID">
    <xsl:param name="id" />
    <xsl:variable name="delim" select="'_'" />
    <xsl:value-of select="substring-before(substring-after($id,$delim),$delim)" />
  </xsl:template>

    <!-- Template selectLang
        synopsis: returns $CurrentLang if $nodes[lang($CurrentLang)] is not empty, else $DefaultLang

        parameters:
        nodes: the nodeset to check
    -->
  <xsl:template name="selectLang">
    <xsl:param name="nodes" />
    <xsl:choose>
      <xsl:when test="$nodes[lang($CurrentLang)]">
        <xsl:value-of select="$CurrentLang" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$DefaultLang" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

    <!-- Template selectPresentLang
        synopsis: returns the result of selectLang if nodes for that language are present, else returns a language for which nodes a present

        parameters:
        nodes: the nodeset to check
    -->
  <xsl:template name="selectPresentLang">
    <xsl:param name="nodes" />
    <xsl:variable name="check">
      <xsl:call-template name="selectLang">
        <xsl:with-param name="nodes" select="$nodes" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$nodes[lang($check)]">
        <xsl:value-of select="$check" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$nodes[1]/@xml:lang" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
    <!-- =================================================================================================== -->
    <!--
        Template: getBrowserAddress
        synopsis: The template will be used to identify the currently selected menu entry and the belonging element item/@href in the navigationBase
        These strategies are embarked on:
        1. RequestURL - lang ?= @href - lang
        2. RequestURL - $WebApplicationBaseURL - lang ?= @href - lang
        3. Root element ?= item//dynamicContentBinding/rootTag
    -->

  <xsl:template name="getBrowserAddress">
    <xsl:variable name="RequestURL.sessionRemoved">
      <xsl:call-template name="UrlDeleteSession">
        <xsl:with-param name="url" select="$RequestURL" />
      </xsl:call-template>
    </xsl:variable>
        <!--remove $lastPage-->
    <xsl:variable name="RequestURL.lastPageDel">
      <xsl:call-template name="UrlDelParam">
        <xsl:with-param name="url" select="$RequestURL.sessionRemoved" />
        <xsl:with-param name="par" select="'XSL.lastPage.SESSION'" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="RequestURL.WebURLDel">
      <xsl:value-of select="concat('/',substring-after($RequestURL.lastPageDel,$WebApplicationBaseURL))" />
    </xsl:variable>
        <!--remove $lang -->
    <xsl:variable name="cleanURL">
      <xsl:call-template name="UrlDelParam">
        <xsl:with-param name="url" select="$RequestURL.lastPageDel" />
        <xsl:with-param name="par" select="'lang'" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="cleanURL2">
      <xsl:call-template name="UrlDelParam">
        <xsl:with-param name="url" select="$RequestURL.WebURLDel" />
        <xsl:with-param name="par" select="'lang'" />
      </xsl:call-template>
    </xsl:variable>

        <!-- 1. case -->
        <!-- test if navigation.xml contains the current browser address -->
    <xsl:variable name="browserAddress_href">
      <xsl:value-of select="$loaded_navigation_xml//item[(@href=$cleanURL2) or (@href=$cleanURL)]/@href" />
    </xsl:variable>
        <!-- 2. case -->
        <!-- TODO: -->
        <!-- remove this code and remove tag(s) <dynamicContentBinding/> from navigation.xml -->
        <!-- look for $browserAddress_dynamicContentBinding -->
    <xsl:variable name="browserAddress_dynamicContentBinding">
      <xsl:if test="  ($browserAddress_href = '') ">
                <!-- assign name of rootTag -> $rootTag -->
        <xsl:variable name="rootTag" select="name(*)" />
        <xsl:for-each select="$loaded_navigation_xml//dynamicContentBinding/rootTag">
          <xsl:if test=" current() = $rootTag ">
            <xsl:for-each select="ancestor-or-self::*[@href]">
              <xsl:if test="position()=last()">
                <xsl:value-of select="@href" />
              </xsl:if>
            </xsl:for-each>
          </xsl:if>
        </xsl:for-each>
      </xsl:if>
    </xsl:variable>

        <!-- assign right browser address -->
    <xsl:choose>
      <xsl:when test=" $browserAddress_href != '' ">
        <xsl:value-of select="$browserAddress_href" />
      </xsl:when>
      <xsl:when test=" $browserAddress_dynamicContentBinding != '' ">
        <xsl:value-of select="$browserAddress_dynamicContentBinding" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$loaded_navigation_xml/@hrefStartingPage" />
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>
    <!-- =================================================================================================== -->
  <xsl:template name="getTemplate">
    <xsl:param name="browserAddress" />
    <xsl:param name="navigationBase" />

    <xsl:variable name="template_tmp">
            <!-- point to rigth item -->
      <xsl:for-each select="$loaded_navigation_xml//item[@href = $browserAddress]">
                <!-- collect @template !='' entries along the choosen axis -->
        <xsl:if test="position()=last()">
          <xsl:for-each select="ancestor-or-self::*[  @template != '' ]">
            <xsl:if test="position()=last()">
              <xsl:value-of select="@template" />
            </xsl:if>
          </xsl:for-each>
        </xsl:if>
                <!-- END OF: collect @template !='' entries along the choosen axis -->
      </xsl:for-each>
            <!-- END OF: point to rigth item -->
    </xsl:variable>

    <xsl:choose>
            <!-- assign appropriate template -->
      <xsl:when test="$template_tmp != ''">
        <xsl:value-of select="$template_tmp" />
      </xsl:when>
            <!-- default template -->
      <xsl:otherwise>
        <xsl:value-of select="$loaded_navigation_xml/@template" />
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>
    <!-- =================================================================================================== -->
    <!--
        Template: formatISODate
        synopsis: formates the given date (ISO 8601) to the defined local format
        param:

        date: date in ISO 8601 format
        format: target format (must suit to SimpleDateFormat)
        locale: use local, e.g. "de" "en"
    -->
  <xsl:template name="formatISODate">
    <xsl:param name="date" />
    <xsl:param name="format" />
    <xsl:param name="locale" select="$CurrentLang" />
    <xsl:variable name="formatArg">
      <xsl:choose>
        <xsl:when test="string-length($format)&gt;0">
          <xsl:value-of select="$format" />
        </xsl:when>
        <xsl:when test="string-length(normalize-space($date))=4">
          <xsl:value-of select="i18n:translate('metaData.dateYear')" />
        </xsl:when>
        <xsl:when test="string-length(normalize-space($date))=7">
          <xsl:value-of select="i18n:translate('metaData.dateYearMonth')" />
        </xsl:when>
        <xsl:when test="string-length(normalize-space($date))=10">
          <xsl:value-of select="i18n:translate('metaData.dateYearMonthDay')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="i18n:translate('metaData.dateTime')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:value-of select="mcrxml:formatISODate( string( $date ),string( $formatArg ),string( $locale ) )" />
  </xsl:template>

    <!--
        Template: formatFileSize
        synopsis: formates the size in a more human readable form
        param:

        size: size in bytes
    -->
  <xsl:template name="formatFileSize">
    <xsl:param name="size" />
    <xsl:value-of xmlns:ifsnode="xalan://org.mycore.datamodel.ifs.MCRFilesystemNode" select="ifsnode:getSizeFormatted($size)" />
  </xsl:template>

    <!-- ====================================================================================={

        section: template name="ersetzen"

        Search for a part in a string and replace it

        parameters:
        vorlage - the original string
        raus - the searched string to replace
        rein - the new string

        }===================================================================================== -->

  <xsl:template name="ersetzen">
    <xsl:param name="vorlage" />
    <xsl:param name="raus" />
    <xsl:param name="rein" />
    <xsl:choose>
      <xsl:when test="contains($vorlage,$raus)">
        <xsl:value-of select="concat(substring-before($vorlage,$raus),$rein)" />
        <xsl:call-template name="ersetzen">
          <xsl:with-param name="vorlage" select="substring-after($vorlage,$raus)" />
          <xsl:with-param name="raus" select="$raus" />
          <xsl:with-param name="rein" select="$rein" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$vorlage" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

    <!-- =================================================================================== -->

    <!--
        Template: Tokenizer
        synopsis: splits a string to tokens elements when ever a delimiter occurs
        param:

        string: the string to split into token
        delimiter: a string that acts a token delimiter in "string"
    -->
  <xsl:template name="Tokenizer">
    <xsl:param name="string" />
    <xsl:param name="delimiter" select="' '" />
    <xsl:choose>
      <xsl:when test="$delimiter and contains($string, $delimiter)">
        <token>
          <xsl:value-of select="substring-before($string,$delimiter)" />
        </token>
        <xsl:call-template name="Tokenizer">
          <xsl:with-param name="string" select="substring-after($string,$delimiter)" />
          <xsl:with-param name="delimiter" select="$delimiter" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <token>
          <xsl:value-of select="$string" />
        </token>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

    <!-- ======================================================================================================== -->
    <!--
        Template: get.readAccess
        synopsis:
        Verifies read access for a given webpage.
        The webpage must be contained in navigation.xml as //item/@href
        params:
        webpage:            *//item/@href from navigation.xml
        blockerWebpage:     *//item/@href from navigation.xml that has already been verified for (write access =true).
        So, only the values of "items/@href" of the ancestor axis till and exclusiv $blockerWebpage
        will be verified.
    -->
  <xsl:template name="get.readAccess">
    <xsl:param name="webpage" />
    <xsl:param name="blockerWebpage" select="''" />
    <xsl:choose>
      <xsl:when test="$webpage=$browserAddress">
        <xsl:value-of select="$readAccess" />
      </xsl:when>
      <xsl:when test="$webpage!=''">
        <xsl:choose>
          <xsl:when test="$blockerWebpage!=''">
            <xsl:copy-of select="layoutUtils:readAccess($webpage,$blockerWebpage)" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:copy-of select="layoutUtils:readAccess($webpage)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="'false'" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

    <!-- ======================================================================================================== -->

  <xsl:template name="get.whiteList">
    <xsl:value-of select="concat($ServletsBaseURL,'MCRLoginServlet')" />
  </xsl:template>

    <!-- ======================================================================================================== -->
</xsl:stylesheet>