<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	
	<!-- ================================================================================= -->
	<xsl:template match="TOC | toc">
		<xsl:param name="browserAddress" />
		<xsl:for-each 
			select="document($navigationBase)/navigation//item[@href=$browserAddress]">
			<xsl:for-each select="child::item">
				<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/greenArrow.gif" />
				<xsl:call-template name="addLink"/>
				<br/>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="HistoryNavigationRow">
		<xsl:param name="browserAddress" />
		<xsl:param name="CSSLayoutClass" />
		<!-- get href of starting page -->
		<xsl:variable name="hrefStartingPage" 
			select="document($navigationBase)/navigation/@hrefStartingPage" />
		<!-- END OF: get href of starting page -->
		<table class="{$CSSLayoutClass}" width="100%" border="0" cellspacing="0" cellpadding="0">
			<tr>
				<td>
					Navigation:
					<xsl:for-each 
						select="document($navigationBase)/navigation//item[@href]">
						<xsl:if test="@href = $browserAddress ">
							<a href="{ concat($WebApplicationBaseURL,substring-after($hrefStartingPage,'/') )}" >
								<xsl:value-of select="$MainTitle" />
							</a>
							<xsl:for-each select="ancestor-or-self::item">
								<xsl:if test="$browserAddress != $hrefStartingPage " > > <xsl:choose> <xsl:when 
									test="position() != last()"> <a 
									href="{concat($WebApplicationBaseURL,substring-after(@href,'/'))}" > <xsl:choose> <xsl:when 
									test="./label[lang($CurrentLang)] != ''"> <xsl:value-of select="./label[lang($CurrentLang)]" 
									/> </xsl:when> <xsl:otherwise> <xsl:value-of select="./label[lang($DefaultLang)]" /> 
									</xsl:otherwise> </xsl:choose> </a> </xsl:when> <xsl:otherwise> <xsl:choose> <xsl:when 
									test="./label[lang($CurrentLang)] != ''"> <xsl:value-of select="./label[lang($CurrentLang)]" 
									/> </xsl:when> <xsl:otherwise> <xsl:value-of select="./label[lang($DefaultLang)]" /> 
									</xsl:otherwise> </xsl:choose> </xsl:otherwise> </xsl:choose> </xsl:if>
							</xsl:for-each>
						</xsl:if>
					</xsl:for-each>
				</td>
			</tr>
		</table>
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="NavigationRow">
		<xsl:param name="rootNode" />
		<xsl:param name="CSSLayoutClass" />
		<xsl:param name="menuPointHeigth" />
		<!-- use pixel values -->
		<xsl:param name="spaceBetweenLinks" />
		<!-- use pixel values -->
		<xsl:param name="browserAddress" />
		<!-- table navigation row -->
		<table class="{$CSSLayoutClass}" border="0" cellspacing="0" cellpadding="0" align="center">
			<tr>
				<!-- read xml item entries and create the link bar -->
				<xsl:for-each select="$rootNode/item">
					<td>
						<xsl:choose>
							<xsl:when test="current()[@href = $browserAddress ]">
								<span class="marked">
									<xsl:call-template name="addLink" />
								</span>
							</xsl:when>
							<xsl:otherwise>
								<xsl:call-template name="addLink" />
							</xsl:otherwise>
						</xsl:choose>
					</td>
					<xsl:if test="position() != last()">
						<td>
							<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="{number($spaceBetweenLinks) div 2}" height="1" 
								border="0" alt=""></img>
						</td>
						<td>|</td>
						<td>
							<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="{number($spaceBetweenLinks) div 2}" height="1" 
								border="0" alt=""></img>
						</td>
					</xsl:if>
				</xsl:for-each>
				<!-- END OF: read xml item entries and create the link bar -->
				<td>
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="{number($spaceBetweenLinks) div 2}" height="1" border="0" alt=""></img>
				</td>
				<td>|</td>
				<td>
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="{number($spaceBetweenLinks) div 2}" height="1" border="0" alt=""></img>
				</td>
				<td>
					<xsl:call-template name="generateFlagButton"/>
				</td>
				<td width="10px"></td>
			</tr>
		</table>
		<!-- END OF: table navigation row -->
	</xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="NavigationTree">
		<xsl:param name="rootNode" />
		<xsl:param name="browserAddress" />
		<xsl:param name="CSSLayoutClass" />
		<xsl:param name="menuPointHeigth" />
		<!-- use pixel values -->
		<xsl:param name="columnWidthIcon" />
		<!-- use percent values -->
		<xsl:param name="spaceBetweenMainLinks" />
		<!-- use pixel values -->
		<xsl:param name="borderWidthTopDown" />
		<!-- use pixel values -->
		<xsl:param name="borderWidthSides" />
		<!-- use percent values -->
		<!-- get maximal depth -> $depth -->
		<xsl:variable name="depth">
			<xsl:for-each select="$rootNode//item">
				<xsl:sort select="count(ancestor-or-self::item)" data-type="number"/>
				<xsl:if test="position()=last()">
					<xsl:value-of select="count(ancestor-or-self::item)"/>
				</xsl:if>
			</xsl:for-each>
		</xsl:variable>
		<!-- END OF: get maximal depth -> $depth -->
		<!-- look for appropriate replaceMenu entry and assign-->
		<xsl:variable name="subRootNode" >
			<xsl:for-each select="$rootNode//item[@href = $browserAddress]" >
				<!-- collect @href's with replaceMenu="true" entries along an axis -->
				<xsl:for-each select="ancestor-or-self::item[  @replaceMenu = 'true' ]">
					<xsl:if test="position()=last()" >
						<xsl:value-of select="@href" />
					</xsl:if>
				</xsl:for-each>
				<!-- END OF: collect @href's with replaceMenu="true" entries along an axis -->
			</xsl:for-each>
		</xsl:variable>
		<!-- END OF: look for appropriate replaceMenu entry and assign -->
		<!-- general table -->
		<table width="100%" border="0" cellspacing="0" cellpadding="0" class="{$CSSLayoutClass}">
			<!-- initialise columns -->
			<tr>
				<!-- border left -->
				<td width="{$borderWidthSides}%" height="1">
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
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
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
				</td>
				<!-- END OF: fill rest -->
				<!-- border right -->
				<td width="{$borderWidthSides}%">
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
				</td>
				<!-- END OF: border right -->
			</tr>
			<!-- END OF: initialise columns -->
			<!-- borderWidthTop -->
			<tr>
				<td height="{$borderWidthTopDown}" colspan="{$depth + 2}">
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
				</td>
			</tr>
			<!-- END OF: borderWidthTop -->
			<!-- navigation tree -->
			<xsl:choose>
				<xsl:when test=" $subRootNode != '' " >
					<!-- point to subRootNode -->
					<xsl:for-each select="$rootNode//item[@href = $subRootNode]" >
						<!-- main link -->
						<xsl:for-each select="item[@href]">
							<tr>
								<td height="{$menuPointHeigth}">
									<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
								</td>
								<th colspan="{$depth + 1}" align="left">
									<xsl:call-template name="addLink" />
								</th>
							</tr>
							<!-- sub links -->
							<!-- test if below this MAIN menu point the searched link is located -->
							<xsl:if test="current()[@href = $browserAddress ] or descendant::item[@href = $browserAddress ] ">
								<xsl:call-template name="createTree">
									<xsl:with-param name="depth" select="$depth" />
									<xsl:with-param name="browserAddress" select="$browserAddress" />
									<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth" />
									<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon" />
									<xsl:with-param name="subRootNode" select="$subRootNode" />
									<xsl:with-param name="rootNode" select="$rootNode" />
								</xsl:call-template>
							</xsl:if>
							<!-- END OF: test if below this main menu point the searched link is located -->
							<!-- END OF: sub links -->
							<!-- place holder between main links -->
							<xsl:if test="count(following-sibling::item) &gt; 0">
								<tr>
									<td height="{$spaceBetweenMainLinks}" colspan="{$depth + 2}">
										<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
									</td>
								</tr>
							</xsl:if>
							<!-- END OF: place holder between main links -->
						</xsl:for-each>
						<!-- END OF: main link -->
					</xsl:for-each>
					<!-- END OF: point to subRootNode -->
				</xsl:when>
				<xsl:otherwise>
					<!-- main link -->
					<xsl:for-each select="$rootNode/item[@href]">
						<tr>
							<td height="{$menuPointHeigth}">
								<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
							</td>
							<th colspan="{$depth + 1}" align="left">
								<xsl:call-template name="addLink" />
							</th>
						</tr>
						<!-- sub links -->
						<!-- test if below this MAIN menu point the searched link is located -->
						<xsl:if test="current()[@href = $browserAddress ] or descendant::item[@href = $browserAddress ] ">
							<xsl:call-template name="createTree">
								<xsl:with-param name="depth" select="$depth" />
								<xsl:with-param name="browserAddress" select="$browserAddress" />
								<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth" />
								<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon" />
								<xsl:with-param name="subRootNode" select="$subRootNode" />
								<xsl:with-param name="rootNode" select="$rootNode" />
							</xsl:call-template>
						</xsl:if>
						<!-- END OF: test if below this main menu point the searched link is located -->
						<!-- END OF: sub links -->
						<!-- place holder between main links -->
						<xsl:if test="count(following-sibling::item) &gt; 0">
							<tr>
								<td height="{$spaceBetweenMainLinks}" colspan="{$depth + 2}">
									<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
								</td>
							</tr>
						</xsl:if>
						<!-- END OF: place holder between main links -->
					</xsl:for-each>
					<!-- END OF: main link -->
				</xsl:otherwise>
			</xsl:choose>
			<!-- END OF: navigation tree -->
			<!-- borderWidthDown -->
			<tr>
				<td height="{$borderWidthTopDown}" colspan="{$depth + 2}">
					<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
				</td>
			</tr>
			<!-- END OF: borderWidthDown -->
		</table>
		<!-- end of general table -->
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="forLoop.createColumns">
		<xsl:param name="i"/>
		<xsl:param name="count"/>
		<xsl:param name="columnWidthIcon"/>
		<xsl:if test="$i &lt;= $count">
			<td width="{$columnWidthIcon}%">
				<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="1" border="0" alt="" title=""></img>
			</td>
		</xsl:if>
		<xsl:if test="$i &lt;= $count">
			<xsl:call-template name="forLoop.createColumns">
				<xsl:with-param name="i">
					<!-- Increment index-->
					<xsl:value-of select="$i + 1"/>
				</xsl:with-param>
				<xsl:with-param name="count">
					<xsl:value-of select="$count"/>
				</xsl:with-param>
				<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="createTree">
		<xsl:param name="browserAddress" />
		<xsl:param name="depth" />
		<xsl:param name="menuPointHeigth" />
		<xsl:param name="columnWidthIcon" />
		<xsl:param name="subRootNode" />
		<xsl:param name="rootNode" />
		<!-- read all items within this name space -->
		<xsl:for-each select="descendant::item">
			<!-- calculate kind of link to display the right icon -> $linkKind -->
			<xsl:variable name="linkKind">
				<xsl:choose>
					<!-- if this item is the browser address -->
					<xsl:when test="current()[@href = $browserAddress ]">
						<xsl:choose>
							<!-- children -->
							<xsl:when test="descendant::item[@href]">
								<xsl:value-of select="'current_popedUp'"/>
							</xsl:when>
							<!-- no children -->
							<xsl:otherwise>
								<xsl:value-of select="'current'"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<!-- END OF: if this item is the browser address -->
					<!-- if searched link is a descentant of the current one -->
					<xsl:when test="descendant::item[@href = $browserAddress ]">
						<xsl:value-of select="'popedUp'"/>
					</xsl:when>
					<!-- END OF: if searched link is a descentant of the current one -->
					<!--	parent::item[@href = $browserAddress ] -> if the searched link is the parent  
		   or   preceding-sibling::item[@href = $browserAddress ] -> if the searched link is a sibling 
				or following-sibling::item[@href = $browserAddress ]		   
		   or   preceding-sibling::item/descendant::item[@href = $browserAddress ] -> if the searched link is a decentant of a sibling  
	            or following-sibling::item/descendant::item[@href = $browserAddress ]"> -->
					<xsl:when 
						test="
				parent::item[@href = $browserAddress ] 
		   or   preceding-sibling::item[@href = $browserAddress ] 
				or following-sibling::item[@href = $browserAddress ]		   
		   or   preceding-sibling::item/descendant::item[@href = $browserAddress ] 
	            or following-sibling::item/descendant::item[@href = $browserAddress ]
				">
						<xsl:choose>
							<!-- children -->
							<xsl:when test="descendant::item[@href]">
								<xsl:value-of select="'notPopedUp'"/>
							</xsl:when>
							<!-- no children -->
							<xsl:otherwise>
								<xsl:value-of select="'normal'"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="'hide'"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<!-- END OF: calculate kind of link to display the right icon -> $linkKind -->
			<xsl:call-template name="addMenuRow">
				<xsl:with-param name="linkKind" select="$linkKind"/>
				<xsl:with-param name="depth" select="$depth"/>
				<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth"/>
				<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon" />
				<xsl:with-param name="subRootNode" select="$subRootNode" />
				<xsl:with-param name="rootNode" select="$rootNode" />
			</xsl:call-template>
		</xsl:for-each>
		<!-- END OF: read all items within this name space -->
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="addMenuRow">
		<xsl:param name="linkKind" />
		<xsl:param name="depth" />
		<xsl:param name="menuPointHeigth" />
		<xsl:param name="columnWidthIcon" />
		<xsl:param name="subRootNode" />
		<xsl:param name="rootNode" />
		<xsl:param name="createSiteMap" />
		<!-- get depth of subRootNode -> $depthSubRootNode -->
		<xsl:variable name="depthSubRootNode">
			<xsl:choose>
				<xsl:when test="$subRootNode != ''">
					<xsl:for-each select="$rootNode//item[@href = $subRootNode]" >
						<xsl:value-of select="count(ancestor-or-self::item)"/>
					</xsl:for-each>
				</xsl:when>
			</xsl:choose>
		</xsl:variable>
		<!-- END OF: get depth of subRootNode -> $depthSubRootNode -->
		<!-- display complete link row when $linkKind != 'hide' -->
		<xsl:choose>
			<xsl:when test="$linkKind != 'hide'">
				<tr>
					<td height="{$menuPointHeigth}">
						<img src="{$ImageBaseURL}emtyDot1Pix.gif" width="1" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
					</td>
					<!-- draw lines before icon and link -->
					<xsl:for-each select="ancestor::item">
						<xsl:choose>
							<xsl:when test="$subRootNode = ''" >
								<xsl:if test="position() &gt; 1 " >
									<td align="center">
										<xsl:call-template name="addIcon">
											<xsl:with-param name="linkKind" select="'line'" />
											<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth"/>
											<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon" />
										</xsl:call-template>
									</td>
								</xsl:if>
							</xsl:when>
							<xsl:otherwise>
								<xsl:if test="position() &gt; 1 and position() &gt; $depthSubRootNode+1 	 " >
									<td align="center">
										<xsl:call-template name="addIcon">
											<xsl:with-param name="linkKind" select="'line'" />
											<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth"/>
											<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon" />
										</xsl:call-template>
									</td>
								</xsl:if>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>
					<!-- END OF: draw lines before icon and link -->
					<!-- draw icon before the link -->
					<td align="center">
						<xsl:call-template name="addIcon">
							<xsl:with-param name="linkKind" select="$linkKind" />
							<xsl:with-param name="menuPointHeigth" select="$menuPointHeigth"/>
							<xsl:with-param name="columnWidthIcon" select="$columnWidthIcon" />
						</xsl:call-template>
					</td>
					<!-- END OF: draw icon before the link -->
					<!-- display link -->
					<xsl:choose>
						<xsl:when test="$subRootNode = ''" >
							<td colspan="{ ($depth - count(ancestor::item)) + 1 }">
								<wbr>
									<xsl:choose>
										<xsl:when test="$linkKind = 'current' or $linkKind = 'current_popedUp'" >
											<span class="marked">
												<xsl:call-template name="addLink">
													<xsl:with-param name="linkKind" select="$linkKind" />
												</xsl:call-template>
											</span>
										</xsl:when>
										<xsl:otherwise>
											<xsl:call-template name="addLink" >
											</xsl:call-template>
										</xsl:otherwise>
									</xsl:choose>
								</wbr>
							</td>
						</xsl:when>
						<xsl:otherwise>
							<td colspan="{ ($depth - count(ancestor::item)) + $depthSubRootNode + 1 }">
								<wbr>
									<xsl:choose>
										<xsl:when test="$linkKind = 'current' or $linkKind = 'current_popedUp'" >
											<span class="marked">
												<xsl:call-template name="addLink">
													<xsl:with-param name="linkKind" select="$linkKind" />
												</xsl:call-template>
											</span>
										</xsl:when>
										<xsl:otherwise>
											<xsl:call-template name="addLink" >
											</xsl:call-template>
										</xsl:otherwise>
									</xsl:choose>
								</wbr>
							</td>
						</xsl:otherwise>
					</xsl:choose>
					<!-- END OF: display link -->
				</tr>
			</xsl:when>
		</xsl:choose>
		<!-- END OF: display complete link row when $linkKind != 'hide' -->
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="addIcon">
		<xsl:param name="linkKind" />
		<xsl:param name="menuPointHeigth" />
		<xsl:param name="columnWidthIcon" />
		<xsl:choose>
			<!-- list end -->
			<xsl:when test="count(following-sibling::item) &lt; 1">
				<xsl:choose>
					<xsl:when test="$linkKind = 'line'" >
						<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/empty-ri.gif" 
							width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
					</xsl:when>
					<xsl:when test="$linkKind = 'normal'" >
						<img 
							src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/line-with-element_end.gif" 
							width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
					</xsl:when>
					<xsl:when test="$linkKind = 'current'" >
						<img 
							src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/line-with-element-selected_end.gif" 
							width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
					</xsl:when>
					<xsl:when test="$linkKind = 'current_popedUp'" >
						<a href="{concat($WebApplicationBaseURL,substring-after((parent::node()/@href),'/'))}" target="{@target}">
							<img 
								src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/minus-selected_end.gif" 
								width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
						</a>
					</xsl:when>
					<xsl:when test="$linkKind = 'popedUp'" >
						<a href="{concat($WebApplicationBaseURL,substring-after((parent::node()/@href),'/'))}" target="{@target}">
							<img 
								src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/minus_end.gif" 
								width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
						</a>
					</xsl:when>
					<xsl:when test="$linkKind = 'notPopedUp'" >
						<a href="{concat($WebApplicationBaseURL,substring-after(@href,'/'))}" target="{@target}">
							<img 
								src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/plus_end.gif" 
								width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title="" />
						</a>
					</xsl:when>
				</xsl:choose>
			</xsl:when>
			<!-- END OF: list end -->
			<!-- not list end -->
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="$linkKind = 'line'" >
						<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/line.gif" 
							width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
					</xsl:when>
					<xsl:when test="$linkKind = 'normal'" >
						<img 
							src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/line-with-element.gif" 
							width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
					</xsl:when>
					<xsl:when test="$linkKind = 'current'" >
						<img 
							src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/line-with-element-selected.gif" 
							width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
					</xsl:when>
					<xsl:when test="$linkKind = 'current_popedUp'" >
						<a href="{concat($WebApplicationBaseURL,substring-after((parent::node()/@href),'/'))}" target="{@target}">
							<img 
								src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/minus-selected.gif" 
								width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
						</a>
					</xsl:when>
					<xsl:when test="$linkKind = 'popedUp'" >
						<a href="{concat($WebApplicationBaseURL,substring-after((parent::node()/@href),'/'))}" target="{@target}">
							<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/minus.gif" 
								width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title=""></img>
						</a>
					</xsl:when>
					<xsl:when test="$linkKind = 'notPopedUp'" >
						<a href="{concat($WebApplicationBaseURL,substring-after(@href,'/'))}" target="{@target}">
							<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/plus.gif" 
								width="{$columnWidthIcon}" height="{$menuPointHeigth}" border="0" alt="" title="" />
						</a>
					</xsl:when>
				</xsl:choose>
			</xsl:otherwise>
			<!-- END OF: not list end -->
		</xsl:choose>
	</xsl:template>
	<!-- ================================================================================= -->
	<!-- ================================================================================= -->
	<xsl:template name="addLink">
		<xsl:param name="createSiteMap" />
		<xsl:choose>
			<!-- item @type is "intern" -> add the web application path before the link -->
			<xsl:when test="@type = 'intern'">
				<a target="{@target}">
					<xsl:attribute name="href">
						<xsl:value-of select="concat($WebApplicationBaseURL,substring-after(@href,'/'))" />
					</xsl:attribute>
					<xsl:choose>
						<xsl:when test="@style = 'bold'">
							<b>
								<xsl:choose>
									<xsl:when test="./label[lang($CurrentLang)] != ''">
										<xsl:value-of select="./label[lang($CurrentLang)]" />
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="./label[lang($DefaultLang)]" />
									</xsl:otherwise>
								</xsl:choose>
							</b>
						</xsl:when>
						<xsl:otherwise>
							<xsl:choose>
								<xsl:when test="./label[lang($CurrentLang)] != ''">
									<xsl:value-of select="./label[lang($CurrentLang)]" />
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="./label[lang($DefaultLang)]" />
								</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
				</a>
			</xsl:when>
			<!-- item @type is something else -> just use href -->
			<xsl:otherwise>
				<a target="{@target}">
					<xsl:attribute name="href">
						<xsl:value-of select="@href" />
					</xsl:attribute>
					<xsl:choose>
						<xsl:when test="@style = 'bold'">
							<b>
								<xsl:choose>
									<xsl:when test="./label[lang($CurrentLang)] != ''">
										<xsl:value-of select="./label[lang($CurrentLang)]" />
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="./label[lang($DefaultLang)]" />
									</xsl:otherwise>
								</xsl:choose>
							</b>
						</xsl:when>
						<xsl:otherwise>
							<xsl:choose>
								<xsl:when test="./label[lang($CurrentLang)] != ''">
									<xsl:value-of select="./label[lang($CurrentLang)]" />
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="./label[lang($DefaultLang)]" />
								</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
				</a>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="generateFlagButton">
		<xsl:variable name="englishFlag">
			<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/lang-en.gif" 
				alt="new language: English" title="Union Jack" 
				style="width:24px; height:12px; vertical-align:bottom; border-style:none;" />
		</xsl:variable>
		<xsl:variable name="germanFlag">
			<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/lang-de.gif" 
				alt="new language: German" title="Deutsch" 
				style="width:24px; height:12px; vertical-align:bottom; border-style:none;" />
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$CurrentLang = 'en'">
				<xsl:call-template name="FlagPrinter">
					<xsl:with-param name="flag" select="$germanFlag" />
					<xsl:with-param name="lang" select="'de'" />
					<xsl:with-param name="url" select="$RequestURL" />
					<xsl:with-param name="alternative" select="concat($RequestURL, '?lang=de')" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="FlagPrinter">
					<xsl:with-param name="flag" select="$englishFlag" />
					<xsl:with-param name="lang" select="'en'" />
					<xsl:with-param name="url" select="$RequestURL" />
					<xsl:with-param name="alternative" select="concat($RequestURL, '?lang=en')" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="FlagPrinter">
		<xsl:param name="flag"/>
		<xsl:param name="lang"/>
		<xsl:param name="url"/>
		<xsl:param name="alternative"/>
		<a href="{$ServletsBaseURL}MCRSearchMaskServlet?lang={$lang}&amp;type=document&amp;mode=CreateSearchMask">
			<xsl:attribute name="href">
				<xsl:variable name="newurl">
					<xsl:call-template name="UrlSetParam">
						<xsl:with-param name="url" select="$url" />
						<xsl:with-param name="par" select="'lang'" />
						<xsl:with-param name="value" select="$lang" />
					</xsl:call-template>
				</xsl:variable>
				<xsl:choose>
					<xsl:when test="contains($newurl,'MCR:ERROR')">
						<xsl:value-of select="$alternative"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$newurl"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:copy-of select="$flag" />
		</a>
	</xsl:template>			
	
	<!-- ================================================================================= -->	
</xsl:stylesheet>