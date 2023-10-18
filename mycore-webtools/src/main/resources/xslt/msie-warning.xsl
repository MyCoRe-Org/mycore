<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n">

  <xsl:param name="msie-banner" select="'enabled'"/>

  <xsl:template name="msie-note">
    <xsl:if test="(contains($User-Agent, 'Trident/') or contains($User-Agent,'MSIE ')) and $msie-banner = 'enabled'">
      <div class="alert alert-light p-2 unsupported-browser" role="alert">
        <div class="container clearfix d-flex justify-content-center py-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="flex-self-start flex-shrink-0 pt-1 mr-2" aria-hidden="true"
               viewBox="0 0 45 44" width="45" height="44">
            <path
                d="M 41.9943 13.1676 C 44.0511 8.23147 44.1882 4.11806 41.7201 1.65001 C 38.9778 -1.09044 32.3964 -0.269582 25.4054 3.02115 H 24.5827 C 19.6466 3.02115 14.8495 4.80545 11.2845 7.95724 C 8.26804 10.6995 6.07422 14.2645 5.11442 18.2408 C 5.79999 17.4181 9.63917 12.8933 14.0268 10.4253 C 14.1639 10.4253 15.2608 9.73972 15.2608 9.73972 C 15.1237 9.73972 13.067 11.7964 12.6557 12.2078 C 3.05772 22.08 -2.56212 37.0235 1.82552 41.4093 C 4.70308 44.2887 9.9134 43.6031 15.9464 40.3124 C 18.5516 41.5464 21.431 42.0949 24.5827 42.0949 C 28.6961 42.0949 32.5353 40.998 35.8261 38.8042 C 39.2521 36.6103 41.7201 33.1843 43.2302 29.208 H 32.5353 C 31.1642 31.8132 28.0106 33.5957 24.7199 33.5957 C 20.058 33.5957 16.2188 29.7565 16.0835 25.3689 V 24.9575 H 44.0511 V 24.5462 C 44.0511 23.8606 44.1882 23.0379 44.1882 22.4913 C 44.1882 19.2006 43.3655 16.0469 41.9943 13.1676 V 13.1676 Z M 4.84019 40.5885 C 2.64638 38.3946 3.33377 34.146 5.9371 29.0727 C 7.17113 32.5006 9.22783 35.5171 11.833 37.7109 C 12.6557 38.3965 13.6155 39.082 14.5753 39.6305 C 10.1876 41.9596 6.62267 42.3728 4.84019 40.5885 V 40.5885 Z M 32.9467 19.7508 H 16.2206 V 19.6137 C 16.4949 15.3632 20.4693 11.6611 25.1312 11.6611 C 29.5188 11.6611 33.0838 15.089 33.358 19.6137 V 19.7508 H 32.9467 Z M 41.3088 12.0725 C 40.4861 10.7013 39.3892 9.33021 38.1552 8.2333 C 36.2433 6.45558 34.0094 5.05943 31.5737 4.11988 C 35.9613 2.06501 39.6634 1.79078 41.583 3.71037 C 43.0913 5.49285 42.9541 8.50752 41.3088 12.0725 C 41.3088 12.2096 41.3088 12.2096 41.3088 12.0725 C 41.3088 12.2096 41.3088 12.2096 41.3088 12.0725 V 12.0725 Z"></path>
          </svg>
          <div class="d-flex flex-auto flex-row">
            <div class="flex-auto min-width-0 mr-2">
              <h5>
                <xsl:value-of select="mcri18n:translate('component.webtools.msie.headline')"/>
              </h5>
              <p class="m-0">
                <xsl:value-of select="concat(mcri18n:translate('component.webtools.msie.recommendation'),' ')"/>
                <a href="https://www.microsoftedge.com" class="alert-link text-nowrap">
                  <i class="fab fa-edge"></i>
                  <xsl:value-of select="' Microsoft Edge'" />
                </a>
                <xsl:value-of select="concat(mcri18n:translate('component.webtools.msie.enum'),' ')"/>
                <a href="https://mozilla.org/firefox/" class="alert-link text-nowrap">
                  <i class="fab fa-firefox"></i>
                  <xsl:value-of select="' Firefox'" />
                </a>
                <xsl:value-of select="concat(mcri18n:translate('component.webtools.msie.enumLast'),' ')"/>
                <a href="https://chrome.google.com" class="alert-link text-nowrap">
                  <i class="fab fa-chrome"></i>
                  <xsl:value-of select="' Google Chrome'" />
                </a>
                <xsl:text>.</xsl:text>
              </p>
            </div>
            <div class="d-flex justify-content-center align-items-center flex-shrink-0 ">
              <form accept-charset="UTF-8">
                <input name="XSL.msie-banner.SESSION" type="hidden" value="disabled"/>
                <button class="btn btn-warning" type="submit">
                  <xsl:value-of select="mcri18n:translate('component.webtools.msie.ignore')"/>
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
