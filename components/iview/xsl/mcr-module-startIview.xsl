<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision: 1.7 $ $Date: 2006-09-07 14:16:58 $ -->
<!--  												-->
<!-- Image Viewer - MCR-IView 1.0, 05-2006  		-->
<!-- +++++++++++++++++++++++++++++++++++++			-->
<!--  												-->
<!-- Andreas Trappe 	- concept, devel. in misc.  -->
<!-- Britta Kapitzki	- Design					-->
<!-- Thomas Scheffler   - html prototype		    -->
<!-- Stephan Schmidt 	- html prototype			-->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xlink="http://www.w3.org/1999/xlink" exclude-result-prefixes="xlink" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">

<xsl:include href="mcr-module-iview.xsl" />
<!--
						modes to start MCRIView:	
							a) XSL - embedd MCRIView in XSL  by
									<xsl:call-template name="iview">
										<xsl:param name="derivID" /> name of ownerID
										<xsl:param name="pathOfImage" /> path to image
										<xsl:param name="height" /> height of viewer (or image in case display = image)
										<xsl:param name="width" /> width of viewer (or image in case display = image)
										<xsl:param name="scaleFactor" /> fitToWidth | fitToScreen | 0.1 <=> 1.0
										<xsl:param name="display" /> thumbnail | minimal | normal | extended
										<xsl:param name="style" /> only necessary if $display!='thumbnail' -> image | thumbnail | text
									</xsl:call-teamplate>
							b) XML - MyCoReWebpage
								<iview
									@derivid (name of ownerID)
									@pathofimage (path to image)
									@height (height of viewer (or image in case display = image) )
									@width (width of viewer (or image in case display = image) )
									@scalefactor ( fitToWidth | fitToScreen | 0.1 <=> 1.0 )
									@display (thumbnail | minimal | normal | extended )
									@style (only necessary if $display!='thumbnail' -> image | thumbnail | text) 
									>
								</iview>			
-->	
	
        <xsl:variable xmlns:encoder="xalan://java.net.URLEncoder" name="lastEmbeddedURL" 
			select="encoder:encode( string( $RequestURL ) )" />	
	
	<!-- ======================================================================== -->
	<xsl:template name="iview">
		<xsl:param name="derivID" />
		<xsl:param name="pathOfImage" />
		<xsl:param name="height" />
		<xsl:param name="width" />
		<xsl:param name="scaleFactor" />						
		<xsl:param name="display" />	
		<xsl:param name="style" />			
		
		<xsl:choose>
			<xsl:when test="$display='thumbnail'">
				<xsl:call-template name="iview.getEmbedded.thumbnail" >
					<xsl:with-param name="derivID" select="$derivID"/>
					<xsl:with-param name="pathOfImage" select="$pathOfImage"/>
					<xsl:with-param name="height" select="$height"/>
					<xsl:with-param name="width" select="$width"/>
					<xsl:with-param name="scaleFactor" select="$scaleFactor"/>						
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="iview.getEmbedded.iframe" >
					<xsl:with-param name="derivID" select="$derivID"/>
					<xsl:with-param name="pathOfImage" select="$pathOfImage"/>
					<xsl:with-param name="height" select="$height"/>
					<xsl:with-param name="width" select="$width"/>
					<xsl:with-param name="scaleFactor" select="$scaleFactor"/>						
					<xsl:with-param name="display" select="$display"/>	
					<xsl:with-param name="style" select="$style"/>			
				</xsl:call-template>				
			</xsl:otherwise>
		</xsl:choose>
			
	</xsl:template>
	<!-- ======================================================================== -->
	<xsl:template match="iview|IVIEW">
		<xsl:call-template name="iview">
			<xsl:with-param name="derivID" select="@derivid" />
			<xsl:with-param name="pathOfImage" select="@pathofimage" />
			<xsl:with-param name="height" select="@height" />
			<xsl:with-param name="width" select="@width" />
			<xsl:with-param name="scaleFactor" select="@scalefactor" />						
			<xsl:with-param name="display" select="@display" />			
			<xsl:with-param name="style" select="@style" />									
		</xsl:call-template>
	</xsl:template>
	<!-- ======================================================================== -->
	<xsl:template name="iview.getEmbedded.thumbnail" >
		<xsl:param name="derivID" />
		<xsl:param name="pathOfImage" />		
		
		<img src="{concat($iview.home,$derivID,$pathOfImage,$HttpSession,'?mode=getImage&amp;XSL.MCR.Module-iview.navi.zoom=thumbnail')}" />		
		
	</xsl:template>
	
	<!-- ======================================================================== -->

	<xsl:template name="iview.getEmbedded.iframe" >
		<xsl:param name="derivID" />
		<xsl:param name="pathOfImage" />
		<xsl:param name="height" />
		<xsl:param name="width" />
		<xsl:param name="scaleFactor" />						
		<xsl:param name="display" />	
		<xsl:param name="style" />		
		
		<iframe marginheight="0" marginwidth="0" frameborder="0" 
			src="{concat($iview.home,$derivID,$pathOfImage,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.navi.zoom.SESSION=',$scaleFactor,'&amp;XSL.MCR.Module-iview.display.SESSION=',$display,'&amp;XSL.MCR.Module-iview.style.SESSION=',$style,'&amp;XSL.MCR.Module-iview.lastEmbeddedURL.SESSION=',$lastEmbeddedURL,'&amp;XSL.MCR.Module-iview.embedded.SESSION=true&amp;XSL.MCR.Module-iview.move=reset')}" 
			name="iview" width="{$width}" height="{$height}" align="left">
		  <p><xsl:value-of select="i18n:translate('iview.error')"/></p>
		</iframe>		
		
	</xsl:template>

	<!-- ======================================================================== -->	
	<xsl:template name="iview.getAddress">
		<xsl:param name="derivID" />
		<xsl:param name="pathOfImage"/>		
		<xsl:param name="height" />
		<xsl:param name="width" />
		<xsl:param name="scaleFactor" />						
		<xsl:param name="display" />	
		<xsl:param name="style" />		
				
		   <xsl:value-of select="concat($iview.home,$derivID,$pathOfImage,$HttpSession,'?mode=generateLayout&amp;XSL.MCR.Module-iview.navi.zoom.SESSION=',$scaleFactor,'&amp;XSL.MCR.Module-iview.display.SESSION=',$display,'&amp;XSL.MCR.Module-iview.style.SESSION=',$style,'&amp;XSL.MCR.Module-iview.lastEmbeddedURL.SESSION=',$lastEmbeddedURL,'&amp;XSL.MCR.Module-iview.embedded.SESSION=false&amp;XSL.MCR.Module-iview.move=reset')" />
		
	</xsl:template>
	<!-- ======================================================================== -->	
	<xsl:template name="iview.getSupport">
		<xsl:param name="derivID" />
		    <xsl:value-of select="document(concat($iview.home,$derivID,$JSessionID,'?mode=getMetadata&amp;type=support&amp;XSL.Style=xml'))/mcr-module/support/@mainFile"/>
	</xsl:template>
	<!-- ======================================================================== -->		

</xsl:stylesheet>
