<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions"
                xmlns:uuid="java:java.util.UUID"
                exclude-result-prefixes="iview2">
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="MCR.Viewer.bootstrapURL" />

  <xsl:output method="html" encoding="UTF-8" indent="yes" />

  <xsl:template match="/">
    <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html></xsl:text>
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="/IViewConfig">
    <xsl:variable name="target" select="normalize-space(xml/properties/property[@name='embedded'])" />
    <xsl:message>
      <xsl:value-of select="$target" />
    </xsl:message>
    <xsl:choose>
      <xsl:when test="string-length($target)&gt;0">
        <script>(function () {
  window["viewerLoader"] = window["viewerLoader"] || (function () {
    let executeOnReady = [];
    let notLoadedCount = 0;
    let scriptToLoad = [];

    let loader = {
      loadedStyles: function () {
        let existingCss = [];
        for (let i = 0; i &lt; document.styleSheets.length; i++) {
          let css = document.styleSheets[i];
          let src = css.src;
          if (src != null) {
            existingCss.push(src);
          }
        }
        return existingCss;
      },
      addConstructorExecution: function (fn) {
        executeOnReady.push(fn);
      },
      getLoadedScripts: function () {
        let existingScripts = [];

        for (let i = 0; i &lt; document.scripts.length; i++) {
          let script = document.scripts[i];
          let href = script.src;
          if (href != null) {
            existingScripts.push(href);
          }
        }
        return existingScripts;
      },
      addRequiredScripts: function (scripts) {
        scripts.filter(function (script) {
          return loader.getLoadedScripts().indexOf(script) === -1;
        }).forEach(function (scriptSrc) {
          let script = document.createElement('script');
          script.async = false;
          notLoadedCount++;
          script.onload = function() {
              console.log("loaded !");
            notLoadedCount--;

            if(notLoadedCount==0){
              let current;
              while((current=executeOnReady.pop()) != null){
                  current();
              }
            }
          };
          script.async = false;
          script.src = scriptSrc;
          document.head.appendChild(script);
        });
      },
      addRequiredCss: function (styles) {
        styles.filter(function (s) {
          return loader.loadedStyles().indexOf(s) === -1;
        }).forEach(function (style) {
          let link = document.createElement('link');
          link.rel = 'stylesheet';
          link.type = 'text/css';
          link.href = style;
          link.media = 'all';
          document.head.appendChild(link);
        });
      }
    };

    return loader;
  })();

// viewer dependency loader
  let configuration =<xsl:value-of select="json"/>;

// the target is encoded this should prevent cross site scripting
  let target = decodeURIComponent("<xsl:value-of select="$target" />");


  viewerLoader.addRequiredCss(configuration.resources.css);
  viewerLoader.addRequiredScripts(configuration.resources.script);
  viewerLoader.addConstructorExecution(function(){
          new mycore.viewer.MyCoReViewer(jQuery(target), configuration.properties);
  });

})
();</script>
      </xsl:when>
      <xsl:otherwise>
        <html>
          <head>
            <script type="text/javascript" src="//code.jquery.com/jquery-2.1.1.min.js"></script>
            <xsl:choose>
              <xsl:when test="xml/properties/property[@name='mobile'] = 'true'">
                <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no" />
                <link href="//netdna.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.css" rel="stylesheet"
                      type="text/css" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:if test="string-length($MCR.Viewer.bootstrapURL)&gt;0">
                  <script type="text/javascript" src="{$MCR.Viewer.bootstrapURL}/js/bootstrap.min.js"></script>
                  <link href="{$MCR.Viewer.bootstrapURL}/css/bootstrap.css" type="text/css" rel="stylesheet"></link>
                </xsl:if>
              </xsl:otherwise>
            </xsl:choose>

            <xsl:apply-templates select="xml/resources/resource" mode="iview.resource" />

            <script>
              window.onload = function() {
              var json = <xsl:value-of select="json" />;
              new mycore.viewer.MyCoReViewer(jQuery("body"), json.properties);
              };
            </script>
          </head>
          <body>
          </body>
        </html>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="resource" mode="iview.resource">
    <xsl:choose>
      <xsl:when test="@type='script'">
        <script src="{text()}" type="text/javascript" />
      </xsl:when>
      <xsl:when test="@type='css'">
        <link href="{text()}" type="text/css" rel="stylesheet"></link>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
