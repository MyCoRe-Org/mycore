<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.9 $ $Date: 2007-10-15 09:58:16 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:template match="classificationBrowser">
  <div class="classificationBrowser">
    <script type="text/javascript" src="/javascript/prototype.js"></script>
    <script language="JavaScript">
      /* <![CDATA[ */
     
      function toogle(classifID,categID)
      {
        var childrenID = 'cbChildren_' + classifID + '_' + categID;
        var button = document.getElementById( 'cbButton_' + classifID + '_' + categID )
        var children = document.getElementById( childrenID );
        
        if( button.value == '-' )
        {
          button.value = '+';
          children.className='cbHidden';
          children.innerHTML = '';
        }
        else
        {
          button.value = '-';
          children.className='cbVisible';
          new Ajax.Updater( childrenID, '/servlets/ClassificationBrowser', 
          { parameters: { 
            classification : classifID,
            category : categID
          } } );      
        } 
      }
      /* ]]> */
    </script>
    
    <xsl:variable name="id" select="generate-id(.)" />
    
    <div id="{$id}" class="cbVisible">
      <script language="JavaScript">
        new Ajax.Updater( '<xsl:value-of select="$id" />', '/servlets/ClassificationBrowser', 
        { parameters : { 
          classification : '<xsl:value-of select="@classification" />', 
          category       : '<xsl:value-of select="@category" />' 
        } } );
      </script>
    </div>
  </div>
</xsl:template>

</xsl:stylesheet>
