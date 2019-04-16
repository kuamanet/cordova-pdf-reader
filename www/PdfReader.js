var exec = require('cordova/exec');

let renderSuccessCallback, renderErrorCallback;

const api = {
  then: callback => {
    renderSuccessCallback = callback;
    return api;
  },
  catch: callback => {
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
    args => {
      if (renderSuccessCallback) {
        renderSuccessCallback(args);
      }
    }, errors => {
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
