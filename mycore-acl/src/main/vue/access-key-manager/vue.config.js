module.exports = {
  chainWebpack: config => {
    config.performance
      .maxEntrypointSize(2000000)
      .maxAssetSize(2000000)
  }
}
