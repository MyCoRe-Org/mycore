<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" encoding="UTF-8" indent="yes"/>

  <xsl:param name="WebApplicationBaseURL"/>
  <xsl:param name="MCR.Neo4J.colors"/>

  <xsl:template match="/mycoreobject">
    <link rel="stylesheet" href="/css/graph.css" type="text/css" media="all" />

    <div class="graph-container">
      <div class="graph-panel">
        <button class="graph-reset-btn" onclick="resetGraph()">Graph zurücksetzen</button>
        <div class="form-group">
          <label>Kindknotenlimit pro Knoten</label>
          <div class="slider-value-group">
            <input id="limit-slider" type="range" min="10" max="1000" step="10" value="100" oninput="updateSliderValue('limit', this.value)"/>
            <span id="limit-slider-label"></span>
          </div>
        </div>
        <div class="form-group">
          <label>Gravitationsstärke</label>
          <div class="slider-value-group">
            <input id="gravity-slider" type="range" min="1" max="30" step="1" value="2" oninput="updateSliderValue('gravity', this.value)"/>
            <span id="gravity-slider-label"></span>
          </div>
        </div>
        <div id="graph-loading"></div>
      </div>
      <div id="graph"></div>
      <div id="graph-metadata">
        <dl class="graph-metadata-list"></dl>
      </div>
    </div>

    <script src="https://cdnjs.cloudflare.com/ajax/libs/vis-network/9.1.6/standalone/umd/vis-network.min.js"></script>
    <script type="text/javascript" src="/js/graph.js" id="{@ID}" baseUrl="{$WebApplicationBaseURL}" colors="{$MCR.Neo4J.colors}"></script>
  </xsl:template>

</xsl:stylesheet>
