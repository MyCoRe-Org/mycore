<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions"
                exclude-result-prefixes="iview2">
  <xsl:param name="WebApplicationBaseURL" />

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
      addRequiredScripts: function (scripts, module) {
        if(!scripts) return;
        if(!loader.isBootstrapPresent()){
            notLoadedCount++;
            let interval = window.setInterval(function(){
                if(loader.isBootstrapPresent()){
                    notLoadedCount--;
                    window.clearInterval(interval);
                    loader.excecuteOnReadyFn();
                }
            }, 50);
        }
        scripts.filter(function (script) {
          return loader.getLoadedScripts().indexOf(script) === -1;
        }).forEach(function (scriptSrc) {
          let script = document.createElement('script');
          script.async = false;
          script.type = module ? 'module' : 'text/javascript';
          notLoadedCount++;

        script.onload = function() {
            notLoadedCount--;
            loader.excecuteOnReadyFn();
          };
          script.async = false;
          script.src = scriptSrc;
          document.head.appendChild(script);
        });


      },
      excecuteOnReadyFn: function(){
        if(notLoadedCount==0){
          let current;
          while((current=executeOnReady.pop()) != null){
            current();
          }
        }
      },
      isBootstrapPresent: function(){
        return  typeof $ !='undefined' &amp;&amp;
                typeof $.fn !='undefined' &amp;&amp;
                typeof $.fn.tooltip !='undefined' &amp;&amp;
                typeof $.fn.tooltip.Constructor!='undefined' &amp;&amp;
                typeof $.fn.tooltip.Constructor.VERSION!='undefined';
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
  viewerLoader.addRequiredScripts(configuration.resources.script, false);
  viewerLoader.addRequiredScripts(configuration.resources.module, true);

  viewerLoader.addConstructorExecution(function(){
    import("<xsl:value-of select="$WebApplicationBaseURL" />modules/iview2/js/iview-client-base.es.js").then((imp) => {
      let containerElements = document.querySelectorAll("[data-viewer='"+configuration.properties.derivate+":"+CSS.escape(configuration.properties.filePath)+"']");
      containerElements.forEach((containerElement)=> {
        new imp.MyCoReViewer(containerElement, configuration.properties);
      })
    });
  });

})
();
  </xsl:template>

</xsl:stylesheet>
