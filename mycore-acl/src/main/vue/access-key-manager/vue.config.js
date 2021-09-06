module.exports = {
  chainWebpack: (config) => {
    config.performance
      .maxEntrypointSize(3000000)
      .maxAssetSize(3000000);
  },
};
