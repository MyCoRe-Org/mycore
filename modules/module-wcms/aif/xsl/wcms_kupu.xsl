<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" >
	<xsl:param name="MCR.WCMS.KUPU.kupuToolboxAddLinks" />
      <xsl:param name="MCR.WCMS.KUPU.kupuToolboxAddImages" />
      <xsl:param name="MCR.WCMS.KUPU.kupuToolboxAddTables" />
      <xsl:param name="MCR.WCMS.KUPU.kupuToolboxDebug" />
      <!-- ================================================================================= -->
	<xsl:template name="kupu">
		<div style="display: none;">
			<xml id="kupuconfig">
				<kupuconfig>
					<dst>fulldoc.html</dst>
					<use_css>1</use_css>
					<reload_after_save>0</reload_after_save>
					<strict_output>1</strict_output>
					<content_type>application/xhtml+xml</content_type>
					<compatible_singletons>0</compatible_singletons>
					<table_classes>
						<class>plain</class>
						<class>listing</class>
						<class>grid</class>
						<class>data</class>
					</table_classes>
				</kupuconfig>
			</xml>
		</div>
		<div class="kupu-fulleditor">
			<div class="kupu-tb" id="toolbar">
				<span id="kupu-tb-buttons">
					<span class="kupu-tb-buttongroup" style="float: right" id="kupu=logo">
						<button type="button" class="kupu-logo" title="Kupu 1.1" accesskey="k" 
							onclick="window.open('http://kupu.oscom.org');">&#xA0;</button>
					</span>
					<select id="kupu-tb-styles">
						<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="P" i18n:translate="paragraph-normal"> 
							Normal </option>
						<option value="H1"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" 
							i18n:translate="heading">Heading</span> 1 </option>
						<option value="H2"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" 
							i18n:translate="heading">Heading</span> 2 </option>
						<option value="H3"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" 
							i18n:translate="heading">Heading</span> 3 </option>
						<option value="H4"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" 
							i18n:translate="heading">Heading</span> 4 </option>
						<option value="H5"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" 
							i18n:translate="heading">Heading</span> 5 </option>
						<option value="H6"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" 
							i18n:translate="heading">Heading</span> 6 </option>
						<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="PRE" 
							i18n:translate="paragraph-formatted"> Formatted </option>
					</select>
					<!--
					<span class="kupu-tb-buttongroup">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-save" 
							id="kupu-save-button" title="save: alt-s" i18n:attributes="title" accesskey="s">&#xA0;</button>
					</span>
					-->
					<span class="kupu-tb-buttongroup" id="kupu-bg-basicmarkup">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-bold" 
							id="kupu-bold-button" title="bold: alt-b" i18n:attributes="title" accesskey="b">&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-italic" 
							id="kupu-italic-button" title="italic: alt-i" i18n:attributes="title" accesskey="i"> 
							&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-underline" 
							id="kupu-underline-button" title="underline: alt-u" i18n:attributes="title" accesskey="u"> 
							&#xA0;</button>
					</span>
					<span class="kupu-tb-buttongroup" id="kupu-bg-subsuper">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-subscript" 
							id="kupu-subscript-button" title="subscript: alt--" i18n:attributes="title" accesskey="-"> 
							&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-superscript" 
							id="kupu-superscript-button" title="superscript: alt-+" i18n:attributes="title" accesskey="+"> 
							&#xA0;</button>
					</span>
					<span class="kupu-tb-buttongroup">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-forecolor" 
							id="kupu-forecolor-button" title="text color: alt-f" i18n:attributes="title" accesskey="f"> 
							&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-hilitecolor" 
							id="kupu-hilitecolor-button" title="background color: alt-h" i18n:attributes="title" 
							accesskey="h">&#xA0;</button>
					</span>
					<span class="kupu-tb-buttongroup" id="kupu-bg-justify">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-justifyleft" 
							id="kupu-justifyleft-button" title="left justify: alt-l" i18n:attributes="title" accesskey="l"> 
							&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-justifycenter" 
							id="kupu-justifycenter-button" title="center justify: alt-c" i18n:attributes="title" 
							accesskey="c">&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-justifyright" 
							id="kupu-justifyright-button" title="right justify: alt-r" i18n:attributes="title" accesskey="r"> 
							&#xA0;</button>
					</span>
					<span class="kupu-tb-buttongroup" id="kupu-bg-list">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-insertorderedlist" 
							title="numbered list: alt-#" id="kupu-list-ol-addbutton" i18n:attributes="title" accesskey="#"> 
							&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-insertunorderedlist" 
							title="unordered list: alt-*" id="kupu-list-ul-addbutton" i18n:attributes="title" accesskey="*"> 
							&#xA0;</button>
					</span>
					<span class="kupu-tb-buttongroup" id="kupu-bg-definitionlist">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" 
							class="kupu-insertdefinitionlist" title="definition list: alt-=" id="kupu-list-dl-addbutton" 
							i18n:attributes="title" accesskey="=">&#xA0;</button>
					</span>
					<span class="kupu-tb-buttongroup" id="kupu-bg-indent">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-outdent" 
							id="kupu-outdent-button" title="outdent: alt-&lt;" i18n:attributes="title" accesskey="&lt;"> 
							&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-indent" 
							id="kupu-indent-button" title="indent: alt-&gt;" i18n:attributes="title" accesskey="&gt;"> 
							&#xA0;</button>
					</span>
					<span class="kupu-tb-buttongroup" id="kupu-bg-remove">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" 
							class="kupu-removeimage invisible" id="kupu-removeimage-button" title="Remove image" 
							i18n:attributes="title">&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" 
							class="kupu-removelink invisible" id="kupu-removelink-button" title="Remove link" 
							i18n:attributes="title">&#xA0;</button>
					</span>
					<span class="kupu-tb-buttongroup" id="kupu-bg-undo">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-undo" 
							id="kupu-undo-button" title="undo: alt-z" i18n:attributes="title" accesskey="z">&#xA0;</button>
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-redo" 
							id="kupu-redo-button" title="redo: alt-y" i18n:attributes="title" accesskey="y">&#xA0;</button>
					</span>
					<span class="kupu-tb-buttongroup">
						<button xmlns:i18n="http://xml.zope.org/namespaces/i18n" type="button" class="kupu-source" 
							id="kupu-source-button" title="Edit source" i18n:attributes="title">&#xA0;</button>
					</span>
				</span>
				<select id="kupu-ulstyles">
					<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="disc" i18n:translate="list-disc"> 
						&#x25CF;</option>
					<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="square" i18n:translate="list-square"> 
						&#x25A0;</option>
					<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="circle" i18n:translate="list-circle"> 
						&#x25CB;</option>
					<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="none" i18n:translate="list-nobullet">no 
						bullet</option>
				</select>
				<select id="kupu-olstyles">
					<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="decimal" i18n:translate="list-decimal"> 
						1</option>
					<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="upper-roman" i18n:translate="list-upperroman">
						 I</option>
					<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="lower-roman" i18n:translate="list-lowerroman">
						 i</option>
					<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="upper-alpha" i18n:translate="list-upperalpha">
						 A</option>
					<option xmlns:i18n="http://xml.zope.org/namespaces/i18n" value="lower-alpha" i18n:translate="list-loweralpha">
						 a</option>
				</select>
			</div>
			<div xmlns="" class="kupu-toolboxes">
                        <h1  xmlns:i18n="http://xml.zope.org/namespaces/i18n" >HTML-Codekorrektur</h1>
				<input type="checkbox" name="codeValidationDisable" value="true"><text></text></input>
                        Codekorrektur ausschalten 
                        <br/>                        
				<div style="visibility:hidden;" class="kupu-toolbox" id="kupu-toolbox-properties">
					<div class="kupu-toolbox-label"></div>
					<input id="kupu-properties-title" type="hidden"/>
					<div class="kupu-toolbox-label"></div>
					<input id="kupu-properties-description" type="hidden"/>
				</div>
                        <xsl:choose>
                              <xsl:when test="$MCR.WCMS.KUPU.kupuToolboxAddLinks = 'true' " >
                                    <div class="kupu-toolbox" id="kupu-toolbox-links">
                                          <h1 xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate="links">Links</h1>
                                          <div id="kupu-toolbox-addlink">
                                                <div class="kupu-toolbox-label"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" 
                                                      i18n:translate="items-matching-keyword"> Link the highlighted text to this URL </span>: </div>
                                                <input id="kupu-link-input" class="kupu-toolbox-st" type="text" style="width: 98%"/>
                                                <div style="text-align: center">
                                                      <button type="button" id="kupu-link-button" class="kupu-toolbox-action">Make Link</button>
                                                </div>
                                          </div>
                                    </div>                                    
                              </xsl:when>
                              <xsl:otherwise>
                                    <div style="visibility:hidden;" class="kupu-toolbox" id="kupu-toolbox-links">
                                          <h1 xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate="links">Links</h1>
                                          <div id="kupu-toolbox-addlink">
                                                <div class="kupu-toolbox-label"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" 
                                                      i18n:translate="items-matching-keyword"> Link the highlighted text to this URL </span>: </div>
                                                <input id="kupu-link-input" class="kupu-toolbox-st" type="text" style="width: 98%"/>
                                                <div style="text-align: center">
                                                      <button type="button" id="kupu-link-button" class="kupu-toolbox-action">Make Link</button>
                                                </div>
                                          </div>
                                    </div>                                                                        
                              </xsl:otherwise>
                        </xsl:choose>
                        <xsl:choose>
                              <xsl:when test="$MCR.WCMS.KUPU.kupuToolboxAddImages = 'true' " >
                                    <div class="kupu-toolbox" id="kupu-toolbox-images">
                                    <h1 xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate="images">Images</h1>
                                          <div class="kupu-toolbox-label"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate=""> 
                                          Insert image at the following URL </span>: </div>
                                          <input id="kupu-image-input" value="kupuimages/kupu_icon.gif" class="kupu-toolbox-st" type="text" 
                                          style="width: 98%"/>
                                                <div class="kupu-toolbox-label"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate=""> 
                                                Image float </span>: </div>
                                                <select id="kupu-image-float-select" class="kupu-toolbox-st">
                                                      <option value="none">No float</option>
                                                      <option value="left">Left</option>
                                                      <option value="right">Right</option>
                                                </select>
                                          <div style="text-align: center">
                                          <button type="button" id="kupu-image-addbutton" class="kupu-toolbox-action">Insert Image</button>
                                          </div>
                                    </div>
                              </xsl:when>
                              <xsl:otherwise>
                                    <div style="visibility:hidden;" class="kupu-toolbox" id="kupu-toolbox-images">
                                    <h1 xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate="images">Images</h1>
                                          <div class="kupu-toolbox-label"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate=""> 
                                          Insert image at the following URL </span>: </div>
                                          <input id="kupu-image-input" value="kupuimages/kupu_icon.gif" class="kupu-toolbox-st" type="text" 
                                          style="width: 98%"/>
                                                <div class="kupu-toolbox-label"><span xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate=""> 
                                                Image float </span>: </div>
                                                <select id="kupu-image-float-select" class="kupu-toolbox-st">
                                                      <option value="none">No float</option>
                                                      <option value="left">Left</option>
                                                      <option value="right">Right</option>
                                                </select>
                                          <div style="text-align: center">
                                          <button type="button" id="kupu-image-addbutton" class="kupu-toolbox-action">Insert Image</button>
                                          </div>
                                    </div>
                              </xsl:otherwise>
                        </xsl:choose>
                        <xsl:choose>
                              <xsl:when test="$MCR.WCMS.KUPU.kupuToolboxAddTables = 'true' " >
				<div class="kupu-toolbox" id="kupu-toolbox-tables">
					<h1 xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate="table-inspector">Tables</h1>
					<div class="kupu-toolbox-label">Table Class <select id="kupu-table-classchooser"> </select> </div>
					<div id="kupu-toolbox-addtable">
						<div class="kupu-toolbox-label">Rows</div>
						<input type="text" id="kupu-table-newrows" style="width: 98%"/>
						<div class="kupu-toolbox-label">Columns</div>
						<input type="text" id="kupu-table-newcols" style="width: 98%"/>
						<div class="kupu-toolbox-label"> Headings <input name="kupu-table-makeheader" 
							id="kupu-table-makeheader" type="checkbox"/> <label for="kupu-table-makeheader">Create</label> 
							</div>
						<div style="text-align: center">
							<button type="button" id="kupu-table-fixall-button">Fix Table</button>
							<button type="button" id="kupu-table-addtable-button">Add Table</button>
						</div>
					</div>
					<div id="kupu-toolbox-edittable">
						<div class="kupu-toolbox-label">Col Align <select id="kupu-table-alignchooser"><option 
							value="left">Left</option><option value="center">Center</option><option 
							value="right">Right</option></select> </div>
						<br/>
						<button type="button" id="kupu-table-addcolumn-button">Add Column</button>
						<button type="button" id="kupu-table-delcolumn-button">Remove Column</button>
						<br/>
						<button type="button" id="kupu-table-addrow-button">Add Row</button>
						<button type="button" id="kupu-table-delrow-button">Remove Row</button>
						<div style="text-align: center">
							<button type="button" id="kupu-table-fix-button">Fix</button>
						</div>
					</div>
				</div>
                        </xsl:when>
                        <xsl:otherwise>
                              <div style="visibility:hidden;" class="kupu-toolbox" id="kupu-toolbox-tables">
					<h1 xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate="table-inspector">Tables</h1>
					<div class="kupu-toolbox-label">Table Class <select id="kupu-table-classchooser"> </select> </div>
					<div id="kupu-toolbox-addtable">
						<div class="kupu-toolbox-label">Rows</div>
						<input type="text" id="kupu-table-newrows" style="width: 98%"/>
						<div class="kupu-toolbox-label">Columns</div>
						<input type="text" id="kupu-table-newcols" style="width: 98%"/>
						<div class="kupu-toolbox-label"> Headings <input name="kupu-table-makeheader" 
							id="kupu-table-makeheader" type="checkbox"/> <label for="kupu-table-makeheader">Create</label> 
							</div>
						<div style="text-align: center">
							<button type="button" id="kupu-table-fixall-button">Fix Table</button>
							<button type="button" id="kupu-table-addtable-button">Add Table</button>
						</div>
					</div>
					<div id="kupu-toolbox-edittable">
						<div class="kupu-toolbox-label">Col Align <select id="kupu-table-alignchooser"><option 
							value="left">Left</option><option value="center">Center</option><option 
							value="right">Right</option></select> </div>
						<br/>
						<button type="button" id="kupu-table-addcolumn-button">Add Column</button>
						<button type="button" id="kupu-table-delcolumn-button">Remove Column</button>
						<br/>
						<button type="button" id="kupu-table-addrow-button">Add Row</button>
						<button type="button" id="kupu-table-delrow-button">Remove Row</button>
						<div style="text-align: center">
							<button type="button" id="kupu-table-fix-button">Fix</button>
						</div>
					</div>
				</div>
                        </xsl:otherwise>
                        </xsl:choose>
                        <xsl:choose>
                              <xsl:when test="$MCR.WCMS.KUPU.kupuToolboxDebug = 'true' " >
				<div class="kupu-toolbox" id="kupu-toolbox-debug">
					<h1 xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate="debug-log">Debug Log</h1>
					<div id="kupu-toolbox-debuglog" class="kupu-toolbox-label" style="height: 10px;">
					</div>
				</div>
                        </xsl:when>
                        <xsl:otherwise>
                              <div style="visibility:hidden;" class="kupu-toolbox" id="kupu-toolbox-debug">
					<h1 xmlns:i18n="http://xml.zope.org/namespaces/i18n" i18n:translate="debug-log">Debug Log</h1>
					<div id="kupu-toolbox-debuglog" class="kupu-toolbox-label" style="height: 10px;">
					</div>
				</div>
                        </xsl:otherwise>
                        </xsl:choose>
			</div>
			<table id="kupu-colorchooser" cellpadding="0" cellspacing="0" 
				style="position: fixed; border-style: solid; border-color: black; border-width: 1px;">
			</table>

			<div class="kupu-editorframe">
				<form>
					<iframe id="kupu-editor" frameborder="0" 
						src=" {concat($WebApplicationBaseURL,'servlets/WCMSGetStaticHTMLServlet?href=',/cms/href,'&amp;lang=',$CurrentLang) }" 
						scrolling="auto">
					</iframe>
					<xsl:choose>
						<xsl:when test="/cms/action = 'translate'">
							<textarea id="kupu-editor-textarea" name="content_currentLang" style="display: none">
										<xsl:text>
										</xsl:text>								
							</textarea>
						</xsl:when>
						<xsl:otherwise>
							<textarea id="kupu-editor-textarea" name="content" style="display: none">
										<xsl:text>
										</xsl:text>								
							</textarea>							
						</xsl:otherwise>
					</xsl:choose>

				</form>
			</div>
			
		</div>

	</xsl:template>
	<!-- ================================================================================= -->
</xsl:stylesheet>