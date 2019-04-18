package net.kuama.pdf;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class PdfReader extends CordovaPlugin {

    public static String BASE_64_DATA;
    public static int PDF_RESULT;
    private CallbackContext mCallbackContext;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        Context context = cordova.getActivity().getApplicationContext();
        mCallbackContext = callbackContext;
        if (action.equals("fromBase64")) {
            Intent intent = new Intent(context, PdfActivity.class);
            try {
                BASE_64_DATA = args.get(0).toString();
                if (args.length() > 0) {
                    intent.putExtra(PdfActivity.Extras.watermark_extra, args.get(1).toString());
                }
                if (args.length() > 2) {
                    intent.putExtra(PdfActivity.Extras.activity_title, args.get(3).toString());
                }
                cordova.setActivityResultCallback (this);
                cordova.getActivity().startActivityForResult(intent, PDF_RESULT);


                return true;
            } catch (Exception e) {
                Log.e("PDFREADER", e.getMessage());
            }

        }
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == PDF_RESULT) {
            cordova.getThreadPool().execute(() -> mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK)));
        }
    }
}
