<?xml version="1.0" encoding="ISO-8859-1" ?>

<xsl:stylesheet 
	version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" >

<!-- ======================================================================================================== -->	
<xsl:template name="getFastWCMS">	
	
	<!--BEGIN: Adminmenu-->
	<xsl:variable name="servletAnswer_XML_tmp" 
		select="concat('request:servlets/WCMSLoginServlet',$JSessionID,'?XSL.Style=xml&amp;flag=true')"
    />

	<xsl:variable name="servletAnswer_XML" 
		select="document($servletAnswer_XML_tmp)"
    />
	
     <xsl:if test="$servletAnswer_XML/cms/modus/text() = 'true'">
		
		<!-- get return address for deleting -->	
		<xsl:variable name="href" >
			<xsl:for-each select="$loaded_navigation_xml//item[@href=$browserAddress]" >
				<xsl:value-of select="parent::node()/@href" />
			</xsl:for-each>
		</xsl:variable>
		
		<xsl:variable name="deletepath">
			<xsl:choose>
				<xsl:when test="$href != '' ">
					<xsl:value-of select="$href"/>
				</xsl:when>
		    	<xsl:otherwise>
				    <xsl:value-of select="$loaded_navigation_xml/@hrefStartingPage"/>
				</xsl:otherwise>
			</xsl:choose>	
		</xsl:variable>
		
		
		<xsl:variable name="allowed">
			
			<xsl:for-each select="$loaded_navigation_xml//item[@href=$browserAddress]">
				
				<xsl:for-each select="ancestor-or-self::node()">
					
					<xsl:variable name="selfhref">
						<xsl:value-of select="./@href"/>
					</xsl:variable>
					<xsl:variable name="selfnohref">
						<xsl:value-of select="name(.)"/>
					</xsl:variable>
					
					<xsl:choose>
						<xsl:when test="$selfhref!=''">
							<xsl:for-each select="$servletAnswer_XML/cms/rootNode/text()">
				   				<xsl:variable name="rootN">
									<xsl:value-of select="."/>
								</xsl:variable>
								<xsl:if test="$rootN=$selfhref">
								   	<xsl:value-of select="'true'" />   	
								</xsl:if>
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<xsl:for-each select="$servletAnswer_XML/cms/rootNode/text()">
				   				<xsl:variable name="rootN">
									<xsl:value-of select="."/>
								</xsl:variable>
								<xsl:if test="$rootN=$selfnohref">
								   	<xsl:value-of select="'true'" />   	
								</xsl:if>
							</xsl:for-each>
						</xsl:otherwise>
					</xsl:choose>
					
				</xsl:for-each>	
								
			</xsl:for-each>
			
		</xsl:variable>
				
	
		<xsl:if test="$allowed = 'true'">	
			    <div style="text-align: right;">
					<xsl:variable name="edit">
						<xsl:value-of select="i18n:translate('wcms.action.editContent')"/>	
					</xsl:variable>
					<xsl:variable name="addIntern_before">
						<xsl:value-of select="i18n:translate('wcms.action.insertMenuBefore')"/>	
					</xsl:variable>
					<xsl:variable name="addIntern_sub">
						<xsl:value-of select="i18n:translate('wcms.action.submenuInsert')"/>	
					</xsl:variable>
					<xsl:variable name="addIntern_after">
						<xsl:value-of select="i18n:translate('wcms.action.insertMenuAfter')"/>	
					</xsl:variable>
					<xsl:variable name="addExtern_before">
						<xsl:value-of select="i18n:translate('wcms.action.insertLinkMenuBefore')"/>	
					</xsl:variable>
					<xsl:variable name="addExtern_sub">
						<xsl:value-of select="i18n:translate('wcms.action.submenuLinkInsert')"/>	
					</xsl:variable>
					<xsl:variable name="addExtern_after">
						<xsl:value-of select="i18n:translate('wcms.action.insertLinkMenuAfter')"/>	
					</xsl:variable>
					<xsl:variable name="delete">
						<xsl:value-of select="i18n:translate('wcms.delete')"/>	
					</xsl:variable>
					<xsl:variable name="translate">
						<xsl:value-of select="i18n:translate('wcms.action.translate')"/>	
					</xsl:variable>					
					<xsl:choose>
						<xsl:when test="$DefaultLang=$CurrentLang">
						    <img src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/fastwcms/menu.gif" href="" 
							   onClick="return clickreturnvalue()" 
								onMouseover="dropdownmenu(this, event, menu1, '100px', '{$WebApplicationBaseURL}', '{$template}', '{$browserAddress}', '{$deletepath}', '{$edit}', '{$addIntern_before}', '{$addIntern_sub}', '{$addIntern_after}', '{$addExtern_before}', '{$addExtern_sub}', '{$addExtern_after}', '{$delete}', '{$translate}')" onMouseout="delayhidemenu()"/>							
						</xsl:when>
						<!--translate-->
						<xsl:otherwise>
						    <img src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/fastwcms/menu.gif" href="" 
							   onClick="return clickreturnvalue()" 
								onMouseover="dropdownmenu(this, event, menu2, '100px', '{$WebApplicationBaseURL}', '{$template}', '{$browserAddress}', '{$deletepath}', '{$edit}', '{$addIntern_before}', '{$addIntern_sub}', '{$addIntern_after}', '{$addExtern_before}', '{$addExtern_sub}', '{$addExtern_after}', '{$delete}', '{$translate}')" onMouseout="delayhidemenu()"/>
						</xsl:otherwise>
					</xsl:choose>
					
			    </div>
		</xsl:if>
    </xsl:if>
    <!--END OF: Adminmenu-->
		
</xsl:template>	
	
</xsl:stylesheet>