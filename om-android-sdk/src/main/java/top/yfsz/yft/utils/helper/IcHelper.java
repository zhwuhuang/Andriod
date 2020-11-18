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

/**
 * The type Ic helper.
 */
public class IcHelper {
    /**
     * reporting given extId; called upon mediation Video ads Finish
     *
     * @param placementId the placement id
     * @param mediationId the mediation id
     * @param insId       the ins id
     * @param scene       the scene
     * @param extId       the ext id
     */
    public static void icReport(String placementId, int mediationId, int insId, int scene, String extId) {
        try {

            Configurations config = DataCache.getInstance().getFromMem(KeyConstants.KEY_CONFIGURATION, Configurations.class);
            if (config == null || config.getApi() == null || TextUtils.isEmpty(config.getApi().getIc())) {
                return;
            }
            String vpcUrl = RequestBuilder.buildIcUrl(config.getApi().getIc());

            if (TextUtils.isEmpty(vpcUrl)) {
                return;
            }

            Headers headers = HeaderUtils.getBaseHeaders();
            AdRequest.post()
                    .url(vpcUrl)
                    .headers(headers)
                    .body(new ByteRequestBody(RequestBuilder.buildIcRequestBody(Integer.valueOf(placementId),
                            mediationId, insId, scene, extId)))
                    .connectTimeout(30000)
                    .readTimeout(60000)
                    .instanceFollowRedirects(true)
                    .performRequest(AdtUtil.getApplication());
        } catch (Exception e) {
            DeveloperLog.LogE("icReport error ", e);
            CrashUtil.getSingleton().saveException(e);
        }
    }
}
