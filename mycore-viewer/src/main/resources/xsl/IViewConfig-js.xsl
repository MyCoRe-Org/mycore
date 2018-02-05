<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions"
                exclude-result-prefixes="iview2">
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="MCR.Viewer.bootstrapURL" />

  <xsl:output method="text" media-type="text/javascript" indent="no" />

  <xsl:template match="/">
    (function () {
  window["viewerLoader"] = window["viewerLoader"] || (function () {
    let executeOnReady = [];
    let notLoadedCount = 0;
    let scriptToLoad = [];

    let loader = {
      loadedStyles: function () {
        let existingCss = [];
    for (let i = 0; i <xsl:text>&#60;</xsl:text> document.styleSheets.length; i++) {
          let css = document.styleSheets[i];
          let src = css.href;
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

    for (let i = 0; i <xsl:text>&#60;</xsl:text> document.scripts.length; i++) {
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
  let configuration =<xsl:value-of select="IViewConfig/json"/>;

  viewerLoader.addRequiredCss(configuration.resources.css);
  viewerLoader.addRequiredScripts(configuration.resources.script);
  viewerLoader.addConstructorExecution(function(){
          let container = jQuery("[data-viewer='"+configuration.properties.derivate+":"+configuration.properties.filePath+"']");
          new mycore.viewer.MyCoReViewer(container, configuration.properties);
  });

})
();
  </xsl:template>

</xsl:stylesheet>
