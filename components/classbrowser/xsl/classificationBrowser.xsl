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
      parameters="{additional MCRSearchServlet parameters}"
      countresults="{true|false, default false, whether to execute queries to count results}"
      countlinks="{true|false, default false, whether to count links to each category}"
      emptyleaves="{true|false, when false and counting activated, skip empty leaves}"
      adduri="{true|false, whether to include URI from classification data}" 
      adddescription="{true|false, whether to include description from category label}" 
      class="{CSS class, default is 'classificationBrowser'}" 
      style="{XSL.Style to use, default is classificationBrowserData.xsl}" 
    />
 -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:param name="WebApplicationBaseURL" />
<xsl:param name="ServletsBaseURL" />
<xsl:param name="RequestURL" />

<xsl:template match="classificationbrowser">
  <div>
    <xsl:attribute name="class">
      <xsl:choose>
        <xsl:when test="string-length(@class) &gt; 0">
          <xsl:value-of select="@class" />
        </xsl:when>
        <xsl:otherwise>classificationBrowser</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
    
    <script type="text/javascript" src="{$WebApplicationBaseURL}javascript/prototype.js"></script>
    <script language="JavaScript">
      /* &lt;![CDATA[ */
      
      function update(elementID,categID) {
        new Ajax.Updater( elementID, '<xsl:value-of select="concat($ServletsBaseURL,'ClassificationBrowser')" />', 
        { parameters: { 
          "XSL.template" : '<xsl:value-of select="$template" />',
          classification : '<xsl:value-of select="@classification" />',
          category       : categID,
          sortby         : '<xsl:value-of select="@sortby" />',
          objecttype     : '<xsl:value-of select="@objecttype" />',
          field          : '<xsl:value-of select="@field" />',
          parameters     : '<xsl:value-of select="@parameters" />',
          restriction    : '<xsl:value-of select="@restriction" />',
          countresults   : '<xsl:value-of select="@countresults" />',
          countlinks     : '<xsl:value-of select="@countlinks" />',
          emptyleaves    : '<xsl:value-of select="@emptyleaves" />',
          adduri         : '<xsl:value-of select="@adduri" />',
          adddescription : '<xsl:value-of select="@adddescription" />',
          style          : '<xsl:value-of select="@style" />',
          webpage        : '<xsl:value-of select="substring-after($RequestURL,$WebApplicationBaseURL)" />'
        } } );      
      }
     
      function toogle(categID, closedImageURL, openImageURL) {
        var childrenID = 'cbChildren_<xsl:value-of select="@classification" />_' + categID;
        var button = document.getElementById( 'cbButton_<xsl:value-of select="@classification" />_' + categID );
        var children = document.getElementById( childrenID );
        
        if( button.value == '-' ) {
          button.value = '+';
          if (button.type == 'image' &amp;&amp; closedImageURL.length > 0){
            button.src=closedImageURL;
          }
          children.className='cbHidden';
          children.innerHTML = '';
        }
        else {
          button.value = '-';
          if (button.type == 'image' &amp;&amp; openImageURL.length > 0){
            button.src=openImageURL;
          }
          children.className='cbVisible';
          update( childrenID, categID );
        } 
      }

      /* ]]&gt; */
    </script>
    
    <xsl:variable name="id" select="generate-id(.)" />
    <div id="{$id}" class="cbVisible">
      <script language="JavaScript">
        update('<xsl:value-of select="$id" />','<xsl:value-of select="@category" />');
      </script>
    </div>
  </div>
</xsl:template>

</xsl:stylesheet>
