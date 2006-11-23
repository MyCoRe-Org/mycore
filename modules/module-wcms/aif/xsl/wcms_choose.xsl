<?xml version="1.0" encoding="UTF-8"?>

<!-- =====================================================================================
========================================================================================={
title: wcms_choose.xsl

Erzeugt die Auswahlseite einer Aktion.

template:
	- wcmsChoose (name)
	- chooseContent (name)
	- chooseContentInfo (name)
	- errorOnChoose (name)
	- hiddenForm (name)
	- wcmsChoose.action.option
}=========================================================================================
====================================================================================== -->

<xsl:stylesheet 
	version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" >

<!-- ====================================================================================={
section: Template: name="wcmsChoose"
docportal build.xml
	- Seitenaufbau
}===================================================================================== -->
	<xsl:template name="wcmsChoose" >
		<xsl:param name="href" />

		<xsl:variable 
		name="BilderPfad" 
		select="concat($WebApplicationBaseURL,'templates/master/template_wcms/IMAGES')" />

		<!-- Menueleiste einblenden, Parameter = ausgewaehlter Menuepunkt -->
		<xsl:call-template name="menuleiste">
			<xsl:with-param name="menupunkt" select="i18n:translate('wcms.labels.edit')" />
		</xsl:call-template>

		<!-- Seitenname -->
		<xsl:call-template name="zeigeSeitenname">
			<xsl:with-param name="seitenname" select="i18n:translate('wcms.labels.chooseAction')" />
		</xsl:call-template>


		<form name="choose" action="{$ServletsBaseURL}WCMSChooseServlet{$JSessionID}" method="post">

		<!-- Inhaltsbereich -->
		<div id="auswahl">
			<!-- Fensterbreite Ziel-->
			<div id="menu-width">
				<!-- 
				<div id="menu_kopf">
					Ziel
				</div>
				-->
				<div id="menu">
					<div class="titel"><xsl:value-of select="i18n:translate('wcms.labels.target')"/></div>
					<div class="inhalt">
					<xsl:call-template name="chooseContent" />
					<!-- div class="titel">Legende</div-->
					<div class="legende">
						<table>
							<tr>
								<td class="s2"><xsl:value-of select="concat(i18n:translate('wcms.labels.legend'),' :')"/></td>
								<td class="s2">&#160;</td>
							</tr>							
                            <tr>
								<td class="s1">&#9472;</td>
								<td class="s2"><xsl:value-of select="i18n:translate('wcms.labels.subsection')"/></td>
							</tr>
							<tr>
								<td class="s1">[m]</td>
								<td class="s2"><xsl:value-of select="i18n:translate('wcms.labels.newMenu')"/></td>
							</tr>
							<tr>
								<td class="s1">[t]</td>
								<td class="s2"><xsl:value-of select="i18n:translate('wcms.labels.persTemplate')"/></td>
							</tr>
							<tr>
								<td class="s1">[o]</td>
								<td class="s2"><xsl:value-of select="i18n:translate('wcms.labels.constrainPopUp')"/></td>
							</tr>
							<xsl:if test="$CurrentLang != $DefaultLang">
								<tr>
									<td class="s1">[!]</td>
									<td class="s2"><xsl:value-of select="i18n:translate('wcms.labels.notTranslated')"/></td>
								</tr>
							</xsl:if>
						</table>
					</div>
					</div>
				</div>
			</div>

			<!-- Fensterbreite Aktion -->
			<div id="content-width">

				<xsl:if test="/cms/error != '' or /cms/usedParser != ''">
					<div id="hinweis-auswahl">
						<xsl:value-of select="concat(i18n:translate('wcms.hint'),' :')"/>
						<div class="hinweis-auswahl-text">
							<xsl:if test=" /cms/error = '0' or /cms/error = '5' or /cms/error = '6' or /cms/error = '7' or /cms/error = '8' or /cms/error = '9' ">
								<xsl:call-template name="errorOnChoose" />
							</xsl:if>
							<xsl:if test="/cms/usedParser != ''">
								<xsl:value-of select="i18n:translate('wcms.hint.actionComplete')"/>
								<br />

								<xsl:for-each select="document($navigationBase)/navigation//item[@href]">
									<xsl:if test="@href = $href">
										<xsl:for-each select="ancestor-or-self::item">
											<xsl:value-of select="./label" />
											<xsl:if test="position() != last()"> > </xsl:if>
										</xsl:for-each>
									</xsl:if>
								</xsl:for-each>
								<xsl:if test="/cms/action = 'delete'" >
									<xsl:value-of select="/cms/label" /> 
								</xsl:if>
								<br />

								<xsl:choose>
									<xsl:when test=" /cms/action = 'add' and /cms/action[@mode='intern']" >
										<xsl:value-of select="i18n:translate('wcms.hint.siteCreated')"/>
									</xsl:when>
									<xsl:when test=" /cms/action = 'add' and /cms/action[@mode='extern']" >
										<xsl:value-of select="i18n:translate('wcms.hint.linkCreated')"/>
									</xsl:when>
									<xsl:when test=" /cms/action = 'edit' and /cms/action[@mode='intern']" >
										<xsl:value-of select="i18n:translate('wcms.hint.siteChanged')"/>
									</xsl:when>
									<xsl:when test=" /cms/action = 'edit' and /cms/action[@mode='extern']" > 
										<xsl:value-of select="i18n:translate('wcms.hint.linkInfoChanged')"/>
									</xsl:when>
									<xsl:when test=" /cms/action = 'delete' and /cms/action[@mode='intern']" >
										<xsl:value-of select="i18n:translate('wcms.hint.siteDeleted')"/>
									</xsl:when>
									<xsl:when test=" /cms/action = 'delete' and /cms/action[@mode='extern']" >
										<xsl:value-of select="i18n:translate('wcms.hint.linkDeleted')"/>
									</xsl:when>
								</xsl:choose>
							</xsl:if>
						</div>
					</div>
				</xsl:if>

				<div id="aktion">
				<div class="titel"><xsl:value-of select="i18n:translate('wcms.action')"/></div>
				<div class="inhalt">					
					<table class="aktion">
						<xsl:choose>
							<xsl:when test="$CurrentLang=$DefaultLang">
								<tr class="aktion">
									<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
									<td class="aktionBeschreibung"><a href="javascript:starteAktion('edit');"><xsl:value-of select="i18n:translate('wcms.action.editContent')"/></a></td>
									<td class="aktionOptionLeer"> </td>
								</tr>								
							</xsl:when>
							<xsl:otherwise>
								<tr class="aktion">
									<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
									<td class="aktionBeschreibung"><a href="javascript:starteAktion('translate');"><xsl:value-of select="i18n:translate('wcms.action.translate')"/></a></td>
									<td class="aktionOptionLeer"> </td>
									<input type="hidden" name="template">
										<xsl:for-each
											select="/cms/templates/content/template" >
												<xsl:attribute name="value">
													<xsl:value-of
														select="node()"/>
												</xsl:attribute>
										</xsl:for-each>
									</input>
								</tr>
							</xsl:otherwise>
						</xsl:choose>
						<tr>
							<td colspan="3" class="leerzeile"> </td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('predecessor');"><xsl:value-of select="i18n:translate('wcms.action.insertMenuBefore')"/></a></td>
							<td rowspan="3" class="aktionOption">
								<xsl:call-template name="wcmsChoose.action.option">
									<xsl:with-param name="whichAction" select="'add'" />
								</xsl:call-template>
								<br />
							</td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('child');"><xsl:value-of select="i18n:translate('wcms.action.submenuInsert')"/></a></td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('successor');"><xsl:value-of select="i18n:translate('wcms.action.insertMenuAfter')"/></a></td>
						</tr>
						<tr>
							<td colspan="3" class="leerzeile"> </td>
						</tr>
						<!--tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('translate');">Übersetzen</a></td>
							<td class="aktionOptionLeer"> </td>
						</tr>
						<tr>
							<td colspan="3" class="leerzeile"> </td>
						</tr-->
						<!--
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="">Nach oben verschieben</a></td>
							<td rowspan="4" class="aktionOptionLeer"></td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="">Einrücken</a></td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="">Ausgliedern</a></td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="">Nach unten verschieben</a></td>
						</tr>
						<tr>
							<td colspan="3" class="leerzeile"> </td>
						</tr>
						-->
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('delete');"><xsl:value-of select="i18n:translate('wcms.delete')"/></a></td>
							<td rowspan="2" class="aktionOptionLeer"></td>
						</tr>
						<!--
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="">Letzte Aktion widerrufen</a></td>
						</tr>
						-->
						<tr>
							<td colspan="3" class="leerzeile"> </td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('view');"><xsl:value-of select="i18n:translate('wcms.view')"/></a></td>
							<td class="aktionOption">
								<xsl:call-template name="wcmsChoose.action.option">
									<xsl:with-param name="whichAction" select="'view'" />
								</xsl:call-template>
							</td>
						</tr>
					</table>
					</div>
				</div>

					<xsl:call-template name="hiddenForm" />
			</div>

			
			<!-- Textumfluss wird unterbrochen und unten fortgesetzt -->
			<div class="clear">
				&#160;
			</div>

		</div>
	<!-- Ende: Inhaltsbereich -->

		</form>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="chooseContent"

	- Menuestruktur (Fomularfeld)
}===================================================================================== -->
	<xsl:template name="chooseContent">
		<select name="href" size="20" class="auswahl-ziel">
			<!-- each root node -->
			<xsl:for-each select="/cms/rootNode" >
				<xsl:variable name="myRootNode" select="node()" />
				<xsl:variable name="myRootNodeTypeHREF" select="@href" />
				<!-- go through the complete navigation.xml -->
				<xsl:for-each select="document($navigationBase)//*">
					<xsl:variable name="firstRootNode" select="position()" />
					<!-- if the current entry is the rootNode -->
					<xsl:choose>
						<xsl:when 
							test="	($myRootNodeTypeHREF = 'yes' and $myRootNode = @href )
									or
									($myRootNodeTypeHREF = 'no' and $myRootNode	= name(current()))">
							<!-- display menu name -->
							<xsl:for-each select="ancestor-or-self::*" >
								<xsl:if test="position() = 2 " >
									<!--
									<option value="9">
									</option>
									-->
									<option value="9">
										<xsl:value-of select="'======================'" />
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
												<xsl:value-of select="concat(@label,$labelPath)" />
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
										<xsl:if test="concat($firstRootNode, position())='21'">
											<xsl:attribute name="selected">
												<xsl:value-of select="'selected'" />
											</xsl:attribute>
										</xsl:if>
										<xsl:for-each select="ancestor::item"><xsl:text> &#9472; </xsl:text></xsl:for-each>
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
										<xsl:if test=" @replaceMenu = 'true' or @template != '' or @constrainPopUp = 'true' ">
											<xsl:call-template name="chooseContentInfo" />
										</xsl:if>
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

<!-- ====================================================================================={
section: Template: name="chooseContentInfo"

	- Legende
}===================================================================================== -->
	<!-- creates identifiers how the site will be build (template and change menu) -->
	<xsl:template name="chooseContentInfo">
		<xsl:text> [</xsl:text>
		<xsl:if test="@replaceMenu='true' " >
			<xsl:text>m</xsl:text>
		</xsl:if>
		<xsl:if test="@constrainPopUp='true' " >
			<xsl:text>o</xsl:text>
		</xsl:if>		
		<xsl:if test="@template != '' ">
			<xsl:text>t</xsl:text>
		</xsl:if>
		<xsl:if test=" count(child::dynamicContentBinding) &gt; 0 " >
			<xsl:text>d</xsl:text>
		</xsl:if>
		<xsl:text>]</xsl:text>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="errorOnChoose"

	- Fehlermeldungen
}===================================================================================== -->

	<xsl:template name="errorOnChoose">

		<xsl:choose>
			<xsl:when test=" /cms/error = '0' or /cms/error = '9' " > 
				<script LANGUAGE="JAVASCRIPT">
					document.write(new Date())
				</script>
				<xsl:value-of select="i18n:translate('wcms.errors.noSite')"/>
			</xsl:when>
			<xsl:when test=" /cms/error = '5' " >
				<xsl:value-of select="i18n:translate('wcms.errors.noCreation')"/> 
			</xsl:when>
			<xsl:when test=" /cms/error = '6' " >
				<xsl:value-of select="i18n:translate('wcms.errors.noCreationExtern')"/>
			</xsl:when>
			<xsl:when test=" /cms/error = '7' " >
				<xsl:value-of select="i18n:translate('wcms.errors.noDelete')"/>
			</xsl:when>
			<xsl:when test=" /cms/error = '8' " >
				<xsl:value-of select="i18n:translate('wcms.errors.noRights')"/> 
			</xsl:when>
		</xsl:choose>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="hiddenForm"

	- Art der Aktion (Formularfeld)
	- Parameter zur Aktion (Formularfeld)
}===================================================================================== -->
	<xsl:template name="hiddenForm"> 
	<!--
		Moegliche Werte:
			edit - bearbeiten (admin, editor)
			add_intern - neue Seite (autor, admin, editor)
			add_extern - neuer Link (autor, admin, editor)
			delete - loeschen (admin)
			translate - uebersetzen
		Bedingung:
			/cms/userClass = 'autor'  or /cms/userClass = 'editor' or /cms/userClass = 'admin'
			$CurrentLang=$DefaultLang
	-->
		<input  name="action" type="hidden" value="" />
	<!--
		Moegliche Werte:
			predecessor - darueber
			successor - darunter
			child - untergeordnet
		Bedingung:
			$CurrentLang=$DefaultLang
	-->
		<input name="addAtPosition" type="hidden" value="" />
		<input name="webBase" type="hidden" value="{$WebApplicationBaseURL}" />
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="wcmsChoose.action.option"

	- Vorlage fuer neuen Inhalt (Formularfeld)
}===================================================================================== -->
	<xsl:template name="wcmsChoose.action.option">
		<xsl:param name="whichAction" />

		<xsl:choose>
			<xsl:when test="$whichAction = 'add'">
				<xsl:choose>
					<xsl:when test="$CurrentLang=$DefaultLang">
						<!--
						<select name="template" size="1" > 
							<xsl:for-each select="/cms/templates/content/template">
								<option>
									<xsl:attribute name="value">
										<xsl:value-of select="node()"/>
									</xsl:attribute>
									<xsl:value-of select="node()"/>
								</option>
							</xsl:for-each>
						</select>
						<xsl:text>Als Vorlage benutzen</xsl:text>
						<br />
						-->
						<input type="hidden" name="template" value="dumy.xml" />
						<input type="checkbox" name="useContent" value="true" checked="checked"  disabled="disabled" />
						<span class="deaktiviert"><xsl:value-of select="i18n:translate('wcms.labels.getContent')"/></span>
						<br />
						<input type="checkbox" name="createLink" value="true" />
						<xsl:value-of select="i18n:translate('wcms.labels.linkToOther')"/> 
					</xsl:when>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="$whichAction = 'view'">
				<input type="checkbox" name="openNewWindow" value="true" checked="checked" />
				<span class="aktionOptionRow"><xsl:value-of select="i18n:translate('wcms.labels.newWinOpen')"/></span>
			</xsl:when>
		</xsl:choose>


	</xsl:template>
<!-- =================================================================================== -->

</xsl:stylesheet>