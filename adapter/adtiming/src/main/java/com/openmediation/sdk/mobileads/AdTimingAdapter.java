// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.openmediation.sdk.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.adtiming.mediationsdk.AdTimingAds;
import com.adtiming.mediationsdk.InitCallback;
import com.adtiming.mediationsdk.adt.interstitial.AdTimingInterstitialAd;
import com.adtiming.mediationsdk.adt.interstitial.InterstitialAdListener;
import com.adtiming.mediationsdk.adt.video.AdTimingRewardedVideo;
import com.adtiming.mediationsdk.adt.video.RewardedVideoListener;
import com.adtiming.mediationsdk.utils.error.AdTimingError;
import top.yfsz.yft.mediation.AdapterErrorBuilder;
import top.yfsz.yft.mediation.CustomAdsAdapter;
import top.yfsz.yft.mediation.InterstitialAdCallback;
import top.yfsz.yft.mediation.MediationInfo;
import top.yfsz.yft.mediation.RewardedVideoCallback;
import com.openmediation.sdk.mobileads.adtiming.BuildConfig;
import top.yfsz.yft.utils.AdLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AdTimingAdapter extends CustomAdsAdapter implements RewardedVideoListener, InterstitialAdListener {
    private static final String TAG = "OM-AdTiming: ";
    private static final String PAY_LOAD = "pay_load";
    private ConcurrentMap<String, RewardedVideoCallback> mVideoListeners;
    private ConcurrentMap<String, InterstitialAdCallback> mInterstitialListeners;
    private volatile InitState mInitState = InitState.NOT_INIT;

    public AdTimingAdapter() {
        mVideoListeners = new ConcurrentHashMap<>();
        mInterstitialListeners = new ConcurrentHashMap<>();
    }

    @Override
    public String getMediationVersion() {
        return AdTimingAds.getSDKVersion();
    }

    @Override
    public String getAdapterVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public int getAdNetworkId() {
        return MediationInfo.MEDIATION_ID_1;
    }

    @Override
    public void setGDPRConsent(Context context, boolean consent) {
        super.setGDPRConsent(context, consent);
        AdTimingAds.setGDPRConsent(consent);
    }

    @Override
    public void setUSPrivacyLimit(Context context, boolean value) {
        super.setUSPrivacyLimit(context, value);
        AdTimingAds.setAgeRestricted(value);
    }

    @Override
    public void setAgeRestricted(Context context, boolean restricted) {
        super.setAgeRestricted(context, restricted);
        AdTimingAds.setAgeRestricted(restricted);
    }

    @Override
    public void setUserAge(Context context, int age) {
        super.setUserAge(context, age);
        AdTimingAds.setUserAge(age);
    }

    @Override
    public void setUserGender(Context context, String gender) {
        super.setUserGender(context, gender);
        AdTimingAds.setUserGender(gender);
    }

    @Override
    public void onResume(Activity activity) {
        super.onResume(activity);
        AdTimingAds.onResume(activity);
    }

    @Override
    public void onPause(Activity activity) {
        AdTimingAds.onPause(activity);
        super.onPause(activity);
    }

    @Override
    public void initRewardedVideo(Activity activity, Map<String, Object> dataMap, RewardedVideoCallback callback) {
        super.initRewardedVideo(activity, dataMap, callback);
        String error = check(activity);
        if (!TextUtils.isEmpty(error)) {
            callback.onRewardedVideoInitFailed(AdapterErrorBuilder.buildInitError(
                    AdapterErrorBuilder.AD_UNIT_REWARDED_VIDEO, mAdapterName, error));
            return;
        }
        String pid = (String) dataMap.get("pid");
        switch (mInitState) {
            case NOT_INIT:
                mVideoListeners.put(pid, callback);
                String appKey = (String) dataMap.get("AppKey");
                initSDK(activity, appKey);
                break;
            case INIT_PENDING:
                mVideoListeners.put(pid, callback);
                break;
            case INIT_SUCCESS:
                callback.onRewardedVideoInitSuccess();
                break;
            case INIT_FAIL:
                callback.onRewardedVideoInitFailed(AdapterErrorBuilder.buildInitError(
                        AdapterErrorBuilder.AD_UNIT_REWARDED_VIDEO, mAdapterName, "AdTiming initRewardedVideo failed"));
                break;
            default:
                break;
        }
    }

    @Override
    public void loadRewardedVideo(Activity activity, String adUnitId, RewardedVideoCallback callback) {
        super.loadRewardedVideo(activity, adUnitId, callback);
        loadRewardedVideoAd(activity, adUnitId, null, callback);
    }

    @Override
    public void loadRewardedVideo(Activity activity, String adUnitId, Map<String, Object> extras, RewardedVideoCallback callback) {
        super.loadRewardedVideo(activity, adUnitId, extras, callback);
        loadRewardedVideoAd(activity, adUnitId, extras, callback);
    }

    private void loadRewardedVideoAd(Activity activity, String adUnitId, Map<String, Object> extras, RewardedVideoCallback callback) {
        String error = check(activity, adUnitId);
        if (!TextUtils.isEmpty(error)) {
            callback.onRewardedVideoLoadFailed(AdapterErrorBuilder.buildLoadCheckError(
                    AdapterErrorBuilder.AD_UNIT_REWARDED_VIDEO, mAdapterName, error));
            return;
        }
        mVideoListeners.put(adUnitId, callback);
        AdTimingRewardedVideo.setAdListener(adUnitId, this);
        String payload = "";
        if (extras != null && extras.containsKey(PAY_LOAD)) {
            payload = String.valueOf(extras.get(PAY_LOAD));
        }
        AdTimingRewardedVideo.loadAdWithPayload(adUnitId, payload);
    }

    @Override
    public void showRewardedVideo(Activity activity, String adUnitId, RewardedVideoCallback callback) {
        super.showRewardedVideo(activity, adUnitId, callback);
        String error = check(activity, adUnitId);
        if (!TextUtils.isEmpty(error)) {
            callback.onRewardedVideoAdShowFailed(AdapterErrorBuilder.buildShowError(
                    AdapterErrorBuilder.AD_UNIT_REWARDED_VIDEO, mAdapterName, error));
            return;
        }
        if (isRewardedVideoAvailable(adUnitId)) {
            AdTimingRewardedVideo.setAdListener(adUnitId, this);
            AdTimingRewardedVideo.showAd(adUnitId);
        } else {
            callback.onRewardedVideoAdShowFailed(AdapterErrorBuilder.buildShowError(
                    AdapterErrorBuilder.AD_UNIT_REWARDED_VIDEO, mAdapterName, "no reward ad or not ready"));
        }
    }

    @Override
    public boolean isRewardedVideoAvailable(String adUnitId) {
        return AdTimingRewardedVideo.isReady(adUnitId);
    }

    @Override
    public void initInterstitialAd(Activity activity, Map<String, Object> dataMap, InterstitialAdCallback callback) {
        super.initInterstitialAd(activity, dataMap, callback);
        String error = check(activity);
        if (!TextUtils.isEmpty(error)) {
            callback.onInterstitialAdInitFailed(AdapterErrorBuilder.buildInitError(
                    AdapterErrorBuilder.AD_UNIT_INTERSTITIAL, mAdapterName, error));
            return;
        }
        String pid = (String) dataMap.get("pid");
        switch (mInitState) {
            case NOT_INIT:
                mInterstitialListeners.put(pid, callback);
                String appKey = (String) dataMap.get("AppKey");
                initSDK(activity, appKey);
                break;
            case INIT_PENDING:
                mInterstitialListeners.put(pid, callback);
                break;
            case INIT_SUCCESS:
                callback.onInterstitialAdInitSuccess();
                break;
            case INIT_FAIL:
                callback.onInterstitialAdInitFailed(AdapterErrorBuilder.buildInitError(
                        AdapterErrorBuilder.AD_UNIT_INTERSTITIAL, mAdapterName, "AdTiming initInterstitialAd failed"));
                break;
            default:
                break;
        }
    }

    @Override
    public void loadInterstitialAd(Activity activity, String adUnitId, InterstitialAdCallback callback) {
        super.loadInterstitialAd(activity, adUnitId, callback);
        loadIsAd(activity, adUnitId, null, callback);
    }

    @Override
    public void loadInterstitialAd(Activity activity, String adUnitId, Map<String, Object> extras, InterstitialAdCallback callback) {
        super.loadInterstitialAd(activity, adUnitId, extras, callback);
        loadIsAd(activity, adUnitId, extras, callback);
    }

    private void loadIsAd(Activity activity, String adUnitId, Map<String, Object> extras, InterstitialAdCallback callback) {
        String error = check(activity, adUnitId);
        if (!TextUtils.isEmpty(error)) {
            callback.onInterstitialAdLoadFailed(AdapterErrorBuilder.buildLoadCheckError(
                    AdapterErrorBuilder.AD_UNIT_INTERSTITIAL, mAdapterName, error));
            return;
        }
        String payload = "";
        if (extras != null && extras.containsKey(PAY_LOAD)) {
            payload = String.valueOf(extras.get(PAY_LOAD));
        }
        mInterstitialListeners.put(adUnitId, callback);
        AdTimingInterstitialAd.setAdListener(adUnitId, this);
        AdTimingInterstitialAd.loadAdWithPayload(adUnitId, payload);
    }

    @Override
    public void showInterstitialAd(Activity activity, String adUnitId, InterstitialAdCallback callback) {
        super.showInterstitialAd(activity, adUnitId, callback);
        String error = check(activity, adUnitId);
        if (!TextUtils.isEmpty(error)) {
            callback.onInterstitialAdShowFailed(AdapterErrorBuilder.buildShowError(
                    AdapterErrorBuilder.AD_UNIT_INTERSTITIAL, mAdapterName, error));
            return;
        }
        if (isInterstitialAdAvailable(adUnitId)) {
            AdTimingInterstitialAd.setAdListener(adUnitId, this);
            AdTimingInterstitialAd.showAd(adUnitId);
        } else {
            callback.onInterstitialAdShowFailed(AdapterErrorBuilder.buildShowError(
                    AdapterErrorBuilder.AD_UNIT_INTERSTITIAL, mAdapterName, "no interstitial ad or not ready"));
        }
    }

    @Override
    public boolean isInterstitialAdAvailable(String adUnitId) {
        return AdTimingInterstitialAd.isReady(adUnitId);
    }

    private void initSDK(Activity activity, String appKey) {
        mInitState = InitState.INIT_PENDING;
        AdTimingAds.init(activity, appKey, new InitCallback() {
            @Override
            public void onSuccess() {
                mInitState = InitState.INIT_SUCCESS;
                if (!mVideoListeners.isEmpty()) {
                    for (Map.Entry<String, RewardedVideoCallback> rewardedVideoCallbackEntry : mVideoListeners.entrySet()) {
                        if (rewardedVideoCallbackEntry != null) {
                            rewardedVideoCallbackEntry.getValue().onRewardedVideoInitSuccess();
                        }
                    }
                }

                if (!mInterstitialListeners.isEmpty()) {
                    for (Map.Entry<String, InterstitialAdCallback> interstitialAdCallbackEntry : mInterstitialListeners.entrySet()) {
                        if (interstitialAdCallbackEntry != null) {
                            interstitialAdCallbackEntry.getValue().onInterstitialAdInitSuccess();
                        }
                    }
                }
            }

            @Override
            public void onError(AdTimingError adTimingError) {
                mInitState = InitState.INIT_FAIL;
                if (!mVideoListeners.isEmpty()) {
                    for (Map.Entry<String, RewardedVideoCallback> rewardedVideoCallbackEntry : mVideoListeners.entrySet()) {
                        if (rewardedVideoCallbackEntry != null) {
                            rewardedVideoCallbackEntry.getValue().onRewardedVideoInitFailed(AdapterErrorBuilder.buildInitError(
                                            AdapterErrorBuilder.AD_UNIT_REWARDED_VIDEO, mAdapterName, adTimingError.toString()));
                        }
                    }
                }

                if (!mInterstitialListeners.isEmpty()) {
                    for (Map.Entry<String, InterstitialAdCallback> interstitialAdCallbackEntry : mInterstitialListeners.entrySet()) {
                        if (interstitialAdCallbackEntry != null) {
                            interstitialAdCallbackEntry.getValue().onInterstitialAdInitFailed(AdapterErrorBuilder.buildInitError(
                                            AdapterErrorBuilder.AD_UNIT_INTERSTITIAL, mAdapterName, adTimingError.toString()));
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onInterstitialAdLoadSuccess(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onInterstitialAdReady : " + placementId);
            InterstitialAdCallback callback = mInterstitialListeners.get(placementId);
            if (callback != null) {
                callback.onInterstitialAdLoadSuccess();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterstitialAdClosed(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onInterstitialAdClose : " + placementId);
            InterstitialAdCallback callback = mInterstitialListeners.get(placementId);
            if (callback != null) {
                callback.onInterstitialAdClosed();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterstitialAdShowed(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onInterstitialAdShowed : " + placementId);
            InterstitialAdCallback callback = mInterstitialListeners.get(placementId);
            if (callback != null) {
                callback.onInterstitialAdShowSuccess();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterstitialAdLoadFailed(String placementId, com.adtiming.mediationsdk.adt.utils.error.AdTimingError error) {
        try {
            AdLog.getSingleton().LogE(TAG + "InterstitialAd Load Failed: " + error);
            InterstitialAdCallback callback = mInterstitialListeners.get(placementId);
            if (callback != null) {
                callback.onInterstitialAdLoadFailed(AdapterErrorBuilder.buildLoadError(
                                AdapterErrorBuilder.AD_UNIT_INTERSTITIAL, mAdapterName, error.getCode(), error.toString()));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterstitialAdClicked(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onInterstitialAdClicked : " + placementId);
            InterstitialAdCallback callback = mInterstitialListeners.get(placementId);
            if (callback != null) {
                callback.onInterstitialAdClick();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onInterstitialAdEvent(String placementId, String event) {
    }

    @Override
    public void onInterstitialAdShowFailed(String placementId, com.adtiming.mediationsdk.adt.utils.error.AdTimingError error) {
        try {
            InterstitialAdCallback callback = mInterstitialListeners.get(placementId);
            if (callback != null) {
                callback.onInterstitialAdShowFailed(AdapterErrorBuilder.buildShowError(
                        AdapterErrorBuilder.AD_UNIT_INTERSTITIAL, mAdapterName, error.getCode(), error.toString()));
            }
        } catch (Exception ignored) {
        }
    }


    @Override
    public void onRewardedVideoAdLoadSuccess(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onVideoAdReady : " + placementId);
            RewardedVideoCallback callback = mVideoListeners.get(placementId);
            if (callback != null) {
                callback.onRewardedVideoLoadSuccess();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRewardedVideoAdClosed(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onVideoAdClose : " + placementId);
            RewardedVideoCallback callback = mVideoListeners.get(placementId);
            if (callback != null) {
                callback.onRewardedVideoAdClosed();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRewardedVideoAdShowed(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onVideoAdShowed : " + placementId);
            RewardedVideoCallback callback = mVideoListeners.get(placementId);
            if (callback != null) {
                callback.onRewardedVideoAdShowSuccess();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRewardedVideoAdRewarded(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onVideoAdRewarded : " + placementId);
            RewardedVideoCallback callback = mVideoListeners.get(placementId);
            if (callback != null) {
                callback.onRewardedVideoAdRewarded();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRewardedVideoAdLoadFailed(String placementId, com.adtiming.mediationsdk.adt.utils.error.AdTimingError error) {
        try {
            RewardedVideoCallback callback = mVideoListeners.get(placementId);
            if (callback != null) {
                callback.onRewardedVideoLoadFailed(AdapterErrorBuilder.buildLoadError(
                                AdapterErrorBuilder.AD_UNIT_REWARDED_VIDEO, mAdapterName, error.getCode(), error.toString()));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRewardedVideoAdClicked(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onVideoAdClicked : " + placementId);
            RewardedVideoCallback callback = mVideoListeners.get(placementId);
            if (callback != null) {
                callback.onRewardedVideoAdClicked();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onVideoAdEvent(String placementId, String event) {
    }

    @Override
    public void onRewardedVideoAdShowFailed(String placementId, com.adtiming.mediationsdk.adt.utils.error.AdTimingError error) {
        try {
            AdLog.getSingleton().LogE(TAG + "onVideoAdShowFailed : " + placementId + " cause :" + error);
            RewardedVideoCallback callback = mVideoListeners.get(placementId);
            if (callback != null) {
                callback.onRewardedVideoAdShowFailed(AdapterErrorBuilder.buildShowError(
                        AdapterErrorBuilder.AD_UNIT_REWARDED_VIDEO, mAdapterName, error.getCode(), error.toString()));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRewardedVideoAdStarted(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onVideoStarted : " + placementId);
            RewardedVideoCallback callback = mVideoListeners.get(placementId);
            if (callback != null) {
                callback.onRewardedVideoAdStarted();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRewardedVideoAdEnded(String placementId) {
        try {
            AdLog.getSingleton().LogD(TAG + "onVideoEnded : " + placementId);
            RewardedVideoCallback callback = mVideoListeners.get(placementId);
            if (callback != null) {
                callback.onRewardedVideoAdEnded();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * AdTiming sdk init state
     */
    private enum InitState {
        /**
         *
         */
        NOT_INIT,
        /**
         *
         */
        INIT_PENDING,
        /**
         *
         */
        INIT_SUCCESS,
        /**
         *
         */
        INIT_FAIL
    }
}
