<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" >
	<!-- cmsChooseAction  ============================================================================== -->
	<xsl:template name="wcmsChooseAction" >
		<form name="choose" action="{$ServletsBaseURL}WCMSChooseServlet" method="post">
			<table class="wcms" width="90%" border="0" cellspacing="0" cellpadding="0" align="center">
				<xsl:call-template name="wcms.headline" >
					<xsl:with-param name="infoText" select="'Aktion und Inhalt festlegen (Schritt 1/3)'" />
				</xsl:call-template>
				<!-- "weiter" button -->
				<xsl:call-template name="next"/>
				<!-- END OF: "weiter" button -->
				<!-- error occured - - - - - - - -->
				<xsl:if 
					test=" /cms/error = '0' or /cms/error = '5' or /cms/error = '6' or /cms/error = '7' or /cms/error = '8' or /cms/error = '9' ">
					<xsl:call-template name="error" />
				</xsl:if>
				<!-- END OF: error occured - - - - - - - -->
				<tr>
					<td colspan="2">
						<br/>
					</td>
				</tr>
				<!-- main window -->
				<tr>
					<td colspan="2">
						<table class="table_noGrid" width="100%" cellspacing="0" cellpadding="0" border="0" align="center">
							<colgroup>
								<col width="50%" />
								<col width="50%" />
							</colgroup>
							<tr>
								<td class="green_noBorder" align="center" colspan="2">
									<b> Bitte legen sie zunächst fest, was sie machen wollen und auf welche Seite sich 
										diese Aktion beziehen soll </b>
								</td>
							</tr>
							<tr>
								<!-- left column -->
								<th align="left" valign="top"> <xsl:call-template name="chooseAction" /> <xsl:call-template 
									name="addAtPosition" /> <xsl:call-template name="template" /> <br/><br/> &lt;m&gt; 
									... neues Menü<br/> &lt;t&gt; ... neues Template<br/> &lt;d&gt; ... dynamischer 
									Inhalt zugewiesen<br/> <xsl:if test="$CurrentLang != $DefaultLang"> <br/><br/> 
									&lt;!&gt; ... Seite bisher nicht übersetzt<br/> </xsl:if> </th>
								<!-- right column -->
								<th align="left" valign="top"> Inhalt:<br/> <xsl:call-template name="chooseContent" /> </th>
							</tr>
						</table>
					</td>
				</tr>
				<!-- END OF: main window -->
				<!-- "weiter" button -->
				<tr>
					<td colspan="2">
						<br/>
					</td>
				</tr>
				<xsl:call-template name="next"/>
				<!-- END OF: "weiter" button -->
			</table>
		</form>
	</xsl:template>
	<!-- END OF: cmsChooseAction  ================================================================================= -->
	<!-- ================================================================================= -->
	<!-- creates identifiers how the site will be build (template and change menu) -->
	<xsl:template name="siteStructureInfo"> &lt;<xsl:if test="@replaceMenu='true' " >m</xsl:if><xsl:if test="@template != '' " 
		>t</xsl:if><xsl:if test=" count(child::dynamicContentBinding) &gt; 0 " >d</xsl:if>&gt; </xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="error">
		<tr>
			<td colspan="2">
				<br/>
			</td>
		</tr>
		<tr>
			<th colspan="2" class="red" align="left">
				<b>
					<xsl:choose>
						<xsl:when test=" /cms/error = '0' or /cms/error = '9' " > Fehler aufgetreten: Keine Seite ausgewählt. 
							<br/><br/> Bitte wählen sie in der 2. Box die Seite aus, die sie bearbeiten wollen. </xsl:when>
						<xsl:when test=" /cms/error = '5' " > Fehler aufgetreten: Anlegen nicht möglich. <br/><br/> Auf dieser 
							Ebene können keine weiteren Menüpunkte angelegt werden. Wählen sie beim Erstellen die Position 
							"darunter" aus. </xsl:when>
						<xsl:when test=" /cms/error = '6' " > Fehler aufgetreten: Anlegen nicht möglich. <br/><br/> Unterhalb 
							eines externen Linkes können keine weiteren Menüpunkte angelegt werden. </xsl:when>
						<xsl:when test=" /cms/error = '7' " > Fehler aufgetreten: Löschen nicht möglich. <br/><br/> Ein 
							Menüpunkt kann nur gelöscht werden, wenn er keine Untermenüpunkte mehr enthält. </xsl:when>
						<xsl:when test=" /cms/error = '8' " > Fehler aufgetreten: Keine Berechtigung. <br/><br/> Sie können in 
							der gewählten Menüebene keinen Inhalt pflegen. </xsl:when>
					</xsl:choose>
				</b>
			</th>
		</tr>
		<tr>
			<td colspan="2">
				<br/>
			</td>
		</tr>
	</xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="chooseAction"> Aktion:<br/> <select name="action" size="5" onchange="setHelpText()" > <xsl:if 
		test="(/cms/userClass = 'admin' or /cms/userClass = 'editor') and $CurrentLang=$DefaultLang" > <option selected="selected" 
		value="edit" >- vorhandenen Inhalt bearbeiten</option> </xsl:if> <xsl:if 
		test="(/cms/userClass = 'autor'  or /cms/userClass = 'editor' or /cms/userClass = 'admin') and $CurrentLang=$DefaultLang" > 
		<option value="add_intern">- neue Web-Seite einpflegen</option> </xsl:if> <xsl:if 
		test="(/cms/userClass = 'autor' or /cms/userClass = 'editor' or /cms/userClass = 'admin') and $CurrentLang=$DefaultLang" > 
		<option value="add_extern">- neuen Link einpflegen</option> </xsl:if> <xsl:if 
		test="/cms/userClass = 'admin' and $CurrentLang=$DefaultLang" > <option value="delete">- Inhalt in Archiv verschieben</option> 
		</xsl:if> <xsl:if test="$DefaultLang != $CurrentLang" > <option value="translate">- Inhalt übersetzen</option> </xsl:if> 
		</select> </xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="chooseContent">
		<select name="href" size="25">
			<!-- each root node -->
			<xsl:for-each select="/cms/rootNode" >
				<xsl:variable name="myRootNode" select="node()" />
				<xsl:variable name="myRootNodeTypeHREF" select="@href" />
				<!-- go through the complete navigation.xml -->
				<xsl:for-each select="document($navigationBase)//*">
					<!-- if the current entry is the rootNode -->
					<xsl:choose>
						<xsl:when 
							test="
			    ($myRootNodeTypeHREF = 'yes' and $myRootNode = @href )
			   or
				($myRootNodeTypeHREF = 'no' and $myRootNode	= name(current())   )
			   " 
							>
							<!-- display menu name -->
							<xsl:for-each select="ancestor-or-self::*" >
								<xsl:if test="position() = 2 " >
									<option value="9">
									</option>
									<option value="9">
									</option>
									<!-- test if user is allowed to select this menu point -->
									<xsl:choose>
										<!-- allowed -->
										<xsl:when test=" position() = last() " >
											<option value="2{@dir}">
												<xsl:value-of select="label[lang($DefaultLang)]" />
											</option>
										</xsl:when>
										<!-- forbidden -->
										<xsl:otherwise>
											<option value="8">
												<xsl:variable name="labelPath">
													<xsl:for-each select="document($navigationBase)//item">
														<xsl:if test="$myRootNode = @href">
															<xsl:for-each select="ancestor-or-self::item">
																<xsl:value-of 
																	select="concat(' > ',./label)"/>
															</xsl:for-each>
														</xsl:if>
													</xsl:for-each>
												</xsl:variable>
												<xsl:value-of select="concat(label[lang($DefaultLang)],$labelPath)" />
											</option>
										</xsl:otherwise>
									</xsl:choose>
									<!-- test if user is allowed to select this menu point -->
									<option value="9">
										<xsl:value-of select="'======================'" />
									</option>
								</xsl:if>
							</xsl:for-each>
							<!-- END OF: display menu name -->
							<!-- display menu items -->
							<xsl:for-each select="descendant-or-self::item" >
								<xsl:if test="@href" >
									<option value="1{@href}">
										<xsl:for-each select="ancestor::item"> - </xsl:for-each>
										<xsl:if 
											test=" @replaceMenu = 'true' or @template != '' or count(child::dynamicContentBinding) &gt; 0 " 
											>
											<xsl:call-template name="siteStructureInfo" />
										</xsl:if>
										<!-- handle different languages -->
										<xsl:variable name="label_defLang" select="./label[lang($DefaultLang)]!=''"/>
										<xsl:variable name="label_curLang" select="./label[lang($CurrentLang)]!=''"/>
										<xsl:choose>
											<!-- not default lang and label translated -->
											<xsl:when test="($CurrentLang != $DefaultLang) and $label_curLang != ''"> 
												<xsl:value-of select="./label[lang($CurrentLang)]" /> 
												(<xsl:value-of select="./label[lang($DefaultLang)]" />) </xsl:when>
											<!-- not default lang and label NOT translated -->
											<xsl:when test="($CurrentLang != $DefaultLang) and $label_curLang = ''"> 
												&lt;!&gt; (<xsl:value-of select="./label[lang($DefaultLang)]" />) 
												</xsl:when>
											<xsl:otherwise>
												<xsl:value-of select="./label[lang($DefaultLang)]" />
											</xsl:otherwise>
										</xsl:choose>
									</option>
								</xsl:if>
							</xsl:for-each>
							<!-- END OF: display menu items -->
						</xsl:when>
					</xsl:choose>
					<!-- END OF: if the current entry is the rootNode -->
				</xsl:for-each>
				<!-- END OF: go through the complete navigation.xml -->
			</xsl:for-each>
			<!-- END OF: each root node -->
		</select>
	</xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="addAtPosition">
		<xsl:choose>
			<xsl:when test="$CurrentLang=$DefaultLang"> <br/><br/><br/> An welcher Position: (nur bei neuem Inhalt)<br/> <select 
				name="addAtPosition" size="3" > <option value="predecessor" > davor </option> <option value="successor"> danach 
				</option> <option selected="selected" value="child"> darunter </option> </select> </xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="template"> <!--
    <xsl:choose>
        <xsl:when test="$CurrentLang=$DefaultLang">
--> <br/><br/><br/> Content-Template: (nur bei neuem Inhalt)<br/> <select name="template" size="1" > <xsl:for-each 
		select="/cms/templates/content/template" > <option> <xsl:attribute name="value"><xsl:value-of select="node()" 
		/></xsl:attribute> <xsl:value-of select="node()" /> </option> </xsl:for-each> </select> <!--    
        </xsl:when>
    </xsl:choose>
--> </xsl:template>
	<!-- ================================================================================= -->
	<xsl:template name="next">
		<tr>
			<td colspan="2" align="right">
				<table cellspacing="0" border="0" cellpadding="0" align="right" class="wcms">
					<!-- submit -->
					<tr>
						<td align="right">
							<img 
								src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/box_left.gif" 
								width="11" height="22" border="0" alt="" title="" />
						</td>
						<td align="right" class="button">
							<a href="javascript:document.choose.submit()">weiter zum nächsten Schritt</a>
						</td>
						<td align="right">
							<img 
								src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/box_right.gif" 
								width="11" height="22" border="0" alt="" title="" />
						</td>
					</tr>
					<!-- END OF: submit -->
				</table>
			</td>
		</tr>
	</xsl:template>
</xsl:stylesheet>