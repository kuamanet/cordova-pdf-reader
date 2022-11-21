var exec = require('cordova/exec');

var renderSuccessCallback, renderErrorCallback;

var api = {
  then: function(callback) {
    renderSuccessCallback = callback;
    return api;
  },
  catch: function(callback) {
    renderErrorCallback = callback;
    return api;
  }
};

/**
 *
 * @param base64PDF
 * @param options: {watermark:String, doShowThumbnails: Boolean, fileName: String}
 * @return {{then: function(*), catch: function(*)}}
 */
exports.fromBase64 = function (base64PDF, options) {
  exec(
    function(args) {
      if (renderSuccessCallback) {
        renderSuccessCallback(args);
      }
    }, function(errors) {
      if (renderErrorCallback) {
        renderErrorCallback(errors);
      }
    },
    'PdfReader',
    'fromBase64',
    [
      base64PDF,
      options.watermark ? options.watermark : '',
      options.doShowThumbnails !== null && options.doShowThumbnails !== undefined ? options.doShowThumbnails : true,
      options.fileName ? options.fileName : ''
    ]);

  return api;
};
