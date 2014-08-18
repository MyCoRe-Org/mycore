<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
  XSL to include a classification browser into any MyCoReWebPage.

  Usage:
    <classificationbrowser
      classification="{ClassificationID}"
      category="{CategoryID to start with, optional}"
      sortby="{id|label, optional sort order of categories}"
      objecttype="{MCRObject type, optional}"
      field="{search field for category queries in this classification}"
      restriction="{additional query expression}"
      filterCategory="{true|false, if true join returnId to id, ignored with lucene, default true}"
      parameters="{additional MCRSearchServlet parameters}"
      addParameter="{any additional request parameters to forward to classification browser servlet}"
      countresults="{true|false, default false, whether to execute queries to count results}"
      countlinks="{true|false, default false, whether to count links to each category}"
      emptyleaves="{true|false, when false and counting activated, skip empty leaves}"
      adduri="{true|false, whether to include URI from classification data}"
      adddescription="{true|false, whether to include description from category label}"
      addclassid="{true|false, adds classification ID to category, default false}"
      class="{CSS class, default is 'classificationBrowser'}"
      style="{XSL.Style to use, default is classificationBrowserData.xsl}"
    />
 -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="RequestURL" />
  <xsl:param name="HttpSession" />
  <xsl:param name="MCR.Ajax.LoadingImage" />
  <xsl:param name="MCR.classbrowser.Servlet" select="'ClassificationBrowser'"/>

  <xsl:template match="classificationbrowser">
    <xsl:call-template name="mcrClassificationBrowser">
      <xsl:with-param name="class" select="@class"/>
      <xsl:with-param name="classification" select="@classification"/>
      <xsl:with-param name="category" select="@category"/>
      <xsl:with-param name="sortby" select="@sortby"/>
      <xsl:with-param name="objecttype" select="@objecttype"/>
      <xsl:with-param name="field" select="@field"/>
      <xsl:with-param name="parameters" select="@parameters"/>
      <xsl:with-param name="restriction" select="@restriction"/>
      <xsl:with-param name="countresults" select="@countresults"/>
      <xsl:with-param name="countlinks" select="@countlinks"/>
      <xsl:with-param name="emptyleaves" select="@emptyleaves"/>
      <xsl:with-param name="adduri" select="@adduri"/>
      <xsl:with-param name="adddescription" select="@adddescription"/>
      <xsl:with-param name="addclassid" select="@addclassid"/>
      <xsl:with-param name="style" select="@style"/>
      <xsl:with-param name="filterCategory" select="@filterCategory='true'"/>
      <xsl:with-param name="addParameter" select="@addParameter"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="mcrClassificationBrowser">
    <xsl:param name="class" />
    <xsl:param name="classification" />
    <xsl:param name="category" />
    <xsl:param name="sortby" />
    <xsl:param name="objecttype" />
    <xsl:param name="field" />
    <xsl:param name="parameters" />
    <xsl:param name="restriction" />
    <xsl:param name="countresults" />
    <xsl:param name="countlinks" />
    <xsl:param name="emptyleaves" />
    <xsl:param name="adduri" />
    <xsl:param name="adddescription" />
    <xsl:param name="addclassid" />
    <xsl:param name="style" />
    <!-- SOLR: if true join returnId to id, ignored with lucene -->
    <xsl:param name="filterCategory" select="false()"/>
    <xsl:param name="addParameter" />

    <div>
      <xsl:attribute name="class">
        <xsl:choose>
          <xsl:when test="string-length($class) &gt; 0">
            <xsl:value-of select="$class" />
          </xsl:when>
          <xsl:otherwise>classificationBrowser</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>

      <!-- jQuery -->
      <script type="text/javascript">
        /* &lt;![CDATA[ */

        var openCategs=[];

        function update(elementID,categID, f) {
          <xsl:if test="string-length($MCR.Ajax.LoadingImage) &gt; 0">
            var loaderImage = '&lt;img class="loading" src="<xsl:value-of select="concat($WebApplicationBaseURL,$MCR.Ajax.LoadingImage)"/>" /&gt;';
            jQuery(document.getElementById(elementID)).html(loaderImage);
          </xsl:if>

          var requestParam = {
            classification : '<xsl:value-of select="$classification" />',
            category       : categID,
            sortby         : '<xsl:value-of select="$sortby" />',
            objecttype     : '<xsl:value-of select="$objecttype" />',
            field          : '<xsl:value-of select="$field" />',
            parameters     : '<xsl:value-of select="$parameters" />',
            restriction    : '<xsl:value-of select="$restriction" />',
            countresults   : '<xsl:value-of select="$countresults" />',
            countlinks     : '<xsl:value-of select="$countlinks" />',
            emptyleaves    : '<xsl:value-of select="$emptyleaves" />',
            adduri         : '<xsl:value-of select="$adduri" />',
            adddescription : '<xsl:value-of select="$adddescription" />',
            addclassid     : '<xsl:value-of select="$addclassid" />',
            style          : '<xsl:value-of select="$style" />',
            filterCategory : '<xsl:value-of select="$filterCategory" />',
            webpage        : '<xsl:value-of select="substring-after($RequestURL,$WebApplicationBaseURL)" />'
          };
          <xsl:variable name="addParam">
            <xsl:if test="string-length($addParameter)&gt;0">
              <xsl:value-of select="concat('?',$addParameter)"/>
            </xsl:if>
          </xsl:variable>
          jQuery(document.getElementById(elementID)).load('<xsl:value-of select="concat($ServletsBaseURL,$MCR.classbrowser.Servlet,$HttpSession,$addParam)" />', requestParam, f );
        }

        function toogle(categID, closedImageURL, openImageURL) {
          var childrenID = 'cbChildren_<xsl:value-of select="$classification" />_' + categID;
          var button = document.getElementById( 'cbButton_<xsl:value-of select="$classification" />_' + categID );
          var children = document.getElementById( childrenID );

          if( button.value == '-' ) {
            button.value = '+';
            if (button.type == 'image' &amp;&amp; closedImageURL.length > 0){
              button.src=closedImageURL;
            }
            children.className='cbHidden';
            children.innerHTML = '';
            openCategs=jQuery.grep(openCategs,function(value){
              return value != categID;
            });
          }
          else {
            button.value = '-';
            if (button.type == 'image' &amp;&amp; openImageURL.length > 0){
              button.src=openImageURL;
            }
            children.className='cbVisible';
            update( childrenID, categID );
            openCategs.push(categID);
          }
          window.location.href=addState(window.location.href);
        }

        function toggleClass(categID, closedClass, openClass){
          var childrenID = 'cbChildren_<xsl:value-of select="$classification" />_' + categID;
          var button = document.getElementById( 'cbButton_<xsl:value-of select="$classification" />_' + categID );
          var children = document.getElementById( childrenID );
          var jButton=jQuery(button);
          jButton.toggleClass(closedClass);
          jButton.toggleClass(openClass);
          if (jButton.data("open")==true){
            children.className='cbHidden';
            children.innerHTML = '';
            openCategs=jQuery.grep(openCategs,function(value){
              return value != categID;
            });
            jButton.data("open", false);
          } else {
            children.className='cbVisible';
            update( childrenID, categID );
            openCategs.push(categID);
            jButton.data("open", true);
          }
        }

        function addState(url){
          var state=(openCategs.length>0)? "#open"+escape('["'+openCategs.join('","')+'"]') :"#open[]";
          var pos=url.indexOf("#open");
          if (pos>0){
            return url.substring(0,pos)+state;
          } else {
            return url+=state;
          }
        }

        function startSearch(baseURL, query, mask, parameters){
          mask=escape(addState(unescape(mask)));
          window.location.href=baseURL+((baseURL.indexOf("?") == -1)?"?query=":"")+query+"&amp;mask="+mask+"&amp;"+parameters;
          return false;
        }

        function loadState() {
          var pos=window.location.href.indexOf("#open");
          if (pos>0){
            var openTree=jQuery.parseJSON(unescape(window.location.href.substring(pos+5,window.location.href.length)));
            jQuery.each(openTree, function(index, value){
              jQuery('#cbButton_<xsl:value-of select="$classification" />_' + value).click();
            });
          }
        }

        /* ]]&gt; */
      </script>

      <xsl:variable name="id" select="generate-id(.)" />
      <div id="{$id}" class="cbVisible">
        <script type="text/javascript">
          update('<xsl:value-of select="$id" />','<xsl:value-of select="$category" />', loadState);
        </script>
      </div>
    </div>
  </xsl:template>
</xsl:stylesheet>