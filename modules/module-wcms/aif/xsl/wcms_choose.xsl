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
	xmlns:xalan="http://xml.apache.org/xalan" >

<!-- ====================================================================================={
section: Template: name="wcmsChoose"

	- Seitenaufbau
}===================================================================================== -->
	<xsl:template name="wcmsChoose" >
		<xsl:param name="href" />

		<xsl:variable 
		name="BilderPfad" 
		select="concat($WebApplicationBaseURL,'templates/master/template_wcms/IMAGES')" />

		<!-- Menueleiste einblenden, Parameter = ausgewaehlter Menuepunkt -->
		<xsl:call-template name="menuleiste">
			<xsl:with-param name="menupunkt" select="'Bearbeiten'" />
		</xsl:call-template>

		<!-- Seitenname -->
		<xsl:call-template name="zeigeSeitenname">
			<xsl:with-param name="seitenname" select="'Ziel und Aktion wählen'" />
		</xsl:call-template>


		<form name="choose" action="{$ServletsBaseURL}WCMSChooseServlet" method="post">

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
					<div class="titel">Ziel (Menüpunkt)</div>
					<div class="inhalt">
					<xsl:call-template name="chooseContent" />
					<!-- div class="titel">Legende</div-->
					<div class="legende">
						<table>
							<tr>
								<td class="s2">Legende:</td>
								<td class="s2">&#160;</td>
							</tr>							<tr>
								<td class="s1">&#9472;</td>
								<td class="s2">Unterpunktebene</td>
							</tr>
							<tr>
								<td class="s1">[m]</td>
								<td class="s2">Neue Menüwurzel </td>
							</tr>
							<tr>
								<td class="s1">[t]</td>
								<td class="s2">Eigenes Template</td>
							</tr>
							<tr>
								<td class="s1">[d]</td>
								<td class="s2">Dynamischer Inhalt (MyCoRe)</td>
							</tr>
							<xsl:if test="$CurrentLang != $DefaultLang">
								<tr>
									<td class="s1">[!]</td>
									<td class="s2">Inhalt nicht übersetzt</td>
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
						Hinweis:
						<div class="hinweis-auswahl-text">
							<xsl:if test=" /cms/error = '0' or /cms/error = '5' or /cms/error = '6' or /cms/error = '7' or /cms/error = '8' or /cms/error = '9' ">
								<xsl:call-template name="errorOnChoose" />
							</xsl:if>
							<xsl:if test="/cms/usedParser != ''">
								<xsl:text>Aktion durchgefuehrt.</xsl:text>
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
										<xsl:text>Seite angelegt</xsl:text>
									</xsl:when>
									<xsl:when test=" /cms/action = 'add' and /cms/action[@mode='extern']" >
										<xsl:text>Link angelegt </xsl:text>
									</xsl:when>
									<xsl:when test=" /cms/action = 'edit' and /cms/action[@mode='intern']" >
										<xsl:text>Seite verändert</xsl:text>
									</xsl:when>
									<xsl:when test=" /cms/action = 'edit' and /cms/action[@mode='extern']" > 
										<xsl:text>Informationen zu Link verändert </xsl:text>
									</xsl:when>
									<xsl:when test=" /cms/action = 'delete' and /cms/action[@mode='intern']" >
										<xsl:text>Seite gelöscht </xsl:text>
									</xsl:when>
									<xsl:when test=" /cms/action = 'delete' and /cms/action[@mode='extern']" >
										<xsl:text>Link gelöscht </xsl:text>
									</xsl:when>
								</xsl:choose>
							</xsl:if>
						</div>
					</div>
				</xsl:if>

				<div id="aktion">
				<div class="titel">Aktion</div>
				<div class="inhalt">					
					<table class="aktion">
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('edit');">Inhalt bearbeiten</a></td>
							<td class="aktionOptionLeer"> </td>
						</tr>
						<tr>
							<td colspan="3" class="leerzeile"> </td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('predecessor');">Menüpunkt davor anlegen</a></td>
							<td rowspan="3" class="aktionOption">
								<xsl:call-template name="wcmsChoose.action.option">
									<xsl:with-param name="whichAction" select="'add'" />
								</xsl:call-template>
								<br />
							</td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('child');">Unterpunkt anlegen</a></td>
						</tr>
						<tr class="aktion">
							<td class="aktionIcon"><img src="{$BilderPfad}/wahl_dummy.gif" /></td>
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('successor');">Menüpunkt danach anlegen</a></td>
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
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('delete');">Löschen</a></td>
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
							<td class="aktionBeschreibung"><a href="javascript:starteAktion('view');">Ansehen</a></td>
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
										<xsl:if test=" @replaceMenu = 'true' or @template != '' or count(child::dynamicContentBinding) &gt; 0 ">
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
				Fehler aufgetreten: Keine Seite ausgewählt. 
				<br/>
				Bitte wählen sie in der 2. Box die Seite aus, die sie bearbeiten wollen.
			</xsl:when>
			<xsl:when test=" /cms/error = '5' " >
				Fehler aufgetreten: Anlegen nicht möglich.
				<br/>
				Auf dieser Ebene können keine weiteren Menüpunkte angelegt werden. 
				Wählen sie beim Erstellen die Position "darunter" aus. 
			</xsl:when>
			<xsl:when test=" /cms/error = '6' " >
				Fehler aufgetreten: Anlegen nicht möglich. 
				<br/>
				Unterhalb eines externen Linkes können keine weiteren Menüpunkte angelegt werden. 
			</xsl:when>
			<xsl:when test=" /cms/error = '7' " >
				Fehler aufgetreten: Löschen nicht möglich.
				<br/>
				Ein Menüpunkt kann nur gelöscht werden, wenn er keine Untermenüpunkte mehr enthält.
			</xsl:when>
			<xsl:when test=" /cms/error = '8' " >
				Fehler aufgetreten: Keine Berechtigung.
				<br/>
				Sie können in der gewählten Menüebene keinen Inhalt pflegen. 
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
						<span class="deaktiviert"><xsl:text> Inhalt der Auswahl übernehmen</xsl:text></span>
						<br />
						<input type="checkbox" name="createLink" value="true" />
						<xsl:text> Auf andere Webseite weiterleiten</xsl:text>
					</xsl:when>
				</xsl:choose>
			</xsl:when>
			<xsl:when test="$whichAction = 'view'">
				<input type="checkbox" name="openNewWindow" value="true" checked="checked" />
				<span class="aktionOptionRow"><xsl:text> Im neuen Fenster öffnen</xsl:text></span>
			</xsl:when>
		</xsl:choose>


	</xsl:template>
<!-- =================================================================================== -->

</xsl:stylesheet>