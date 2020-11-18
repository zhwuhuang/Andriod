// Copyright 2019 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.openmediation.sdk.mobileads;

import android.app.Activity;
import android.content.Context;

import top.yfsz.yft.mediation.AdapterErrorBuilder;
import top.yfsz.yft.mediation.CustomBannerEvent;
import top.yfsz.yft.mediation.MediationInfo;
import com.openmediation.sdk.mobileads.unity.BuildConfig;
import top.yfsz.yft.utils.AdLog;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.util.Map;


public class UnityBanner extends CustomBannerEvent implements BannerView.IListener {
    private static final String TAG = "OM-Unity: ";
    private boolean mDidInit = false;

    private BannerView mBannerView;

    @Override
    public void setGDPRConsent(Context context, boolean consent) {
        super.setGDPRConsent(context, consent);
        if (context != null) {
            MetaData gdprMetaData = new MetaData(context);
            gdprMetaData.set("gdpr.consent", consent);
            gdprMetaData.commit();
        }
    }

    @Override
    public void setAgeRestricted(Context context, boolean restricted) {
        super.setAgeRestricted(context, restricted);
        if (context != null) {
            MetaData ageGateMetaData = new MetaData(context);
            ageGateMetaData.set("privacy.useroveragelimit", restricted);
            ageGateMetaData.commit();
        }
    }

    @Override
    public void setUSPrivacyLimit(Context context, boolean value) {
        super.setUSPrivacyLimit(context, value);
        if (context != null) {
            MetaData privacyMetaData = new MetaData(context);
            privacyMetaData.set("privacy.consent", !value);
            privacyMetaData.commit();
        }
    }

    @Override
    public void loadAd(Activity activity, Map<String, String> config) throws Throwable {
        super.loadAd(activity, config);

        if (!check(activity, config)) {
            return;
        }
        AdLog.getSingleton().LogD(TAG + "Unity Banner Ad Start Load : " + mInstancesKey);
        if (!mDidInit) {
            initSDK(activity, config.get("AppKey"));
            mDidInit = true;
        }

        UnityBannerSize bannerSize = getAdSize(activity, config);
        BannerView bannerView = new BannerView(activity, mInstancesKey, bannerSize);
        bannerView.setListener(this);
        bannerView.load();
    }

    private synchronized void initSDK(Activity activity, String appKey) {
        MediationMetaData mediationMetaData = new MediationMetaData(activity);
        mediationMetaData.setName("Om");
        mediationMetaData.setVersion(BuildConfig.VERSION_NAME);
        mediationMetaData.commit();
        UnityAds.initialize(activity, appKey);
    }

    @Override
    public int getMediation() {
        return MediationInfo.MEDIATION_ID_4;
    }

    @Override
    public void destroy(Activity activity) {
        if (mBannerView != null) {
            mBannerView.destroy();
            mBannerView = null;
        }
        isDestroyed = true;
    }

    @Override
    public void onBannerLoaded(BannerView bannerAdView) {
        if (isDestroyed) {
            return;
        }
        if (mBannerView != null) {
            mBannerView.destroy();
        }
        mBannerView = bannerAdView;
        onInsReady(bannerAdView);
    }

    @Override
    public void onBannerClick(BannerView bannerAdView) {
        if (isDestroyed) {
            return;
        }
        AdLog.getSingleton().LogD(TAG + "Unity Banner Ad Click : " + mInstancesKey);
        onInsClicked();
    }

    @Override
    public void onBannerFailedToLoad(BannerView bannerAdView, BannerErrorInfo errorInfo) {
        if (isDestroyed) {
            return;
        }
        onInsError(AdapterErrorBuilder.buildLoadError(
                AdapterErrorBuilder.AD_UNIT_BANNER, mAdapterName, errorInfo.errorCode.name()));
    }

    @Override
    public void onBannerLeftApplication(BannerView bannerView) {

    }

    private UnityBannerSize getAdSize(Context context, Map<String, String> config) {
        String bannerDesc = getBannerDesc(config);
        switch (bannerDesc) {
            case DESC_BANNER:
                return new UnityBannerSize(320, 50);
            case DESC_LEADERBOARD:
                return new UnityBannerSize(728, 90);
            case DESC_RECTANGLE:
                return new UnityBannerSize(300, 250);
            case DESC_SMART:
                if (isLargeScreen(context)) {
                    return new UnityBannerSize(728, 90);
                } else {
                    return new UnityBannerSize(320, 50);
                }
            default:
                return UnityBannerSize.getDynamicSize(context);
        }
    }

}
