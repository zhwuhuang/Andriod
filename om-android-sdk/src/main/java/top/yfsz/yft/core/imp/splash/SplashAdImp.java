package top.yfsz.yft.core.imp.splash;

import android.app.Activity;
import android.text.TextUtils;
import android.view.ViewGroup;

import top.yfsz.yft.bid.AuctionUtil;
import top.yfsz.yft.core.AbstractHybridAd;
import top.yfsz.yft.core.AdManager;
import top.yfsz.yft.core.OmManager;
import top.yfsz.yft.mediation.CustomSplashEvent;
import top.yfsz.yft.splash.SplashAdListener;
import top.yfsz.yft.utils.AdsUtil;
import top.yfsz.yft.utils.HandlerUtil;
import top.yfsz.yft.utils.PlacementUtils;
import top.yfsz.yft.utils.constant.CommonConstants;
import top.yfsz.yft.utils.error.ErrorCode;
import top.yfsz.yft.utils.event.EventId;
import top.yfsz.yft.utils.event.EventUploadManager;
import top.yfsz.yft.utils.model.BaseInstance;
import top.yfsz.yft.utils.model.PlacementInfo;

import java.util.Map;

public class SplashAdImp extends AbstractHybridAd {

    private SplashAdListener mAdListener;

    private long mLoadTimeout;

    private int mWidth, mHeight;

    SplashAdImp(Activity activity, String placementId) {
        super(activity, placementId);
    }

    void setLoadTimeout(long timeout) {
        mLoadTimeout = timeout;
    }

    @Override
    public void loadAd(OmManager.LOAD_TYPE type) {
        AdsUtil.callActionReport(mPlacementId, 0, EventId.CALLED_LOAD);
        super.loadAd(type);
    }

    @Override
    protected void loadInsOnUIThread(BaseInstance instances) throws Throwable {
        instances.reportInsLoad(EventId.INSTANCE_LOAD);
        if (!checkActRef()) {
            onInsError(instances, ErrorCode.ERROR_ACTIVITY);
            return;
        }
        if (TextUtils.isEmpty(instances.getKey())) {
            onInsError(instances, ErrorCode.ERROR_EMPTY_INSTANCE_KEY);
            return;
        }
        CustomSplashEvent splashEvent = getAdEvent(instances);
        if (splashEvent == null) {
            onInsError(instances, ErrorCode.ERROR_CREATE_MEDATION_ADAPTER);
            return;
        }
        instances.setStart(System.currentTimeMillis());
        String payload = "";
        if (mBidResponses != null && mBidResponses.containsKey(instances.getId())) {
            payload = AuctionUtil.generateStringRequestData(mBidResponses.get(instances.getId()));
        }
        Map<String, String> placementInfo = PlacementUtils.getPlacementInfo(mPlacementId, instances, payload);
        placementInfo.put("Timeout", String.valueOf(mLoadTimeout));
        placementInfo.put("Width", String.valueOf(mWidth));
        placementInfo.put("Height", String.valueOf(mHeight));
        splashEvent.loadAd(mActRef.get(), placementInfo);
        iLoadReport(instances);
    }

    @Override
    protected boolean isInsReady(BaseInstance instance) {
        return isReady(instance);
    }

    public void setAdListener(SplashAdListener listener) {
        mAdListener = listener;
    }

    private boolean isReady(BaseInstance instance) {
        if (instance == null) {
            return false;
        }
        CustomSplashEvent splashEvent = getAdEvent(instance);
        if (splashEvent == null) {
            return false;
        }
        return splashEvent.isReady();
    }

    public boolean isReady() {
        return isReady(mCurrentIns);
    }

    public void show(ViewGroup container) {
        if (container == null) {
            onAdShowFailed("SplashAd Container is null");
            return;
        }
        if (!isReady()) {
            onAdShowFailed("SplashAd not ready");
            return;
        }
        getAdEvent(mCurrentIns).show(container);
    }

    private void onAdShowFailed(final String error) {
        HandlerUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onAdShowFailedCallback(error);
            }
        });
    }

    @Override
    protected int getAdType() {
        return CommonConstants.SPLASH;
    }

    @Override
    protected PlacementInfo getPlacementInfo() {
        return new PlacementInfo(mPlacementId).getPlacementInfo(getAdType());
    }

    @Override
    protected void onAdErrorCallback(String error) {
        if (mAdListener != null) {
            EventUploadManager.getInstance().uploadEvent(EventId.CALLBACK_LOAD_ERROR,
                    PlacementUtils.placementEventParams(mPlacementId));
            mAdListener.onSplashAdFailed(error);
        }
    }

    @Override
    protected void onAdReadyCallback() {
        if (mAdListener != null) {
            EventUploadManager.getInstance().uploadEvent(EventId.CALLBACK_LOAD_SUCCESS,
                    PlacementUtils.placementEventParams(mPlacementId));
            mAdListener.onSplashAdLoad();
        }
    }

    @Override
    protected void onAdClickCallback() {
        if (mAdListener != null) {
            EventUploadManager.getInstance().uploadEvent(EventId.CALLBACK_CLICK,
                    PlacementUtils.placementEventParams(mPlacementId));
            mAdListener.onSplashAdClicked();
        }
    }

    @Override
    protected void onAdShowedCallback() {
        if (mAdListener != null) {
            EventUploadManager.getInstance().uploadEvent(EventId.CALLBACK_PRESENT_SCREEN,
                    PlacementUtils.placementEventParams(mPlacementId));
            mAdListener.onSplashAdShowed();
        }
    }

    @Override
    protected void onAdShowFailedCallback(String error) {
        if (mAdListener != null) {
            EventUploadManager.getInstance().uploadEvent(EventId.CALLBACK_SHOW_FAILED,
                    PlacementUtils.placementEventParams(mPlacementId));
            mAdListener.onSplashAdShowFailed(error);
        }
    }

    @Override
    protected void onAdCloseCallback() {
        if (mAdListener != null) {
            EventUploadManager.getInstance().uploadEvent(EventId.CALLBACK_DISMISS_SCREEN,
                    PlacementUtils.placementEventParams(mPlacementId));
            mAdListener.onSplashAdDismissed();
        }
    }

    @Override
    protected synchronized void onInsClosed(String instanceKey, String instanceId) {
        super.onInsClosed(instanceKey, instanceId);
        if (mCurrentIns != null) {
            destroyAdEvent(mCurrentIns);
            AdManager.getInstance().removeInsAdEvent(mCurrentIns);
        }
        cleanAfterCloseOrFailed();
    }

    @Override
    protected void onInsTick(String instanceKey, String instanceId, long millisUntilFinished) {
        super.onInsTick(instanceKey, instanceId, millisUntilFinished);
        if (mAdListener != null) {
            mAdListener.onSplashAdTick(millisUntilFinished);
        }
    }

    private CustomSplashEvent getAdEvent(BaseInstance instances) {
        return (CustomSplashEvent) AdManager.getInstance().getInsAdEvent(CommonConstants.SPLASH, instances);
    }

    void setSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }
}
