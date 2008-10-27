<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.13 $ $Date: 2007-11-06 16:57:10 $ -->
<!-- ============================================== -->
  <!--
    + | This stylesheet controls the Web-Layout of the MCRClassificationBrowser Servlet. | | This Template is embedded
    as a Part in the XML-Site, configurated in the Classification | section of the mycore.properties. | The complete XML
    stream is sent to the Layout Servlet and finally handled by this stylesheet. | | Authors: A.Schaar | Last changes:
    2005-30-10 +
  -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:acl="xalan://org.mycore.access.MCRAccessManager"
   exclude-result-prefixes="xlink i18n acl xsl">
  <xsl:include href="start-acl-editor.xsl"/>
  <xsl:variable name="Navigation.title" select="i18n:translate('component.classhandler.titles.pageTitle.classEdit')" />
  <xsl:variable name="MainTitle" select="i18n:translate('common.titles.mainTitle')" />
  <xsl:variable name="PageTitle" select="i18n:translate('component.classhandler.titles.pageTitle.classEdit')" />

  <!-- The main template -->
  <xsl:template match="classificationBrowser">
    <xsl:variable name="startPath" select="startPath" />
    <xsl:variable name="TrueFalse" select="showComments" />
    <div id="classificationBrowser">
      <xsl:variable name="type" select="startPath" />
      <xsl:variable name="search" select="searchField" />

      <!-- single classification listed -->
      <xsl:apply-templates select="navigationtree" />

      <!-- display list of classifications -->
      <xsl:apply-templates select="classificationlist" />
    </div>
  </xsl:template>
  <xsl:template match="navigationtree">
    <xsl:variable name="classifID" select="@classifID" />
    <xsl:variable name="label" select="../label" />
    <xsl:variable name="actcateg" select="@categID" />
    <xsl:variable name="userEdited" select="../userEdited" />
    <xsl:variable name="canEdit" select="../userCanEdit" />
    <xsl:variable name="path" select="concat($WebApplicationBaseURL, 'browse', ../uri ,'?mode=edit')" />
    <table cellspacing="0" cellpadding="0" style="width:100%; margin: 3% 0px 3% 2%;" class="bg_background">
      <tr>
        <td style="text-align:left;padding-left:33px;">
          <b>
            <xsl:value-of select="$label" />
          </b>
        </td>
        <td style="text-align:right;padding-right:88px;">
          <a href="{concat($WebApplicationBaseURL, 'browse?mode=edit')}">
            <xsl:value-of select="i18n:translate('component.classhandler.browse.showAllClass')" />
          </a>
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <xsl:value-of select="concat('[',$classifID,']')" />
          <xsl:if test="$userEdited != 'false'">
            <br />
            <b>
              <xsl:value-of select="i18n:translate('component.classhandler.browse.editError')" />
            </b>&#160;
            <xsl:value-of select="$userEdited" />
          </xsl:if>
			
		 <table cellspacing="1" cellpadding="2" style="margin: 3% 10px 3% 2%;" >

            <xsl:for-each select="row">

              <xsl:variable name="trStyle">
                <xsl:choose>
                  <xsl:when test="$actcateg = col[2]/@lineID">actrow</xsl:when>
                  <xsl:otherwise>classeditor</xsl:otherwise>
                </xsl:choose>
              </xsl:variable>
              <xsl:choose>
                <xsl:when test="$CurrentLang = 'ar'">
                  <tr valign="top">
                    <xsl:apply-templates select="." mode="editButtons">
                      <xsl:with-param name="cellStyle" select="'classeditor_arabic_s2_td2'" />
                    </xsl:apply-templates>
                    <td class="classeditor_arabic_s2_td1" width="25">&#160;&#160;&#160;</td>     
                    <xsl:apply-templates select="." mode="comment">
                      <xsl:with-param name="cellStyle" select="$trStyle" />
                    </xsl:apply-templates>
                    <xsl:apply-templates select="." mode="categoryText">
                      <xsl:with-param name="cellStyle" select="'classeditor_arabic_s2_td4'" />
                    </xsl:apply-templates>
                    <xsl:apply-templates select="." mode="folderColumn">
                      <xsl:with-param name="cellStyle" select="'classeditor_arabic_s2_td5'" />
                    </xsl:apply-templates>
            	  </tr>  			  
                </xsl:when>
                <xsl:otherwise>
                  <!-- language is not arabic -->				  
    			  <tr valign="top">
    				  
    				<xsl:apply-templates select="." mode="folderColumn">
                      <xsl:with-param name="cellStyle" select="$trStyle" />
                    </xsl:apply-templates>
                    <xsl:apply-templates select="." mode="categoryText">
                      <xsl:with-param name="cellStyle" select="$trStyle" />
                    </xsl:apply-templates>
                    <xsl:apply-templates select="." mode="comment">
                      <xsl:with-param name="cellStyle" select="$trStyle" />
                    </xsl:apply-templates>
                    <td class="{$trStyle}" width="25">&#160;&#160;&#160;</td>
                    <xsl:apply-templates select="." mode="editButtons">
                      <xsl:with-param name="cellStyle" select="$trStyle" />
                    </xsl:apply-templates>
    				  
                  </tr>			  
    			</xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>

            <tr>
              <td colspan="6">
                <xsl:if test="($canEdit = 'true')">
                  <hr />
                  <center>
                    <table>
                      <tr>
                        <td>
                          <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                            <input type="hidden" name="todo" value='create-category' />
                            <input type="hidden" name="todo2" value='modify-classification' />
                            <input type="hidden" name="path" value='{$path}' />
                            <input type="hidden" name="clid" value='{$classifID}' />
                            <input type="hidden" name="categid" value='empty' />
                            <input type="submit" class="button" name="newcateg" value="{i18n:translate('component.classhandler.browse.newCat')}" />
                          </form>
                        </td>
                        <td>
                          <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                            <input type="hidden" name="path" value='{$path}' />
                            <input type="hidden" name="clid" value='{$classifID}' />
                            <input type="hidden" name="todo" value='save-all' />
                            <input type="submit" class="button" name="saveAll" value="{i18n:translate('component.classhandler.browse.saveClass')}" />
                          </form>
                        </td>
                        <td>
                          <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                            <input type="hidden" name="path" value='{$path}' />
                            <input type="hidden" name="clid" value='{$classifID}' />
                            <input type="hidden" name="todo" value='purge-all' />
                            <input type="submit" class="button" name="purgeAll" value="{i18n:translate('component.classhandler.browse.discardClass')}" />
                          </form>
                        </td>
                      </tr>
                    </table>
                  </center>
                  <hr />
                </xsl:if>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </xsl:template>

  <xsl:template match="classificationlist">
    <xsl:variable name="path" select="concat($WebApplicationBaseURL, 'browse/',$HttpSession, '?mode=edit' )" />
    <xsl:variable name="canCreate" select="../userCanCreate" />

    <xsl:variable name="classnew" select="concat($WebApplicationBaseURL, 'images/classnew.gif')" />
    <xsl:variable name="classedit" select="concat($WebApplicationBaseURL, 'images/classedit.gif')" />
    <xsl:variable name="classdelete" select="concat($WebApplicationBaseURL, 'images/classdelete.gif')" />
    <xsl:variable name="classacl" select="concat($WebApplicationBaseURL, 'images/classacl.gif')" />
    <xsl:variable name="classup" select="concat($WebApplicationBaseURL, 'images/classup.gif')" />
    <xsl:variable name="classdown" select="concat($WebApplicationBaseURL, 'images/classdown.gif')" />
    <xsl:variable name="classleft" select="concat($WebApplicationBaseURL, 'images/classleft.gif')" />
    <xsl:variable name="classright" select="concat($WebApplicationBaseURL, 'images/classright.gif')" />
    <xsl:variable name="classexport" select="concat($WebApplicationBaseURL, 'images/classexport.gif')" />
    <xsl:variable name="imgEmpty" select="concat($WebApplicationBaseURL, 'images/emtyDot1Pix.gif')" />
    <xsl:variable name="use-aclEditor" select="acl:checkPermission('use-aclEditor')" />
	  
    <table cellspacing="0" cellpadding="0" border="0">
      <xsl:for-each select="classification">

        <xsl:variable name="browserClass" select="@browserClass" />
        <xsl:variable name="classifID" select="@ID" />
        <xsl:variable name="hasLinks" select="@hasLinks" />
        <xsl:variable name="categpath" select="concat($WebApplicationBaseURL, 'browse/',$browserClass, $HttpSession, '?mode=edit&amp;clid=',$classifID )" />
        <xsl:variable name="edited" select="@edited" />
        <xsl:variable name="userEdited" select="@userEdited" />
        <xsl:variable name="canEdit" select="@userCanEdit" />
        <xsl:variable name="canDelete" select="@userCanDelete" />
		  
		<xsl:choose>
        <xsl:when test="$CurrentLang = 'ar'">
		<tr valign="top" class="">
				
          <td nowrap="yes" class="classeditor_arabic_td1">			  
            &#160;	  			  
            <xsl:if test="($canEdit = 'true') and ($userEdited = 'false')">			
								
              <table cellpadding="0" cellspacing="0" height="16" valign="right">
				<tr valign="right" height="16" >
					
				  <td width="200">&#160;</td>	
				  <td width="25" valign="top">
                    <xsl:if test="($canDelete = 'true')">
                      <xsl:if test="$hasLinks = 'false'">
                        <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                          <input type="hidden" name="todo" value='delete-classification' />
                          <input type="hidden" name="path" value='{$categpath}' />
                          <input type="hidden" name="clid" value='{$classifID}' />
                          <input type="image" src='{$classdelete}' title="{i18n:translate('component.classhandler.browse.classDelete')}" />
                        </form>
                      </xsl:if>
                      <xsl:if test="$hasLinks != 'false'">
                        <img src="{$imgEmpty}" border="0" width="21" />
                      </xsl:if>
                    </xsl:if>
                  </td>
                  <xsl:if test="acl:checkPermission('use-aclEditor')">
                    <td width="25" valign="top">
                      <xsl:variable name="aclEditorAddress_edit">
                        <xsl:choose>
                          <xsl:when test="acl:hasRule($classifID, 'writedb')">
                            <xsl:call-template name="aclEditor.embMapping.getAddress">
                                <xsl:with-param name="objId" select="$classifID" />
                                <xsl:with-param name="action" select="'edit'" />
                            </xsl:call-template>
                          </xsl:when>
                          <xsl:otherwise>
                            <xsl:call-template name="aclEditor.embMapping.getAddress">
                                <xsl:with-param name="objId" select="$classifID" />
                                <xsl:with-param name="action" select="'add'" />
                                <xsl:with-param name="permission" select="'writedb'" />
                            </xsl:call-template>
                          </xsl:otherwise>
                        </xsl:choose>
                      </xsl:variable>
                      <a href="{$aclEditorAddress_edit}">
                          <img width="18" height="13" src="{$classacl}"
                              title="{i18n:translate('wcms.rightsManag.acl.edit')}" alt="{i18n:translate('component.classhandler.browse.classACL')}" />
                      </a>
                    </td>
                  </xsl:if>						
                  <xsl:if test="$edited='false'">
                    <td width="25" valign="top">
                      <a target="new" alt="$classifID" onclick="fensterCodice('{$WebApplicationBaseURL}servlets/MCRClassExportServlet?id={$classifID}');return false;"
                        href="{$WebApplicationBaseURL}servlets/MCRClassExportServlet?id={$classifID}{$HttpSession}">
                        <input onclick="fensterCodice('{$WebApplicationBaseURL}servlets/MCRClassExportServlet?id={$classifID}');return false;" type="image"
                          src='{$classexport}' title="{i18n:translate('component.classhandler.browse.classExport')}" />
                      </a>
                    </td>
                  </xsl:if>				  	
                  <td width="25" valign="top">
                    <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                      <input type="hidden" name="todo" value='modify-classification' />
                      <input type="hidden" name="path" value='{$categpath}' />
                      <input type="hidden" name="clid" value='{$classifID}' />
                      <input type="image" src='{$classedit}' title="{i18n:translate('component.classhandler.browse.classDescEdit')}" />
                    </form>
                  </td>
								
				</tr>				
              </table>				
            </xsl:if>
          </td>
		  <td class="classeditor_arabic_td2">
            <xsl:choose>
              <xsl:when test="$browserClass != ''">
                <a href='{$categpath}'>
                  <xsl:value-of select="concat(label/@text,'&#160;','[',@ID,']')" />
                </a>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="concat(label/@text,'&#160;','[',@ID,']&#160;(Browserpfad fehlt)')" />
              </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="$edited='true'">
              <span class="classEdited">
                <xsl:value-of select="concat(' ',i18n:translate('component.classhandler.browse.edited'))" />
              </span>
            </xsl:if>
            <br />
            <xsl:if test="label/@description != ''">
              <xsl:value-of select="label/@description" />
            </xsl:if>
          </td>
		</tr>			
		</xsl:when>
        <xsl:otherwise>	  
        <tr valign="top">	  
			
		  <td class="classeditor">
            <xsl:choose>
              <xsl:when test="$browserClass != ''">
                <a href='{$categpath}'>
                  <xsl:value-of select="concat(label/@text,'&#160;','[',@ID,']')" />
                </a>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="concat(label/@text,'&#160;','[',@ID,']&#160;(Browserpfad fehlt)')" />
              </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="$edited='true'">
              <span class="classEdited">
                <xsl:value-of select="concat(' ',i18n:translate('component.classhandler.browse.edited'))" />
              </span>
            </xsl:if>
            <br />
            <xsl:if test="label/@description != ''">
              <xsl:value-of select="label/@description" />
            </xsl:if>
          </td>
			
          <td nowrap="yes" class="classeditor">			  
            &#160;	  			  
            <xsl:if test="($canEdit = 'true') and ($userEdited = 'false')">			
								
              <table cellpadding="0" cellspacing="0" height="16">
				<tr valign="middle" height="16">					
                  <td width="25">&#160;</td>					
                  <td width="25" valign="top">
                    <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                      <input type="hidden" name="todo" value='modify-classification' />
                      <input type="hidden" name="path" value='{$categpath}' />
                      <input type="hidden" name="clid" value='{$classifID}' />
                      <input type="image" src='{$classedit}' title="{i18n:translate('component.classhandler.browse.classDescEdit')}" />
                    </form>
                  </td>					
                  <xsl:if test="$edited='false'">
                    <td width="25" valign="top">
                      <a target="new" alt="$classifID" onclick="fensterCodice('{$WebApplicationBaseURL}servlets/MCRClassExportServlet?id={$classifID}');return false;"
                        href="{$WebApplicationBaseURL}servlets/MCRClassExportServlet?id={$classifID}">
                        <input onclick="fensterCodice('{$WebApplicationBaseURL}servlets/MCRClassExportServlet?id={$classifID}{$HttpSession}');return false;" type="image"
                          src='{$classexport}' title="{i18n:translate('component.classhandler.browse.classExport')}" />
                      </a>
                    </td>
                  </xsl:if>					
                  <xsl:if test="$use-aclEditor">
                    <td width="25" valign="top">
                      <xsl:variable name="aclEditorAddress_edit">
                        <xsl:choose>
                          <xsl:when test="acl:hasRule($classifID, 'writedb')">
                            <xsl:call-template name="aclEditor.embMapping.getAddress">
                                <xsl:with-param name="objId" select="$classifID" />
                                <xsl:with-param name="action" select="'edit'" />
                            </xsl:call-template>
                          </xsl:when>
                          <xsl:otherwise>
                            <xsl:call-template name="aclEditor.embMapping.getAddress">
                                <xsl:with-param name="objId" select="$classifID" />
                                <xsl:with-param name="action" select="'add'" />
                                <xsl:with-param name="permission" select="'writedb'" />
                            </xsl:call-template>
                          </xsl:otherwise>
                        </xsl:choose>
                      </xsl:variable>
                      <a href="{$aclEditorAddress_edit}">
                          <img width="18" height="13" src="{$classacl}"
                              title="{i18n:translate('wcms.rightsManag.acl.edit')}" alt="{i18n:translate('component.classhandler.browse.classACL')}" />
                      </a>
                    </td>
                  </xsl:if>						
				  <td width="25" valign="top">
                    <xsl:if test="($canDelete = 'true')">
                      <xsl:if test="$hasLinks = 'false'">
                        <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                          <input type="hidden" name="todo" value='delete-classification' />
                          <input type="hidden" name="path" value='{$categpath}' />
                          <input type="hidden" name="clid" value='{$classifID}' />
                          <input type="image" src='{$classdelete}' title="{i18n:translate('component.classhandler.browse.classDelete')}" />
                        </form>
                      </xsl:if>
                      <xsl:if test="$hasLinks != 'false'">
                        <img src="{$imgEmpty}" border="0" width="21" />
                      </xsl:if>
                    </xsl:if>
                  </td>									
                </tr>
              </table>				
            </xsl:if>
          </td>			
        </tr>
	  </xsl:otherwise>
	  </xsl:choose>
		  
      </xsl:for-each>
      <xsl:if test="($canCreate = 'true')">
        <tr>
          <td colspan="4" align="center">
            <hr />
            <br />
            <table>
			  <xsl:choose>
              <xsl:when test="$CurrentLang = 'ar'">
			  <tr>
				<td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='purge-all' />
                    <input type="submit" class="button" name="purgeAll" value="{i18n:translate('component.classhandler.browse.discardClass')}" />
                  </form>
                </td>  
				<td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='save-all' />
                    <input type="submit" class="button" name="saveAll" value="{i18n:translate('component.classhandler.browse.saveClass')}" />
                  </form>
                </td> 
				<td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='import-classification' />
                    <input type="submit" class="button" name="importClass" value="{i18n:translate('component.classhandler.browse.importClass')}" />
                  </form>
                </td>  
				<td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='create-classification' />
                    <input type="submit" class="button" name="newClass" value="{i18n:translate('component.classhandler.browse.newClass')}" />
                  </form>
                </td>  
			  </tr>    
			  </xsl:when>
              <xsl:otherwise>
			  <tr>
                <td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='create-classification' />
                    <input type="submit" class="button" name="newClass" value="{i18n:translate('component.classhandler.browse.newClass')}" />
                  </form>
                </td>
                <td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='import-classification' />
                    <input type="submit" class="button" name="importClass" value="{i18n:translate('component.classhandler.browse.importClass')}" />
                  </form>
                </td>
                <td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='save-all' />
                    <input type="submit" class="button" name="saveAll" value="{i18n:translate('component.classhandler.browse.saveClass')}" />
                  </form>
                </td>
                <td>
                  <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
                    <input type="hidden" name="path" value='{$path}' />
                    <input type="hidden" name="clid" value='' />
                    <input type="hidden" name="todo" value='purge-all' />
                    <input type="submit" class="button" name="purgeAll" value="{i18n:translate('component.classhandler.browse.discardClass')}" />
                  </form>
                </td>
              </tr>
			  </xsl:otherwise>
	          </xsl:choose>
            </table>
            <hr />
          </td>
        </tr>
      </xsl:if>
    </table>
  </xsl:template>

  <!-- - - - - - - - - Identity Transformation  - - - - - - - - - -->

  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()' />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="comment">
    <xsl:apply-templates select='@*|node()' />
  </xsl:template>

  <xsl:template name="lineLevelLoop">
    <xsl:param name="anz" />
    <xsl:param name="img" />

    <xsl:if test="$anz > 0">
      <img border="0" width="10" src='{$img}' />
      <xsl:call-template name="lineLevelLoop">
        <xsl:with-param name="anz" select="$anz - 1" />
        <xsl:with-param name="img" select="$img" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>
  <xsl:include href="MyCoReLayout.xsl" />
  
  <xsl:template match="row" mode="folderColumn">
    <xsl:param name="cellStyle" />
    <td class="{$cellStyle}" nowrap="yes">
      <xsl:call-template name="lineLevelLoop">
        <xsl:with-param name="anz" select="col[1]/@lineLevel" />
        <xsl:with-param name="img" select="concat($WebApplicationBaseURL, 'images/folder_blank.gif')" />
      </xsl:call-template>
      <xsl:choose>
        <xsl:when test="col[1]/@plusminusbase">
          <a href="{concat($WebApplicationBaseURL, 'browse', col[2]/@searchbase,$HttpSession,'?mode=edit&amp;clid=',../@classifID)}">
            <img border="0" src="{concat($WebApplicationBaseURL, 'images/', col[1]/@folder1, '.gif')}" />
          </a>
        </xsl:when>
        <xsl:otherwise>
          <img border="0" src="{concat($WebApplicationBaseURL, 'images/', col[1]/@folder1, '.gif')}" />
        </xsl:otherwise>
      </xsl:choose>
    </td>
  </xsl:template>
  <xsl:template match="row" mode="categoryText">
    <xsl:param name="cellStyle" />
    <td class="{$cellStyle}">
      <xsl:choose>
        <xsl:when test="col[2]/@numDocs > 0 and col[2]/@searchbase != 'default' ">
          <a href="{concat($ServletsBaseURL, 'MCRSearchServlet',$HttpSession,'?query=(',../@searchField,'+=+', col[2]/@lineID, ')+and+',../@doctype,'&amp;numPerPage=10','&amp;mask=browse/',../startPath)}">
            <xsl:value-of select="col[2]/text()" />
          </a>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="col[2]/text()" />
        </xsl:otherwise>
      </xsl:choose>
    </td>
  </xsl:template>
  <xsl:template match="row" mode="comment">
    <xsl:param name="cellStyle" />
    <td class="{$cellStyle}">
      <xsl:choose>
        <xsl:when test="col[2]/comment != ''">
          <i>
            (
            <xsl:apply-templates select="col[2]/comment" />
            )
          </i>
        </xsl:when>
        <xsl:otherwise>&#160;
        </xsl:otherwise>
      </xsl:choose>
    </td>
  </xsl:template>
  <xsl:template match="row" mode="editButton">
    <xsl:param name="todo" />
    <xsl:param name="todo2" select="'modify-classification'" />
    <xsl:param name="path" select="concat($WebApplicationBaseURL, 'browse', ../../uri,'?mode=edit')" />
    <xsl:param name="image"/>
    <xsl:param name="imageTitle"/>
    <xsl:param name="notEmpty" select="true()"/>
    <xsl:param name="width" select="23"/>
    <xsl:variable name="imgEmpty" select="concat($WebApplicationBaseURL, 'images/emtyDot1Pix.gif')" />   
    <td width="{$width}">
      <xsl:choose>
        <xsl:when test="$notEmpty = true()">
          <form action="{$WebApplicationBaseURL}servlets/MCRStartClassEditorServlet{$HttpSession}" method="get">
            <input type="hidden" name="todo" value="{$todo}" />
            <input type="hidden" name="todo2" value="{$todo2}" />
            <input type="hidden" name="path" value="{$path}" />
            <input type="hidden" name="clid" value="{../@classifID}" />
            <input type="hidden" name="categid" value="{col[2]/@lineID}" />
            <input type="image" src="{$image}" title="{$imageTitle}" />
          </form>
        </xsl:when>
        <xsl:otherwise>
          <img src="{$imgEmpty}" border="0" width="{$width}" />
        </xsl:otherwise>
      </xsl:choose>
    </td>
  </xsl:template>
  
  <xsl:template match="row" mode="editButtons">
    <xsl:param name="cellStyle" />
    <xsl:variable name="childpos" select="col[1]/@childpos" />
    <td class="{$cellStyle}">
      <xsl:choose>
        <xsl:when test="col[1]/@folder1 = 'folder_minus' ">
            <!-- leer  weil hier nur die Rückreferenz aufs parent kommt
                 geht anders bestimmt schöner -->&#160;
        </xsl:when>
        <xsl:otherwise>
          <!-- Wurzel-, Kindknoten mit und ohne Dokumenten -->
          <xsl:if test="(../../userCanEdit = 'true') and ((../../userEdited='false') or ((../../session='') or (../../session=../../currentSession))) ">
            <table cellpadding="0" cellspacing="0" height="16">
              <tr valign="middle" height="16">
                <xsl:apply-templates select="." mode="editButton">
                  <xsl:with-param name="todo" select="'create-category'"/>
                  <xsl:with-param name="image" select="concat($WebApplicationBaseURL, 'images/classnew.gif')"/>
                  <xsl:with-param name="imageTitle" select="i18n:translate('component.classhandler.browse.newUnderCatInsert')"/>
                </xsl:apply-templates>
                <xsl:apply-templates select="." mode="editButton">
                  <xsl:with-param name="todo" select="'modify-category'"/>
                  <xsl:with-param name="image" select="concat($WebApplicationBaseURL, 'images/classedit.gif')"/>
                  <xsl:with-param name="imageTitle" select="i18n:translate('component.classhandler.browse.editCat')"/>
                </xsl:apply-templates>
                <xsl:apply-templates select="." mode="editButton">
                  <xsl:with-param name="todo" select="'delete-category'"/>
                  <xsl:with-param name="image" select="concat($WebApplicationBaseURL, 'images/classdelete.gif')"/>
                  <xsl:with-param name="imageTitle" select="i18n:translate('component.classhandler.browse.deleteCat')"/>
                  <xsl:with-param name="notEmpty" select="(col[2]/@hasLinks = 'false') and (../../userCanEdit = 'true')"/>
                </xsl:apply-templates>
                <xsl:apply-templates select="." mode="editButton">
                  <xsl:with-param name="todo" select="'up-category'"/>
                  <xsl:with-param name="image" select="concat($WebApplicationBaseURL, 'images/classup.gif')"/>
                  <xsl:with-param name="imageTitle" select="i18n:translate('component.classhandler.browse.moveUp')"/>
                  <xsl:with-param name="notEmpty" select="$childpos = 'last' or $childpos = 'middle'"/>
                  <xsl:with-param name="width" select="16"/>
                </xsl:apply-templates>
                <xsl:apply-templates select="." mode="editButton">
                  <xsl:with-param name="todo" select="'down-category'"/>
                  <xsl:with-param name="image" select="concat($WebApplicationBaseURL, 'images/classdown.gif')"/>
                  <xsl:with-param name="imageTitle" select="i18n:translate('component.classhandler.browse.moveDown')"/>
                  <xsl:with-param name="notEmpty" select="$childpos = 'first' or $childpos = 'middle'"/>
                  <xsl:with-param name="width" select="16"/>
                </xsl:apply-templates>
                <xsl:apply-templates select="." mode="editButton">
                  <xsl:with-param name="todo" select="'left-category'"/>
                  <xsl:with-param name="image" select="concat($WebApplicationBaseURL, 'images/classleft.gif')"/>
                  <xsl:with-param name="imageTitle" select="i18n:translate('component.classhandler.browse.moveLeft')"/>
                  <xsl:with-param name="notEmpty" select="col[1]/@lineLevel > 1"/>
                  <xsl:with-param name="width" select="16"/>
                </xsl:apply-templates>
                <xsl:apply-templates select="." mode="editButton">
                  <xsl:with-param name="todo" select="'right-category'"/>
                  <xsl:with-param name="image" select="concat($WebApplicationBaseURL, 'images/classright.gif')"/>
                  <xsl:with-param name="imageTitle" select="i18n:translate('component.classhandler.browse.moveRight')"/>
                  <xsl:with-param name="notEmpty" select="$childpos = 'last' or $childpos = 'middle'"/>
                  <xsl:with-param name="width" select="16"/>
                </xsl:apply-templates>
              </tr>
            </table>
          </xsl:if>
        </xsl:otherwise>
      </xsl:choose>
    </td>			  
  </xsl:template>

</xsl:stylesheet>
