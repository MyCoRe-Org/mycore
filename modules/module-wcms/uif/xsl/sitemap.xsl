<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >

	<xsl:variable name="PageTitle">
		<xsl:value-of select="'Sitemap'"/>
	</xsl:variable>
	<xsl:include href="MyCoReLayout-de.xsl" />
	
	<!-- ================================================================================= -->
	<xsl:template match="sitemap">
		<xsl:param name="template" />
		<!-- temp. take care of template='template_docportal to provide old layout -->
		<xsl:choose>
			<xsl:when test="$template='template_docportal'">
				<div id="help" >
					<div class="resultcmd" style="padding-left:20px;">
						<table style="width:100%;" cellpadding="0" cellspacing="0">
							<tr style="height:20px;">
								<td class="resultcmd">
									<xsl:value-of select="$MainTitle"/>
								</td>
								<td class="resultcmd" style="text-align:right;padding-right:5px;vertical-align:bottom;">
									<xsl:value-of select="$PageTitle"/>
								</td>
							</tr>
						</table>
					</div>
					<div style="padding:20px;">
						<xsl:call-template name="sitemap.index" />
					</div>
				</div>
			</xsl:when>
			<!-- end of: temp. take care of template='template_docportal to provide old layout -->
			<xsl:otherwise>
				<xsl:call-template name="sitemap.index" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- ================================================================================= -->
	<xsl:template name="sitemap.index">
		<table class="sitemap" width="90%" border="0" cellspacing="0" cellpadding="0" align="center">
			<!-- general column widths definition -->
			<tr>
				<td height="0" width="5%"></td>
				<td width="40%"></td>
				<td width="10%"></td>
				<td width="40%"></td>
				<td width="5%"></td>
			</tr>
			<!-- END OF: general column widths definition -->
			<tr>
				<td colspan="5">
					<br/>
					<br/>
				</td>
			</tr>
			<!-- menu left -->
			<xsl:call-template name="createSitemap">
				<xsl:with-param name="myRootNode" 
					select="$loaded_navigation_xml/navi-main" />
				<xsl:with-param name="typeOfMenu" select="'tree'" />
			</xsl:call-template>
			<!-- END OF: menu left -->
			<tr>
				<td colspan="5">
					<br/>
					<br/>
				</td>
			</tr>
			<!-- menu on top -->
			<xsl:call-template name="createSitemap">
				<xsl:with-param name="myRootNode" 
					select="$loaded_navigation_xml/navigation/navi-below" />
				<xsl:with-param name="typeOfMenu" select="'horizontal'" />
			</xsl:call-template>
			<!-- END OF: menu on top -->
		</table>
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="createSitemap">
		<xsl:param name="myRootNode" />
		<xsl:param name="typeOfMenu" />
		<xsl:variable name="numerOfMainEntries" select="count($myRootNode/item)" />
		<!-- get maximal depth -> $depth -->
		<xsl:variable name="depth">
			<xsl:for-each select="$myRootNode//item">
				<xsl:sort select="count(ancestor-or-self::item)" data-type="number"/>
				<xsl:if test="position()=last()">
					<xsl:value-of select="count(ancestor-or-self::item)"/>
				</xsl:if>
			</xsl:for-each>
		</xsl:variable>
		<!-- END OF: get maximal depth -> $depth -->
		<!-- display name of menu -->
		<tr>
			<td></td>
			<th colspan="3">
				<xsl:value-of select="$myRootNode/@label" />
			</th>
			<td></td>
		</tr>
		<!-- END OF: display name of menu -->
		<xsl:choose>
			<!-- display tree -->
			<xsl:when test=" $typeOfMenu = 'tree' " >
				<tr>
					<td colspan="5">
						<br/>
					</td>
				</tr>
				<xsl:call-template name="forLoop.createSitemap">
					<xsl:with-param name="myRootNode" select="$myRootNode" />
					<xsl:with-param name="depth" select="$depth" />
					<!-- use pixel values -->
					<xsl:with-param name="startValue" select="1" />
					<xsl:with-param name="endValue" select="ceiling($numerOfMainEntries div 2)" />
					<xsl:with-param name="columnWidthIcon" select="'9'" />
					<xsl:with-param name="menuPointHeigth" select="'17'" />
					<!-- use pixel values -->
					<xsl:with-param name="borderWidthSides" select="'1'" />
					<!-- use percent values -->
					<xsl:with-param name="numerOfMainEntries" select="$numerOfMainEntries" />
				</xsl:call-template>
			</xsl:when>
			<!-- display horizontal version -->
			<xsl:otherwise>
				<!-- sub menu tree -->
				<tr valign="top">
					<td></td>
					<td>
						<xsl:for-each select="$myRootNode" >
							<xsl:call-template name="sitemap.createTree" >
								<xsl:with-param name="menuPointHeigth" select="'15'" />
								<!-- use pixel values -->
								<xsl:with-param name="depth" select="$depth" />
								<!-- use pixel values -->
								<xsl:with-param name="borderWidthSides" select="'1'" />
								<xsl:with-param name="columnWidthIcon" select="'7'" />
							</xsl:call-template>
						</xsl:for-each>
					</td>
					<td colspan="3"></td>
				</tr>
				<!-- END OF: sub menu tree -->
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="forLoop.createSitemap">
		<xsl:param name="startValue"/>
		<xsl:param name="endValue"/>
		<xsl:param name="columnWidthIcon"/>
		<xsl:param name="depth" />
		<xsl:param name="myRootNode" />
		<xsl:param name="menuPointHeigth" />
		<xsl:param name="borderWidthSides" />
		<xsl:param name="numerOfMainEntries" />
		<xsl:if test="$startValue &lt;= $endValue">
			<xsl:for-each select="$myRootNode/item" >
				<xsl:if test="position() = ($startValue*2)-1 " >
					<!-- display main menu name -->
					<tr>
						<!-- left column -->
						<td></td>
						<th class="mainMenuPoint"> [<xsl:value-of select="number($startValue)*2-1" />] <a 
							href="{concat($WebApplicationBaseURL,substring-after(@href,'/'))}" > <xsl:choose> <xsl:when 
							test="./label[lang($CurrentLang)] != ''"> <xsl:value-of select="./label[lang($CurrentLang)]" /> 
							</xsl:when> <xsl:otherwise> <xsl:value-of select="./label[lang($DefaultLang)]" /> </xsl:otherwise> 
							</xsl:choose> </a> </th>
						<td></td>
						<!-- right column -->
						<xsl:choose>
							<xsl:when test=" $startValue*2 &gt; $numerOfMainEntries " >
								<td></td>
							</xsl:when>
							<xsl:otherwise>
								<th class="mainMenuPoint"> [<xsl:value-of select="number($startValue)*2" />] <a 
									href="{concat($WebApplicationBaseURL,substring-after(following-sibling::item/@href,'/'))}" > 
									<xsl:choose> <xsl:when test="following-sibling::item/label[lang($CurrentLang)] != ''"> 
									<xsl:value-of select="following-sibling::item/label[lang($CurrentLang)]" /> </xsl:when> 
									<xsl:otherwise> <xsl:value-of select="following-sibling::item/label[lang($DefaultLang)]" /> 
									</xsl:otherwise> </xsl:choose> </a> </th>
							</xsl:otherwise>
						</xsl:choose>
						<td></td>
					</tr>
					<!-- END OF: display main menu name -->
					<!-- sub menu tree -->
					<tr valign="top">
						<td></td>
						<td>
							<xsl:call-template name="sitemap.createTree">
								<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth" />
								<!-- use pixel values -->
								<xsl:with-param name="depth" select="$depth" />
								<!-- use pixel values -->
								<xsl:with-param name="borderWidthSides" select="$borderWidthSides" />
								<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon" />
							</xsl:call-template>
						</td>
						<td></td>
						<xsl:choose>
							<xsl:when test=" $startValue*2 &gt; $numerOfMainEntries " >
								<td></td>
							</xsl:when>
							<xsl:otherwise>
								<xsl:for-each select="$myRootNode/item" >
									<xsl:choose>
										<xsl:when test="position() = ($startValue*2) " >
											<td>
												<xsl:call-template name="sitemap.createTree">
													<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth" />
													<!-- use pixel values -->
													<xsl:with-param name="depth" select="$depth" />
													<!-- use pixel values -->
													<xsl:with-param name="borderWidthSides" select="$borderWidthSides" />
													<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon" />
												</xsl:call-template>
											</td>
										</xsl:when>
									</xsl:choose>
								</xsl:for-each>
							</xsl:otherwise>
						</xsl:choose>
						<td></td>
					</tr>
					<!-- END OF: sub menu tree -->
				</xsl:if>
			</xsl:for-each>
			<tr>
				<td colspan="5">
					<br/>
				</td>
			</tr>
		</xsl:if>
		<!-- recursive recall -->
		<xsl:if test="$startValue &lt;= $endValue">
			<xsl:call-template name="forLoop.createSitemap">
				<xsl:with-param name="startValue">
					<xsl:value-of select="$startValue + 1"/>
				</xsl:with-param>
				<xsl:with-param name="endValue">
					<xsl:value-of select="$endValue"/>
				</xsl:with-param>
				<xsl:with-param name="columnWidthIcon">
					<xsl:value-of select="$columnWidthIcon"/>
				</xsl:with-param>
				<xsl:with-param name="myRootNode">
					<xsl:value-of select="$myRootNode"/>
				</xsl:with-param>
				<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth" />
				<xsl:with-param name="depth" select="$depth" />
				<xsl:with-param name="borderWidthSides" select="$borderWidthSides" />
				<xsl:with-param name="numerOfMainEntries" select="$numerOfMainEntries" />
			</xsl:call-template>
		</xsl:if>
		<!-- END OF: recursive recall -->
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="sitemap.createTree">
		<xsl:param name="depth" />
		<xsl:param name="menuPointHeigth" />
		<xsl:param name="borderWidthSides" />
		<xsl:param name="columnWidthIcon"/>
		<table width="100%" border="0" cellspacing="0" cellpadding="0" align="left">
			<!-- initialise columns -->
			<tr>
				<!-- border left -->
				<td width="{$borderWidthSides}%" height="0">
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title="" />
				</td>
				<!-- END OF: border left -->
				<!-- create columns to give space for the icons -->
				<xsl:call-template name="forLoop.createColumns">
					<xsl:with-param name="i" select="1" />
					<xsl:with-param name="count" select="$depth - 1" />
					<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon" />
				</xsl:call-template>
				<!-- END OF: create columns to give space for the icons -->
				<!-- fill rest -->
				<td width="{ 100 - (2*$borderWidthSides) - ($columnWidthIcon*($depth - 1)) }%">
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title="" />
				</td>
				<!-- END OF: fill rest -->
				<!-- border right -->
				<td width="{$borderWidthSides}%">
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title="" />
				</td>
				<!-- END OF: border right -->
			</tr>
			<!-- END OF: initialise columns -->
			<!-- read all items within this name space -->
			<xsl:for-each select="descendant::item">
				<!-- calculate kind of link to display the right icon -> $linkKind -->
				<xsl:variable name="linkKind">
					<xsl:choose>
						<!-- Does the current node have got childrens ? -->
						<xsl:when test=" count(child::item) &gt; 0 ">
							<xsl:value-of select="'popedUp'"/>
						</xsl:when>
						<!-- END OF: Does the current node have got childrens ? -->
						<xsl:otherwise>
							<xsl:value-of select="'normal'"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<!-- END OF: calculate kind of link to display the right icon -> $linkKind -->
				<xsl:call-template name="addMenuRow">
					<xsl:with-param name="linkKind" select="$linkKind"/>
					<xsl:with-param name="depth" select="$depth"/>
					<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth"/>
					<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon"/>
					<!--	   <xsl:with-param name="createSiteMap" select="'yes'"/> 	 	     	  -->
				</xsl:call-template>
			</xsl:for-each>
			<!-- END OF: read all items within this name space -->
		</table>
	</xsl:template>
	<!-- ================================================================================= -->
</xsl:stylesheet>