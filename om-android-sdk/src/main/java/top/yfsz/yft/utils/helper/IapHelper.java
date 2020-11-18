// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils.helper;

import android.text.TextUtils;

import top.yfsz.yft.utils.AdtUtil;
import top.yfsz.yft.utils.DeveloperLog;
import top.yfsz.yft.utils.cache.DataCache;
import top.yfsz.yft.utils.constant.KeyConstants;
import top.yfsz.yft.utils.crash.CrashUtil;
import top.yfsz.yft.utils.model.Configurations;
import top.yfsz.yft.utils.request.HeaderUtils;
import top.yfsz.yft.utils.request.RequestBuilder;
import top.yfsz.yft.utils.request.network.AdRequest;
import top.yfsz.yft.utils.request.network.ByteRequestBody;
import top.yfsz.yft.utils.request.network.Headers;
import top.yfsz.yft.utils.request.network.Request;
import top.yfsz.yft.utils.request.network.Response;

import org.json.JSONObject;

/**
 *
 */
public class IapHelper {

    private static final float FLOAT_ACCURACY = 0.0001f;
    private static final String IAP_NUMBER = "c_iap_number";

    public static void setIap(final float iapNumber, final String currency) {

        if (FLOAT_ACCURACY > iapNumber) {
            DeveloperLog.LogE("iapNumber  is zero");
            return;
        }

        if (TextUtils.isEmpty(currency)) {
            DeveloperLog.LogE("currency is null");
            return;
        }

        iapReport(String.valueOf(iapNumber), currency, String.valueOf(getIap()), new Request.OnRequestCallback() {
            @Override
            public void onRequestSuccess(Response response) {
                try {
                    if (response == null || response.code() != 200) {
                        DeveloperLog.LogE("iap result error");
                        return;
                    }
                    String resStr = response.body().string();
                    DeveloperLog.LogE(String.format("iap result : %s", resStr));
                    if (!TextUtils.isEmpty(resStr)) {
                        JSONObject resObj = new JSONObject(resStr);
                        saveIap(resObj.optDouble("iapUsd", 0L));
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    DeveloperLog.LogE("save iap data error", e);
                }
            }

            @Override
            public void onRequestFailed(String error) {
                DeveloperLog.LogE("http iap error=" + error);
            }
        });
    }

    private static void saveIap(double iapNumber) {
        try {
            if (FLOAT_ACCURACY > iapNumber) {
                DeveloperLog.LogE("iapNumber  is zero");
                return;
            }

            DataCache.getInstance().set(IAP_NUMBER, String.valueOf(iapNumber));
        } catch (Exception e) {
            DeveloperLog.LogE("saveIap error :", e);
        }
    }


    static String getIap() {
        try {
            String iapStr = DataCache.getInstance().get(IAP_NUMBER, String.class);
            if (TextUtils.isEmpty(iapStr)) {
                return String.valueOf(0.00);
            }
            return iapStr;
        } catch (Exception e) {
            DeveloperLog.LogE("getIap error :", e);
            return String.valueOf(0.00);
        }
    }

    private static void iapReport(String iapCount, String currency, String iapt, Request.OnRequestCallback callback) {
        try {

            Configurations config = DataCache.getInstance().getFromMem(KeyConstants.KEY_CONFIGURATION, Configurations.class);
            if (config == null || config.getApi() == null || TextUtils.isEmpty(config.getApi().getIap())) {
                callback.onRequestFailed("empty Url");
                return;
            }

            String url = RequestBuilder.buildIapUrl(config.getApi().getIap());

            DeveloperLog.LogD(String.format("iap url : %s", url));

            Headers headers = HeaderUtils.getBaseHeaders();

            byte[] bytes = RequestBuilder.buildIapRequestBody(
                    currency,
                    iapCount,
                    iapt);

            if (bytes == null) {
                if (callback != null) {
                    callback.onRequestFailed("Iap param is null");
                }
                return;
            }

            ByteRequestBody requestBody = new ByteRequestBody(bytes);
            AdRequest.post()
                    .url(url)
                    .body(requestBody)
                    .headers(headers)
                    .connectTimeout(30000)
                    .readTimeout(60000)
                    .callback(callback)
                    .performRequest(AdtUtil.getApplication());

        } catch (Exception e) {
            DeveloperLog.LogE("HttpIAP error ", e);
            CrashUtil.getSingleton().saveException(e);

            if (callback != null) {
                callback.onRequestFailed("httpIAP error");
            }
        }
    }
}
