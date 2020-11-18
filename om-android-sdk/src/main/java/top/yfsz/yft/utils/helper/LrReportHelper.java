// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils.helper;

import android.text.TextUtils;

import top.yfsz.yft.utils.AdtUtil;
import top.yfsz.yft.utils.DeveloperLog;
import top.yfsz.yft.utils.cache.DataCache;
import top.yfsz.yft.utils.constant.KeyConstants;
import top.yfsz.yft.utils.crash.CrashUtil;
import top.yfsz.yft.utils.model.BaseInstance;
import top.yfsz.yft.utils.model.Configurations;
import top.yfsz.yft.utils.request.HeaderUtils;
import top.yfsz.yft.utils.request.RequestBuilder;
import top.yfsz.yft.utils.request.network.AdRequest;
import top.yfsz.yft.utils.request.network.ByteRequestBody;
import top.yfsz.yft.utils.request.network.Headers;

/**
 *
 */
public final class LrReportHelper {


    public static void report(String placementId, int loadType, int abt, int reportType, int bid) {
        report(placementId, -1, loadType, -1, -1, abt, reportType, bid);
    }

    public static void report(BaseInstance instance, int loadType, int abt, int reportType, int bid) {
        report(instance, -1, loadType, abt, reportType, bid);
    }

    public static void report(BaseInstance instance, int sceneId, int loadType, int abt, int reportType, int bid) {
        if (instance == null) {
            return;
        }
        report(instance.getPlacementId(), sceneId, loadType, instance.getId(), instance.getMediationId(), abt, reportType, bid);
    }

    private static void report(String placementId, int sceneId, int loadType,
                               int instanceId, int mediationId, int abt, int reportType, int bid) {
        try {
            Configurations config = DataCache.getInstance().getFromMem(KeyConstants.KEY_CONFIGURATION, Configurations.class);
            if (config == null || config.getApi() == null || TextUtils.isEmpty(config.getApi().getLr())) {
                return;
            }
            String lrUrl = RequestBuilder.buildLrUrl(config.getApi().getLr());

            if (TextUtils.isEmpty(lrUrl)) {
                return;
            }

            Headers headers = HeaderUtils.getBaseHeaders();
            AdRequest.post()
                    .url(lrUrl)
                    .headers(headers)
                    .body(new ByteRequestBody(RequestBuilder.buildLrRequestBody(Integer.parseInt(placementId),
                            sceneId,
                            loadType,
                            mediationId,
                            instanceId,
                            abt,
                            reportType,
                            bid)
                    ))
                    .connectTimeout(30000)
                    .readTimeout(60000)
                    .instanceFollowRedirects(true)
                    .performRequest(AdtUtil.getApplication());
        } catch (Exception e) {
            DeveloperLog.LogE("httpLr error ", e);
            CrashUtil.getSingleton().saveException(e);
        }
    }
}
