package com.sumsub.msdk.plugins.cordova;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ValueCallback;

import androidx.annotation.Nullable;

import com.sumsub.sns.R;
import com.sumsub.sns.core.SNSMobileSDK;
import com.sumsub.sns.core.data.listener.CordovaTokenExpirationHandler;
import com.sumsub.sns.core.data.model.SNSCompletionResult;
import com.sumsub.sns.core.data.model.SNSException;
import com.sumsub.sns.core.data.model.SNSSDKState;
import com.sumsub.sns.core.data.model.SNSSupportItem;
import com.sumsub.sns.liveness3d.SNSLiveness3d;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Locale;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import timber.log.Timber;


public class SNSMobileSdkCordovaPlugin extends CordovaPlugin {
    private static final String LAUNCH_ACTION = "launchSNSMobileSDK";
    private static final String NEW_TOKEN_ACTION = "setNewAccessToken";
    private static final String DISMISS_ACTION = "dismiss";
    private CallbackContext callbackContextApp;

    private static volatile String newAccessToken = null;
    private static final Object lock = new Object();


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        callbackContextApp = callbackContext;
        if (action.equals(LAUNCH_ACTION)) {
            if (args.isNull(0)) {
                callbackContextApp.error("Error: SDK Config object must be provided");
                return false;
            }

            JSONObject conf = args.getJSONObject(0);
            String apiUrl = conf.optString("apiUrl");
            String flowName = conf.optString("flowName");
            String accessToken = conf.optString("accessToken");
            String supportEmail = conf.optString("supportEmail");
            String locale = conf.optString("locale");
            boolean isDebug = conf.optBoolean("debug", false);

            if (TextUtils.isEmpty(supportEmail)) {
                supportEmail = "support@sumsub.com";
            }

            if (TextUtils.isEmpty(apiUrl) || TextUtils.isEmpty(flowName) || TextUtils.isEmpty(accessToken)) {
                callbackContextApp.error("Error: Access token, API URL and Flow Name must be provided");
                return false;
            }
            if (TextUtils.isEmpty(locale)) {
                locale = Locale.getDefault().getLanguage();
            }
            this.launchSNSMobileSDK(apiUrl, flowName, accessToken, supportEmail, locale, isDebug);
            return true;
        } else if (action.equals(NEW_TOKEN_ACTION)) {
            newAccessToken = args.getString(0);
            return true;
	} else if (action.equals(DISMISS_ACTION)) {
            cordova.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final SNSMobileSDK.SDK snsSdk = new SNSMobileSDK.Builder(cordova.getActivity(), "", "").build();
                    snsSdk.dismiss();
                }
            });
	        return true;
    } else {
            callbackContextApp.error("Method not implemented");
            return false;
      }
    }

    private void requestNewAccessToken() {
        webView.getEngine().evaluateJavascript("window.SNSMobileSDK.getNewAccessToken()", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                // no op
            }
        });
    }

    private void launchSNSMobileSDK(final String apiUrl, final String flowName, final String accessToken, String supportEmail, final String locale, final boolean isDebug) {
        final SNSSupportItem supportItem = new SNSSupportItem(
                R.string.sns_support_EMAIL_title,
                R.string.sns_support_EMAIL_description,
                R.drawable.sns_ic_email,
                SNSSupportItem.Type.Email,
                supportEmail, null);


        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final SNSMobileSDK.SDK snsSdk = new SNSMobileSDK.Builder(cordova.getActivity(), apiUrl, flowName)
                        .withAccessToken(accessToken, new CordovaTokenExpirationHandler() {
                            @Override
                            public String onTokenExpiredMain() {
                                Timber.d("SumSub: calling onTokenExpiredMain!");
                                newAccessToken = null;
                                requestNewAccessToken();
                                int cnt = 0;
                                while (newAccessToken == null) {
                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        //no op
                                    }
                                    if (++cnt > 100) {
                                        return null;
                                    }
                                }
                                Timber.d("SumSub: Received new token: " + newAccessToken + ' ' + Thread.currentThread().getName());
                                return newAccessToken;
                            }
                        })
                        .withDebug(isDebug)
                        .withModules(Collections.singletonList(new SNSLiveness3d()))
                        .withHandlers(new Function1<SNSException, Unit>() {
                                          @Override
                                          public Unit invoke(SNSException e) {
                                              Timber.d(Log.getStackTraceString(e));
                                              return Unit.INSTANCE;
                                          }
                                      }, new Function2<SNSSDKState, SNSSDKState, Unit>() {
                                          @Override
                                          public Unit invoke(SNSSDKState oldState, SNSSDKState newState) {
                                              return Unit.INSTANCE;
                                          }
                                      }, new Function2<SNSCompletionResult, SNSSDKState, Unit>() {
                                          @Override
                                          public Unit invoke(SNSCompletionResult snsCompletionResult, SNSSDKState snssdkState) {
                                              getResultToTheClient(snsCompletionResult, snssdkState);
                                              return Unit.INSTANCE;
                                          }
                                      }
                        )
                        .withSupportItems(Collections.singletonList(supportItem))
                        .withLocale(new Locale(locale))
                        .build();
                snsSdk.launch();

            }
        });

        cordova.setActivityResultCallback(this);
    }

    private void getResultToTheClient(SNSCompletionResult snsCompletionResult, SNSSDKState snssdkState) {
        if (SNSCompletionResult.SuccessTermination.INSTANCE.equals(snsCompletionResult)) {
            callbackContextApp.success(getResult(true, snssdkState.getClass().getSimpleName(), null, null));
        } else if (snsCompletionResult instanceof SNSCompletionResult.AbnormalTermination) {
            SNSCompletionResult.AbnormalTermination abnormalTermination = (SNSCompletionResult.AbnormalTermination) snsCompletionResult;
            String message = abnormalTermination.getException() != null ? abnormalTermination.getException().getMessage() : null;
            if (snssdkState instanceof SNSSDKState.Failed) {
                callbackContextApp.success(getResult(false, "Failed", message, snssdkState.getClass().getSimpleName()));
            } else {
                callbackContextApp.success(getResult(false, "Failed", message, "Unknown"));

            }
        } else {
            callbackContextApp.error("Unknown completion result: " + snsCompletionResult.getClass().getName());
        }
    }

    private JSONObject getResult(boolean success, String state, String errorMsg, String errorType)  {
        JSONObject result = new JSONObject();
        try {
            result.put("success", success);
            result.put("state", state);
            result.put("errorMsg", errorMsg);
            result.put("errorType", errorType);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}