// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.core.imp.interstitialad;

import top.yfsz.yft.utils.model.Instance;
import top.yfsz.yft.core.OmManager;
import top.yfsz.yft.interstitial.InterstitialAdListener;
import top.yfsz.yft.mediation.MediationInterstitialListener;
import top.yfsz.yft.utils.DeveloperLog;
import top.yfsz.yft.utils.error.Error;
import top.yfsz.yft.utils.error.ErrorCode;
import top.yfsz.yft.utils.model.PlacementInfo;
import top.yfsz.yft.core.AbstractAdsManager;

import java.util.Map;

public final class IsManager extends AbstractAdsManager implements IsManagerListener {

    public IsManager() {
        super();
    }

    public void initInterstitialAd() {
        checkScheduleTaskStarted();
    }

    public void loadInterstitialAd() {
        loadAdWithAction(OmManager.LOAD_TYPE.MANUAL);
    }

    public void showInterstitialAd(String scene) {
        showAd(scene);
    }

    public boolean isInterstitialAdReady() {
        return isPlacementAvailable();
    }

    public void setInterstitialAdListener(InterstitialAdListener listener) {
        mListenerWrapper.setInterstitialAdListener(listener);
    }

    public void setMediationInterstitialAdListener(MediationInterstitialListener listener) {
        mListenerWrapper.setMediationInterstitialListener(listener);
    }

    @Override
    protected PlacementInfo getPlacementInfo() {
        return new PlacementInfo(mPlacement.getId()).getPlacementInfo(mPlacement.getT());
    }

    @Override
    protected void initInsAndSendEvent(Instance instance) {
        if (!(instance instanceof IsInstance)) {
            instance.setMediationState(Instance.MEDIATION_STATE.INIT_FAILED);
            onInsInitFailed(instance, new Error(ErrorCode.CODE_LOAD_UNKNOWN_INTERNAL_ERROR,
                    "current is not an rewardedVideo adUnit", -1));
            return;
        }
        IsInstance isInstance = (IsInstance) instance;
        isInstance.setIsManagerListener(this);
        isInstance.initIs(mActivityReference.get());
    }

    @Override
    protected boolean isInsAvailable(Instance instance) {
        if (instance instanceof IsInstance) {
            return ((IsInstance) instance).isIsAvailable();
        }
        return false;
    }

    @Override
    protected void insShow(final Instance instance) {
        ((IsInstance) instance).showIs(mActivityReference.get(), mScene);
    }

    @Override
    protected void insLoad(Instance instance) {
        IsInstance isInstance = (IsInstance) instance;
        isInstance.loadIs(mActivityReference.get());
    }

    @Override
    protected void inLoadWithBid(Instance instance, Map<String, Object> extras) {
        IsInstance isInstance = (IsInstance) instance;
        isInstance.loadIsWithBid(mActivityReference.get(), extras);
    }

    @Override
    protected void onAvailabilityChanged(boolean available, Error error) {
        mListenerWrapper.onInterstitialAdAvailabilityChanged(available);
    }

    @Override
    protected void callbackAvailableOnManual() {
        super.callbackAvailableOnManual();
        mListenerWrapper.onInterstitialAdAvailabilityChanged(true);
        mListenerWrapper.onInterstitialAdLoadSuccess();
    }

    @Override
    protected void callbackLoadSuccessOnManual() {
        super.callbackLoadSuccessOnManual();
        mListenerWrapper.onInterstitialAdLoadSuccess();
    }

    @Override
    protected void callbackLoadFailedOnManual(Error error) {
        super.callbackLoadFailedOnManual(error);
        mListenerWrapper.onInterstitialAdLoadFailed(error);
    }

    @Override
    protected void callbackLoadError(Error error) {
        mListenerWrapper.onInterstitialAdLoadFailed(error);
        boolean hasCache = hasAvailableCache();
        if (shouldNotifyAvailableChanged(hasCache)) {
            mListenerWrapper.onInterstitialAdAvailabilityChanged(hasCache);
        }
        super.callbackLoadError(error);
    }

    @Override
    protected void callbackShowError(Error error) {
        super.callbackShowError(error);
        mListenerWrapper.onInterstitialAdShowFailed(mScene, error);
    }

    @Override
    protected void callbackAdClosed() {
        mListenerWrapper.onInterstitialAdClosed(mScene);
    }

    @Override
    public void onInterstitialAdInitSuccess(IsInstance isInstance) {
        loadInsAndSendEvent(isInstance);
    }

    @Override
    public void onInterstitialAdInitFailed(Error error, IsInstance isInstance) {
        onInsInitFailed(isInstance, error);
    }

    @Override
    public void onInterstitialAdShowFailed(Error error, IsInstance isInstance) {
        isInShowingProgress = false;
        mListenerWrapper.onInterstitialAdShowFailed(mScene, error);
    }

    @Override
    public void onInterstitialAdShowSuccess(IsInstance isInstance) {
        onInsOpen(isInstance);
        mListenerWrapper.onInterstitialAdShowed(mScene);
    }

    @Override
    public void onInterstitialAdClick(IsInstance isInstance) {
        mListenerWrapper.onInterstitialAdClicked(mScene);
        onInsClick(isInstance);
    }

    @Override
    public void onInterstitialAdClosed(IsInstance isInstance) {
        onInsClose();
    }

    @Override
    public void onInterstitialAdLoadSuccess(IsInstance isInstance) {
        onInsReady(isInstance);
    }

    @Override
    public void onInterstitialAdLoadFailed(Error error, IsInstance isInstance) {
        DeveloperLog.LogD("IsManager onInterstitialAdLoadFailed : " + isInstance + " error : " + error);
        onInsLoadFailed(isInstance, error);
    }
}
