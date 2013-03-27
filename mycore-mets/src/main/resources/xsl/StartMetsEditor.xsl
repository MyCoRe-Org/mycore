<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager">
  <xsl:output method="html" indent="yes" doctype-public="-//IETF//DTD HTML 2.0//EN" />
  <xsl:include href="coreFunctions.xsl" />

  <xsl:param name="RequestURL" />
  <xsl:param name="WebApplicationBaseURL" />

  <xsl:variable name="derivateId">
    <xsl:call-template name="UrlGetParam">
      <xsl:with-param name="url" select="$RequestURL" />
      <xsl:with-param name="par" select="'derivate'" />
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="useExistingMets">
    <xsl:call-template name="UrlGetParam">
      <xsl:with-param name="url" select="$RequestURL" />
      <xsl:with-param name="par" select="'useExistingMets'" />
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="createMetsAllowed" select="acl:checkPermission($derivateId,'writedb')" />

  <xsl:template match="/StartMetsEditor">
    <!-- $Revision$ $Date$ $LastChangedBy$ Copyright 2010 - Thüringer Universitäts- 
      und Landesbibliothek Jena Mets-Editor is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License 
      as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Mets-Editor is distributed 
      in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
      PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with Mets-Editor. 
      If not, see http://www.gnu.org/licenses/. -->
    <xsl:comment>
      Start - StartMetsEditor.xsl
    </xsl:comment>
    <xsl:choose>
      <xsl:when test="$createMetsAllowed">
        <html>
          <head>
            <title>
              <xsl:value-of select="concat('Mets Editor - ',$derivateId)" />
            </title>
            <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/dojo/1.6.1/dojo/dojo.xd.js" djConfig="parseOnLoad:true"></script>
            <link rel="stylesheet" type="text/css" href="style/style.css" />
            <style type="text/css">
              @import "http://ajax.googleapis.com/ajax/libs/dojo/1.6.1/dojo/resources/dojo.css";
              @import
              "http://ajax.googleapis.com/ajax/libs/dojo/1.6.1/dijit/themes/tundra/tundra.css";
              @import
              "http://ajax.googleapis.com/ajax/libs/dojo/1.6.1/dojo/resources/dnd.css";

              .myFolder {
              background-image:
              url(img/folder_16x16.png);
              }
              .myItem {
              background-image: url(img/page_16x16.png);
              }

              .addStructureIconEnabled {
              background-image:
              url(img/tree_icon_enabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .addStructureIconDisabled {
              background-image:
              url(img/tree_icon_disabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .foliateIconDisabled {
              background-image:
              url(img/foliation_icon_disabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .foliateIconEnabled {
              background-image:
              url(img/foliation_icon_enabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .helpIcon {
              background-image: url(img/help.png);
              width:
              16px;
              height: 16px;
              }

              .editStructureIconEnabled {
              background-image: url(img/edit_icon_enabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .editStructureIconDisabled {
              background-image: url(img/edit_icon_disabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .deleteStructureIconEnabled {
              background-image: url(img/delete_icon_enabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .deleteStructureIconDisabled {
              background-image: url(img/delete_icon_disabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .reverseOrderEnabled {
              background-image: url(img/icon_reverse_enabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .reverseOrderDisabled {
              background-image: url(img/icon_reverse_disabled_16x16.png);
              width: 16px;
              height: 16px;
              }

              .setDocTypeIcon {
              background-image:
              url(img/icon_setDocTitle_enabled_16x16.png);
              width: 16px;
              height: 16px;
              }

            </style>
            <script type="text/javascript" src="js/includes/includes.js"></script>
            <script type="text/javascript" src="js/saveMets.js"></script>
            <script type="text/javascript" src="js/model/StructureModel.js"></script>
            <script type="text/javascript" src="js/model/modelConversion.js"></script>
            <script type="text/javascript" src="js/structuring.js"></script>
            <script type="text/javascript" src="js/structuringDelete.js"></script>
            <script type="text/javascript" src="js/dnd/dnd.js"></script>
            <script type="text/javascript" src="js/selectionManagement.js"></script>
            <script type="text/javascript" src="js/foliation.js"></script>
            <script type="text/javascript" src="js/reverse.js"></script>
            <script type="text/javascript" src="js/itemToolTips.js"></script>
            <script type="text/javascript" src="js/preview.js"></script>
            <script type="text/javascript" src="js/help.js"></script>
            <script type="text/javascript" src="js/storeUtils.js"></script>
            <script type="text/javascript" src="js/uuid.js"></script>
            <script type="text/javascript" src="js/logger.js"></script>
            <script type="text/javascript" src="js/reload.js"></script>

            <script type="text/javascript">
              <!-- define resetTreeURL -->
              <xsl:value-of
                select="concat('var resetTreeURL = &quot;',$WebApplicationBaseURL, 'metseditor/start_mets_editor.xml?derivate=',$derivateId,'&amp;useExistingMets=false','&quot;;')" />
              <!-- define reloadTreeURL -->
              <xsl:value-of
                select="concat('var reloadTreeURL = &quot;',$WebApplicationBaseURL, 'metseditor/start_mets_editor.xml?derivate=',$derivateId,'&amp;useExistingMets=true','&quot;;')" />
              <!-- define webApplicationBaseURL -->
              <xsl:value-of select="concat('var webApplicationBaseURL = &quot;', $WebApplicationBaseURL,'&quot;;')" />
              <!-- define derivateId -->
              <xsl:value-of select="concat('var derivateId = &quot;', $derivateId,'&quot;;')" />

              dojo.addOnLoad(function() {
              dojo.connect(dijit.byId('itemTree'), "focusNode", toggleStructureButtons);
              dojo.connect(dijit.byId('itemTree'), "focusNode", displayItemProperties);
              dojo.connect(dijit.byId('itemTree'), "onClick",
              trackSelection);
              dojo.connect(dijit.byId('itemTree'), "onClick", loadPreviewImage);
              dojo.connect(dijit.byId('itemTree'),
              "onDblClick", showEditStructureDialog);
              dojo.connect(dijit.byId('itemTree'), "onKeyDown", handleKeyDown);
              dijit.byId('toolbar1.addStructure').setDisabled(true);
              dijit.byId('toolbar1.editStructure').setDisabled(true);
              dijit.byId('toolbar1.foliation').setDisabled(true);
              dijit.byId('toolbar1.deleteStructure').setDisabled(true);
              dijit.byId('toolbar1.deleteStructure').setDisabled(true);
              }
              );

              window.onbeforeunload = onEditorClose;
              function onEditorClose() {
                <xsl:value-of select="concat('var msg = &quot;', i18n:translate('component.mets.editor.close'), '&quot;;')" />
                return msg;
              }

            </script>
          </head>
          <body class="tundra">
            <span dojoType="dijit.Declaration" widgetClass="ToolbarSectionStart" defaults="{{ label: 'Label'}}">
              <span dojoType="dijit.ToolbarSeparator"></span>
              <i>${label}:</i>
            </span>

            <div id="toolbar1" dojoType="dijit.Toolbar">
              <div dojoType="dijit.form.Button" id="toolbar1.reloadTree" iconClass="dijitEditorIcon dijitEditorIconUndo" showLabel="true"
                onclick="reloadTree"></div>
              <div dojoType="dijit.form.Button" id="toolbar1.resetTree" iconClass="dijitEditorIcon dijitEditorIconNewPage" showLabel="true"
                onclick="resetTree"></div>
              <div dojoType="dijit.ToolbarSeparator" id="toolbar1.sep4"></div>
              <div dojoType="dijit.form.Button" id="toolbar1.save" iconClass="dijitEditorIcon dijitEditorIconSave" showLabel="false"
                onclick="save"></div>
              <div dojoType="ToolbarSectionStart" label="Strukturierung"></div>
              <div dojoType="dijit.form.Button" id="toolbar1.setDocType" iconClass="setDocTypeIcon" onclick="showEditDocTypeDialog"></div>
              <div dojoType="dijit.form.Button" id="toolbar1.addStructure" iconClass="addStructureIconDisabled" onclick="showAddStructureDialog"></div>
              <div dojoType="dijit.form.Button" id="toolbar1.editStructure" iconClass="editStructureIconDisabled" onclick="showEditStructureDialog"></div>
              <div dojoType="dijit.form.Button" id="toolbar1.deleteStructure" iconClass="deleteStructureIconDisabled" onclick="deleteStructure"></div>
              <div dojoType="ToolbarSectionStart" label="Foliierung/Paginierung"></div>
              <div dojoType="dijit.form.Button" id="toolbar1.foliation" iconClass="foliateIconDisabled" onclick="showFoliationDialog"></div>
              <div dojoType="dijit.form.Button" id="toolbar1.reverse" iconClass="reverseOrderEnabled" onclick="reverseTree"></div>
              <div dojoType="dijit.ToolbarSeparator" id="toolbar1.sep3"></div>
              <div dojoType="dijit.form.Button" id="toolbar1.help" iconClass="helpIcon" onclick="showViewHelpDialog"></div>
            </div>

            <!-- ########################################### -->
            <!-- Definition of the tree and the content area -->
            <!-- ########################################### -->

            <div id="evenmoreouter" dojoType="dijit.layout.ContentPane" style="width: 100%; height: 100%;">
              <!-- the overall layout container -->
              <div id="outer" dojoType="dijit.layout.SplitContainer" style="width: 100%; height:90%;" persist="false" activeSizing="false">
                <!-- content pane for the tree -->
                <div id="c1" dojoType="dijit.layout.ContentPane" sizeShare="10">
                  <div id="loadingMsg">
                    <img src="img/loading.gif" style="display: block; margin-left: auto; margin-right: auto; padding-top:8em;" />
                  </div>
                  <!-- the actual tree -->
                  <div dojoType="dojo.data.ItemFileWriteStore" jsId="myStore" urlPreventCache="true"
                    url="{concat($WebApplicationBaseURL, 'servlets/MCRJSONProviderServlet?derivate=', $derivateId, '&amp;useExistingMets=',$useExistingMets)}">
                  </div>
                  <div dojoType="dijit.tree.TreeStoreModel" jsId="itemModel" store="myStore" childrenAttrs="children">
                  </div>
                  <div dojoType="dijit.Tree" id="itemTree" model="itemModel" dndController="dijit.tree.dndSource" checkAcceptance="dndAccept"
                    checkItemAcceptance="isDropAllowed" betweenThreshold="5" dragThreshold="5" persist="true" getIconClass="getIcon" onLoad="onTreeLoaded">
                  </div>
                </div>
                <!-- placeholder for the image -->
                <div id="previewImageContainer" dojoType="dijit.layout.ContentPane" sizeShare="20"></div>
              </div>
              <!-- place temporary status bar here -->
              <div id="statusBar" dojoType="dijit.layout.ContentPane"
                style="width:100%;height:10%; padding-left:0.5em; padding-top:0.5em; color:black; background:#eaeaea url(&quot;img/backroundStatusBar.png&quot;) repeat-x top left;">
                <label id="displayItemProps" />
              </div>
            </div>


            <!-- ##################################### -->
            <!-- Definitions for the structure dialogs -->
            <!-- ##################################### -->

            <div id="addStructureDialog" dojoType="dijit.Dialog" title="Hinzufügen eines Strukturelements">
              <div id="addStructureDialogContentPane" dojoType="dijit.layout.ContentPane" style="width:300px">
                <div dojoType="dojo.data.ItemFileReadStore" jsId="DFGStructureDatasetStore" url="js/stores/DFGStructureDataset.js"></div>
                <div dojoType="dijit.layout.ContentPane">

                  <div style="padding-top:0.5em;">
                    <label for="structureType">
                      <xsl:value-of select="concat(i18n:translate('component.mets.dialog.structuretype'),': ')" />
                    </label>
                    <div style="float:right;">
                      <input id="structureType" name="structureType" dojoType="dijit.form.ComboBox" autoComplete="true" type="text"
                        forceValidOption="true" persist="false" store="DFGStructureDatasetStore">
                      </input>
                    </div>
                  </div>
                  <div style="padding-top:0.5em;">
                    <label for="structure">
                      <xsl:value-of select="concat(i18n:translate('component.mets.dialog.structureelement'),': ')" />
                    </label>
                    <div style="float:right;">
                      <input id="structureName" name="structureName" dojoType="dijit.form.TextBox" class="dijitTextBox" type="text"
                        autocomplete="off"
                        dojoattachevent="onmouseenter:_onMouse, onmouseleave:_onMouse,onfocus:_onMouse, onblur:_onMouse,onkeyup,onkeypress:_onKeyPress"
                        dojoattachpoint="textbox,focusNode" tabindex="0" widgetid="dijit_form_TextBox" value="">
                      </input>
                    </div>
                  </div>
                </div>


                <div dojoType="dijit.layout.ContentPane">
                  <button dojoType="dijit.form.Button" id="addStructure" label="Hinzuf&#252;gen" iconClass="circleIcon" onclick="addStructureToTree"
                    style="float: right; padding-top: 1em;" />
                </div>
              </div>
            </div>

            <!-- The definition of the edit structure dialog -->
            <div id="editStructureDialog" dojoType="dijit.Dialog" title="Bearbeiten eines Strukturelements">
              <div dojoType="dojo.data.ItemFileReadStore" jsId="DFGStructureDatasetStore2" url="js/stores/DFGStructureDataset.js"></div>
              <div id="editStructureDialogContentPane" dojoType="dijit.layout.ContentPane" style="width:300px">
                <div dojoType="dijit.layout.ContentPane">

                  <div style="padding-top:0.5em;">
                    <label for="structureTypeEdit">
                      <xsl:value-of select="i18n:translate('component.mets.dialog.structuretype')" />
                    </label>
                    <div style="float:right;">
                      <input id="structureTypeEdit" name="structureTypeEdit" dojoType="dijit.form.ComboBox" autoComplete="true" type="text"
                        forceValidOption="true" persist="false" store="DFGStructureDatasetStore2" />
                    </div>
                  </div>

                  <div style="padding-top:0.5em;">
                    <label for="structureNameEdit">
                      <xsl:value-of select="i18n:translate('component.mets.dialog.structureelement')" />
                    </label>
                    <div style="float:right;">
                      <input id="structureNameEdit" name="structureNameEdit" dojoType="dijit.form.TextBox" />
                    </div>
                  </div>
                </div>

                <div dojoType="dijit.layout.ContentPane">
                  <button dojoType="dijit.form.Button" id="addStructureEdit" label="Änderungen übernehmen" iconClass="circleIcon"
                    onclick="saveEditedStructure" style="float: right; padding-top: 1em;" />
                </div>
              </div>
            </div>


            <!-- ################################### -->
            <!-- Definition for the foliation dialog -->
            <!-- ################################### -->

            <div id="foliationDialog" dojoType="dijit.Dialog" title="Paginierung">
              <!-- the store containing the foliation types -->
              <div dojoType="dojo.data.ItemFileReadStore" jsId="FoliationTypeStore" url="js/stores/FoliationTypes.js"></div>

              <div id="foliationDialogContentPane" dojoType="dijit.layout.ContentPane" style="height:170px; width:350px">
                <div id="cp1" dojoType="dijit.layout.ContentPane" style="height:120;">
                  <!-- from and to -->
                  <div style="float:left;">
                    <label id="labelFromPrefix">
                      <xsl:value-of select="i18n:translate('component.mets.dialog.firstpage')" />
                    </label>
                    <br />
                    <label id="labelToPrefix">
                      <xsl:value-of select="i18n:translate('component.mets.dialog.lastpage')" />
                    </label>
                  </div>
                  <div style="padding-bottom:0.5em;">
                    <label id="labelFrom" class="foliationRange">
                      <xsl:value-of select="i18n:translate('component.mets.dialog.label')" />
                    </label>
                    <br />
                    <label id="labelTo" class="foliationRange">
                      <xsl:value-of select="i18n:translate('component.mets.dialog.label')" />
                    </label>
                  </div>

                  <!-- the combobox with the foliation types -->
                  <div style="padding-top:0.5em;">
                    <label for="foliationTypeCombo">Paginierungsart </label>
                    <div style="float:right;">
                      <input id="foliationTypeCombo" dojoType="dijit.form.ComboBox" autoComplete="true"
                        invalidMessage="Bitte wählen Sie eine gültige Paginierungsart" type="text" forceValidOption="true" persist="false"
                        store="FoliationTypeStore" />
                    </div>
                  </div>

                  <div style="padding-top:0.5em;">
                    <label for="startValueTextBox">Beginnen mit </label>
                    <div style="float:right;">
                      <input id="startValueTextBox" dojoType="dijit.form.NumberTextBox" invalidMessage="Nur Zahlen bitte" />
                    </div>
                  </div>

                  <div style="padding-top:0.5em;">
                    <label for="enableReverseFoliation">Rückwärtpaginierung </label>
                    <div style="float:right;">
                      <input type="checkbox" id="enableReverseFoliation" dojoType="dijit.form.CheckBox"></input>
                    </div>
                  </div>

                </div>
                <div id="cp2" dojoType="dijit.layout.ContentPane">
                  <!-- the save/foliate button -->
                  <button dojoType="dijit.form.Button" id="doFoliateButton" label="Paginieren" iconClass="circleIcon" onclick="validateFoliationSettings"
                    style="float: right; padding-top: 1em;">
                  </button>
                </div>
              </div>
            </div>

            <!-- #################################### -->
            <!-- Definition of the save failed dialog -->
            <!-- #################################### -->

            <div id="saveFailedDialog" dojoType="dijit.Dialog" title="Ungültige Struktur">
              <div id="saveFailedDialogContentPane" dojoType="dijit.layout.ContentPane" style="width:300px; height:150px;">
                <label id="saveFailedMsg" class="saveFailedMsg">
                  <xsl:value-of select="i18n:translate('component.mets.msg.savefailed')" />
                </label>
                <label id="affectedItems" class="affectedItems" />

                <button dojoType="dijit.form.Button" id="okSaveFailed" label="Ok" iconClass="circleIcon" onclick="dijit.byId('saveFailedDialog').hide()"
                  style="float: right; padding-top: 2em;">
                </button>
              </div>
            </div>

            <!-- #################################### -->
            <!-- Definition of the set doctype dialog -->
            <!-- #################################### -->

            <div id="setDocumentTypeAndTitleDialog" dojoType="dijit.Dialog" title="Dokumenttyp/Titel festlegen">
              <div id="setDocumentTypeAndTitleDialogContentPane" dojoType="dijit.layout.ContentPane" style="height:210px; width:205px">
                <div dojoType="dijit.layout.ContentPane">
                  <div dojoType="dojo.data.ItemFileReadStore" jsId="DocumentTypeStore" url="js/stores/DocumentTypes.js"></div>
                  <div style="padding-top:0.5em;">
                    <label for="docTypeCombo">
                      <xsl:value-of select="i18n:translate('component.mets.msg.doctype')" />
                    </label>
                    <div>
                      <input id="docTypeCombo" dojoType="dijit.form.ComboBox" autoComplete="true"
                        invalidMessage="Bitte wählen Sie einen gültigen Dokumenttyp" type="text" forceValidOption="true" persist="false"
                        store="DocumentTypeStore">
                      </input>
                    </div>
                  </div>

                  <div style="padding-top:0.5em;">
                    <label for="titleTextArea">
                      <xsl:value-of select="i18n:translate('component.mets.msg.title')" />
                    </label>
                    <div>
                      <textarea id="titleTextBox" dojoType="dijit.form.SimpleTextarea" rows="4" cols="25"></textarea>
                    </div>
                  </div>

                </div>

                <div dojoType="dijit.layout.ContentPane">
                  <button dojoType="dijit.form.Button" id="saveTypeAndTitle" label="Speichern" iconClass="circleIcon" onclick="saveDocTypeAndTitle"
                    style="float: right; padding-top: 2em;">
                  </button>
                </div>
              </div>
            </div>

            <!-- ################################## -->
            <!-- Definition of the edit item dialog -->
            <!-- ################################## -->

            <div id="editItemDialog" dojoType="dijit.Dialog" title="Eigenschaften festlegen">
              <div id="upperEditItemDialogDialogContentPane" dojoType="dijit.layout.ContentPane" style="height:140px; width:300px">
                <div dojoType="dijit.layout.ContentPane">
                  <div style="padding-top:0.5em;">
                    <label for="orderLabelTextBox">
                      <xsl:value-of select="i18n:translate('component.mets.editor.pagenumber')" />
                    </label>
                    <div style="float:right;">
                      <input id="orderLabelTextBox" dojoType="dijit.form.TextBox" class="dijitTextBox" type="text" />
                    </div>
                  </div>
                </div>

                <div dojoType="dijit.layout.ContentPane">
                  <div style="padding-top:0.5em;">
                    <label for="commonLabelTextBox">
                      <xsl:value-of select="i18n:translate('component.mets.editor.label')" />
                    </label>
                    <div style="float:right;">
                      <input id="commonLabelTextBox" dojoType="dijit.form.TextBox" class="dijitTextBox" type="text" />
                    </div>
                  </div>
                </div>

                <div dojoType="dijit.layout.ContentPane">
                  <div style="padding-top:0.5em;">
                    <label for="checkDisplay">
                      <xsl:value-of select="i18n:translate('component.mets.editor.hide')" />
                    </label>
                    <div style="float:right;">
                      <input id="checkDisplay" dojoType="dijit.form.CheckBox" class="dijitCheckBox" checked="false" type="text" />
                    </div>
                  </div>
                </div>

                <div dojoType="dijit.layout.ContentPane">
                  <button dojoType="dijit.form.Button" id="saveItemProperties" label="Speichern" iconClass="circleIcon" onclick="saveItemProperties"
                    style="float: right; padding-top: 2em;">
                  </button>
                </div>
              </div>

            </div>

            <!-- ############################################# -->
            <!-- Definition of the confirm reload/reset dialog -->
            <!-- ############################################# -->

            <div id="confirmReloadDialog" dojoType="dijit.Dialog" title="Struktur zurücksetzen/neuladen?">
              <div id="confirmReloadDialogContentPane" dojoType="dijit.layout.ContentPane" style="width:300px; height:100px;">
                <label id="userInfoMsg">
                  <xsl:value-of select="i18n:translate('component.mets.msg.reload')" />
                  <br />
                </label>
                <button dojoType="dijit.form.Button" id="cancelReload" label="Abbrechen" iconClass="circleIcon"
                  onclick="dijit.byId('confirmReloadDialog').hide()" style="float: right; padding-top: 2em;" />
                <button dojoType="dijit.form.Button" id="okReload" label="Ok" iconClass="circleIcon" style="float: right; padding-top: 2em;"
                  onclick="redirect" />
              </div>
            </div>

            <!-- ############################# -->
            <!-- Definition of the help dialog -->
            <!-- ############################# -->

            <div id="viewHelpDialog" dojoType="dijit.Dialog" title="Hilfe">
              <div id="viewHelpDialogContentPane" dojoType="dijit.layout.ContentPane" href="pages/help.html" style="width:640px; height:480px;">
              </div>
            </div>

            <!-- Tooltips -->
            <span dojoType="dijit.Tooltip" connectId="toolbar1.save">
              <xsl:value-of select="i18n:translate('component.mets.editor.type.save')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="toolbar1.resetTree">
              <xsl:value-of select="i18n:translate('component.mets.editor.type.reset')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="toolbar1.reloadTree">
              <xsl:value-of select="i18n:translate('component.mets.editor.type.lastversion')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="toolbar1.setDocType">
              <xsl:value-of select="i18n:translate('component.mets.editor.type.title')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="toolbar1.addStructure">
              <xsl:value-of select="i18n:translate('component.mets.editor.type.add')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="toolbar1.editStructure">
              <xsl:value-of select="i18n:translate('component.mets.editor.type.edit')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="toolbar1.deleteStructure">
              <xsl:value-of select="i18n:translate('component.mets.editor.type.delete')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="toolbar1.foliation">
              <xsl:value-of select="i18n:translate('component.mets.editor.pagination')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="toolbar1.reverse">
              <xsl:value-of select="i18n:translate('component.mets.editor.order')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="toolbar1.help">
              <xsl:value-of select="i18n:translate('component.mets.editor.help')" />
            </span>

            <span dojoType="dijit.Tooltip" connectId="saveItemProperties">
              <xsl:value-of select="i18n:translate('component.mets.editor.apply')" />
            </span>
          </body>
        </html>
      </xsl:when>
      <xsl:otherwise>
        <html>
          <head>
            <title>
              <xsl:value-of select="concat('Mets Editor ',i18n:translate('component.mets.editor.accessDenied'))" />
            </title>
          </head>
          <body>
            <div id="noAccessMsg">
              <xsl:value-of select="i18n:translate('component.mets.editor.accessDenied.reason')" />
            </div>
          </body>
        </html>

      </xsl:otherwise>
    </xsl:choose>
    <xsl:comment>
      End - StartMetsEditor.xsl
    </xsl:comment>
  </xsl:template>
</xsl:stylesheet>
