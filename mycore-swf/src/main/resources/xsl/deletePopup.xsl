<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2009/03/05 14:10:08 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mcr="http://www.mycore.org/" xmlns:acl="xalan://org.mycore.access.MCRAccessManager"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:xalan="http://xml.apache.org/xalan"
  exclude-result-prefixes="xlink mcr xalan i18n acl">


  <xsl:param name="re_mcrid" />
  <xsl:param name="se_mcrid" /> 
     
    
  <!-- default title line -->
  <xsl:template match="/mycoreobject" mode="title" priority="0">
    <xsl:value-of select="@ID"/>
  </xsl:template>  
  
  <xsl:template match="deleteDerivate"> 

      <xsl:choose>
        <xsl:when test="$direction = 'rtl'">
          
          <div class="secureRequest">
            <div class="secureRequest1-rtl">
              <span class="secureRequestText-rtl">
                <xsl:value-of select="i18n:translate('buttons.derivate.delete.text')" />
              </span>              
              <span class="secureRequestIMG-rtl">
                <img src="{$WebApplicationBaseURL}images/nav-empty.gif"/>
              </span>
            </div>
            <div class="secureRequest2-rtl">
              <span class="secureRequestCancel">
                <input type="button" class="editorButton" onClick="history.back();" >
                  <xsl:attribute name="value">
                    <xsl:value-of select="i18n:translate('buttons.cancel')" />
                  </xsl:attribute>
                </input>  
              </span>              
              <span class="secureRequestDelete">
                <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$re_mcrid}&amp;re_mcrid={$re_mcrid}&amp;se_mcrid={$se_mcrid}&amp;todo=sdelder">
                  <input type="button" class="editorButton">
                    <xsl:attribute name="value">
                      <xsl:value-of select="i18n:translate('buttons.derivate.delete.yes')" />
                    </xsl:attribute>
                  </input>     
                </a>          
              </span>
            </div>        
          </div>
          
        </xsl:when>
        <xsl:otherwise>

          <div class="secureRequest">
            <div class="secureRequest1">
              <span class="secureRequestIMG">
                <img src="{$WebApplicationBaseURL}images/nav-empty.gif"/>
              </span>
              <span class="secureRequestText">
                <xsl:value-of select="i18n:translate('buttons.derivate.delete.text')" />
              </span>
            </div>
            <div class="secureRequest2">
              <span class="secureRequestDelete">
                <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$re_mcrid}&amp;re_mcrid={$re_mcrid}&amp;se_mcrid={$se_mcrid}&amp;todo=sdelder">
                <input type="button" class="editorButton">
                    <xsl:attribute name="value">
                      <xsl:value-of select="i18n:translate('buttons.derivate.delete.yes')" />
                    </xsl:attribute>
                  </input>     
                </a>          
              </span>
              <span class="secureRequestCancel">
                <input type="button" class="editorButton" onClick="history.back();" >
                  <xsl:attribute name="value">
                    <xsl:value-of select="i18n:translate('buttons.cancel')" />
                  </xsl:attribute>
                </input>  
              </span>
            </div>        
          </div>
                              
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

  <xsl:template match="deleteObject"> 

      <xsl:choose>
        <xsl:when test="$direction = 'rtl'">
          
          <div class="secureRequest">
            <div class="secureRequest1-rtl">
              <span class="secureRequestText-rtl">
                <xsl:value-of select="i18n:translate('buttons.derivate.delete.text')" />
              </span>              
              <span class="secureRequestIMG-rtl">
                <img src="{$WebApplicationBaseURL}images/nav-empty.gif"/>
              </span>
            </div>
            <div class="secureRequest2-rtl">
              <span class="secureRequestCancel">
                <input type="button" class="editorButton" onClick="history.back();" >
                  <xsl:attribute name="value">
                    <xsl:value-of select="i18n:translate('buttons.cancel')" />
                  </xsl:attribute>
                </input>  
              </span>              
              <span class="secureRequestDelete">
                <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$re_mcrid}&amp;re_mcrid={$re_mcrid}&amp;se_mcrid={$se_mcrid}&amp;step=commit&amp;todo=sdelobj">
                  <input type="button" class="editorButton">
                    <xsl:attribute name="value">
                      <xsl:value-of select="i18n:translate('buttons.derivate.delete.yes')" />
                    </xsl:attribute>
                  </input>     
                </a>          
              </span>
            </div>        
          </div>
          
        </xsl:when>
        <xsl:otherwise>

          <div class="secureRequest">
            <div class="secureRequest1">
              <span class="secureRequestIMG">
                <img src="{$WebApplicationBaseURL}images/nav-empty.gif"/>
              </span>
              <span class="secureRequestText">
                <xsl:value-of select="i18n:translate('buttons.derivate.delete.text')" />
              </span>
            </div>
            <div class="secureRequest2">
              <span class="secureRequestDelete">
                <a href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$re_mcrid}&amp;re_mcrid={$re_mcrid}&amp;se_mcrid={$se_mcrid}&amp;step=commit&amp;todo=sdelobj">
                <input type="button" class="editorButton">
                    <xsl:attribute name="value">
                      <xsl:value-of select="i18n:translate('buttons.derivate.delete.yes')" />
                    </xsl:attribute>
                  </input>     
                </a>          
              </span>
              <span class="secureRequestCancel">
                <input type="button" class="editorButton" onClick="history.back();" >
                  <xsl:attribute name="value">
                    <xsl:value-of select="i18n:translate('buttons.cancel')" />
                  </xsl:attribute>
                </input>  
              </span>
            </div>        
          </div>
                              
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>
    
</xsl:stylesheet>