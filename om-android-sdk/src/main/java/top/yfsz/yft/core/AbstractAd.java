// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.core;

import android.app.Activity;
import android.text.TextUtils;

import top.yfsz.yft.InitCallback;
import top.yfsz.yft.banner.AdSize;
import top.yfsz.yft.bid.AdTimingAuctionManager;
import top.yfsz.yft.bid.AdTimingBidResponse;
import top.yfsz.yft.bid.AuctionCallback;
import top.yfsz.yft.mediation.Callback;
import top.yfsz.yft.utils.ActLifecycle;
import top.yfsz.yft.utils.AdLog;
import top.yfsz.yft.utils.AdRateUtil;
import top.yfsz.yft.utils.AdsUtil;
import top.yfsz.yft.utils.DeveloperLog;
import top.yfsz.yft.utils.HandlerUtil;
import top.yfsz.yft.utils.IOUtil;
import top.yfsz.yft.utils.InsUtil;
import top.yfsz.yft.utils.PlacementUtils;
import top.yfsz.yft.utils.Preconditions;
import top.yfsz.yft.utils.WorkExecutor;
import top.yfsz.yft.utils.constant.CommonConstants;
import top.yfsz.yft.utils.crash.CrashUtil;
import top.yfsz.yft.utils.device.DeviceUtil;
import top.yfsz.yft.utils.error.Error;
import top.yfsz.yft.utils.error.ErrorBuilder;
import top.yfsz.yft.utils.error.ErrorCode;
import top.yfsz.yft.utils.helper.LrReportHelper;
import top.yfsz.yft.utils.helper.WaterFallHelper;
import top.yfsz.yft.utils.model.BaseInstance;
import top.yfsz.yft.utils.model.Placement;
import top.yfsz.yft.utils.model.PlacementInfo;
import top.yfsz.yft.utils.request.network.Request;
import top.yfsz.yft.utils.request.network.Response;
import top.yfsz.yft.utils.request.network.util.NetworkChecker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.HttpsURLConnection;

/**
 * The type Abstract ad.
 */
public abstract class AbstractAd extends Callback implements Request.OnRequestCallback,
        InitCallback, AuctionCallback {

    /**
     * The M placement.
     */
    protected Placement mPlacement;
    /**
     * The M placement id.
     */
    protected String mPlacementId;
    /**
     * The Is destroyed.
     */
    protected boolean isDestroyed;
    /**
     * The M act ref.
     */
    protected WeakReference<Activity> mActRef;
    /**
     * The M current ins.
     */
    protected BaseInstance mCurrentIns;
    /**
     * The M bid responses.
     */
    protected Map<Integer, AdTimingBidResponse> mBidResponses;

    /**
     * The Is manual triggered.
     */
    protected boolean isManualTriggered;
    private OmManager.LOAD_TYPE mLoadType;

    /**
     * The M total ins.
     */
    BaseInstance[] mTotalIns;
    private List<BaseInstance> mLastInstances;
    /**
     * The M bs.
     */
    int mBs;
    /**
     * The Is fo.
     */
    boolean isFo;
    /**
     * The M pt.
     */
    int mPt;

    /**
     * The M load ts.
     */
    protected long mLoadTs;
    /**
     * The M callback ts.
     */
    protected long mCallbackTs;

    protected AdSize mAdSize;

    /**
     * Gets ad type.
     *
     * @return the ad type
     */
    protected abstract int getAdType();

    /**
     * Gets placement info.
     *
     * @return the placement info
     */
    protected abstract PlacementInfo getPlacementInfo();

    /**
     * Dispatch ad request.
     */
    protected abstract void dispatchAdRequest();

    /**
     * On ad error callback.
     *
     * @param error the error
     */
    protected abstract void onAdErrorCallback(String error);

    /**
     * On ad ready callback.
     */
    protected abstract void onAdReadyCallback();

    /**
     * On ad click callback.
     */
    protected abstract void onAdClickCallback();

    /**
     * On ad show success callback.
     */
    protected void onAdShowedCallback() {
    }

    /**
     * On ad show failed callback.
     */
    protected void onAdShowFailedCallback(String error) {
    }

    /**
     * On ad close callback.
     */
    protected void onAdCloseCallback() {
    }

    /**
     * Instantiates a new Abstract ad.
     *
     * @param activity    the activity
     * @param placementId the placement id
     */
    AbstractAd(Activity activity, String placementId) {
        isDestroyed = false;
        mActRef = new WeakReference<>(activity);
        mPlacementId = placementId;
    }

    /**
     * Load ad.
     *
     * @param type the type
     */
    public void loadAd(OmManager.LOAD_TYPE type) {
        //load returns if in the middle of initialization
        if (InitImp.isInitRunning()) {
            OmManager.getInstance().pendingInit(this);
            return;
        }

        //checks if initialization was successful
        if (!InitImp.isInit()) {
            InitImp.reInitSDK(mActRef.get(), this);
            return;
        }
        delayLoad(type);
    }

    /**
     * Destroy.
     */
    public void destroy() {
        AdLog.getSingleton().LogD("Ad destroy placementId: " + mPlacementId);
        mPlacement = null;
        if (mActRef != null) {
            mActRef.clear();
            mActRef = null;
        }
        isDestroyed = true;
    }


    @Override
    public void onSuccess() {
        //initialization successful. starts loading
        delayLoad(OmManager.LOAD_TYPE.INIT);
    }

    @Override
    public void onError(Error error) {
        callbackAdErrorOnUiThread(error != null ? error.toString() : "");
    }

    @Override
    public void onBidComplete(List<AdTimingBidResponse> c2sResponses, List<AdTimingBidResponse> s2sResponses) {
        try {
            WaterFallHelper.wfRequest(getPlacementInfo(), mLoadType, c2sResponses, s2sResponses,
                    InsUtil.getInstanceLoadStatuses(mLastInstances),this);
            if (mBidResponses == null) {
                mBidResponses = new HashMap<>();
            }
            if (c2sResponses != null && !c2sResponses.isEmpty()) {
                storeC2sResult(c2sResponses);
            }
        } catch (Exception e) {
            callbackAdErrorOnUiThread(e.getMessage());
            CrashUtil.getSingleton().saveException(e);
            System.out.println("========");
            System.out.println("错误信息"+e);
        }
    }

    @Override
    public void onRequestSuccess(Response response) {
        //parses the response. gets ins array, bs size,pt size,fo switch,campaign info
        try {
            if (response == null || response.code() != HttpsURLConnection.HTTP_OK) {
                callbackAdErrorOnUiThread(ErrorCode.ERROR_NO_FILL);
                return;
            }

            JSONObject clInfo = new JSONObject(response.body().string());
            int code = clInfo.optInt("code");
            if (code != 0) {
                String msg = clInfo.optString("msg");
                DeveloperLog.LogE(msg);
                callbackAdErrorOnUiThread(msg);
                return;
            }

            mPlacement.setWfAbt(clInfo.optInt("abt"));
            clearLoadFailedInstances();
            BaseInstance[] tmp = WaterFallHelper.getArrayInstances(clInfo, mPlacement, mBs);
            if (tmp == null || tmp.length == 0) {
                DeveloperLog.LogD("Ad", "request cl success, but ins[] is empty" + mPlacement);
                callbackAdErrorOnUiThread(ErrorCode.ERROR_NO_FILL);
            } else {
                mTotalIns = tmp;
                Map<Integer, AdTimingBidResponse> bidResponseMap = WaterFallHelper.getS2sBidResponse(clInfo);
                if (bidResponseMap != null && !bidResponseMap.isEmpty()) {
                    if (mBidResponses == null) {
                        mBidResponses = new HashMap<>();
                    }
                    mBidResponses.putAll(bidResponseMap);
                }
                doLoadOnUiThread();
            }
        } catch (IOException | JSONException e) {
            CrashUtil.getSingleton().saveException(e);
            callbackAdErrorOnUiThread(ErrorCode.ERROR_NO_FILL);
        } finally {
            IOUtil.closeQuietly(response);
        }
    }

    @Override
    public void onRequestFailed(String error) {//if failed to request cl, calls ad error callback
        callbackAdErrorOnUiThread(ErrorCode.ERROR_NO_FILL);
    }

    /**
     * Sets manual triggered.
     *
     * @param isManualTriggered the is manual triggered
     */
    public void setManualTriggered(boolean isManualTriggered) {
        this.isManualTriggered = isManualTriggered;
    }

    /**
     * Clean after close or failed.
     */
    protected void cleanAfterCloseOrFailed() {
        mTotalIns = null;
        mBs = 0;
        mPt = 0;
        isFo = false;
        if (mCurrentIns != null) {
            mCurrentIns.setObject(null);
            mCurrentIns.setStart(0);
            mCurrentIns = null;
        }
    }

    /**
     * Ad loading error callback
     *
     * @param error error reason
     */
    void callbackAdErrorOnUiThread(final String error) {
        if (isDestroyed) {
            return;
        }
        AdLog.getSingleton().LogE("Ad load failed placementId: " + mPlacementId + ", " + error);
        if (mLoadTs > mCallbackTs) {
            mCallbackTs = System.currentTimeMillis();
        }
//        cleanAfterCloseOrFailed();
        HandlerUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onAdErrorCallback(error);
            }
        });
    }

    /**
     * Ad loading success callback
     */
    void callbackAdReadyOnUiThread() {
        if (isDestroyed) {
            return;
        }
        AdLog.getSingleton().LogD("Ad load success placementId: " + mPlacementId);
        if (mLoadTs > mCallbackTs) {
            mCallbackTs = System.currentTimeMillis();
        }
        HandlerUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onAdReadyCallback();
            }
        });
    }

    /**
     * Ad click callback
     */
    void callbackAdClickOnUiThread() {
        if (isDestroyed) {
            return;
        }
        AdLog.getSingleton().LogD("Ad clicked placementId: " + mPlacementId);
        HandlerUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onAdClickCallback();
            }
        });
    }

    /**
     * Ad open callback
     */
    void callbackAdShowedOnUiThread() {
        if (isDestroyed) {
            return;
        }
        HandlerUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onAdShowedCallback();
            }
        });
    }

    /**
     * Ad open callback
     */
    void callbackAdShowFailedOnUiThread(final String error) {
        if (isDestroyed) {
            return;
        }
        HandlerUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onAdShowFailedCallback(error);
            }
        });
    }

    /**
     * Ad open callback
     */
    void callbackAdCloseOnUiThread() {
        if (isDestroyed) {
            return;
        }
        HandlerUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onAdCloseCallback();
            }
        });
    }

    /**
     * All-is-ready reporting
     */
    void aReadyReport() {
        if (isDestroyed) {
            return;
        }
        if (isManualTriggered) {
            LrReportHelper.report(mPlacementId, OmManager.LOAD_TYPE.MANUAL.getValue(), mPlacement.getWfAbt(),
                    CommonConstants.WATERFALL_READY, 0);
        } else {
            LrReportHelper.report(mPlacementId, OmManager.LOAD_TYPE.INTERVAL.getValue(), mPlacement.getWfAbt(),
                    CommonConstants.WATERFALL_READY, 0);
        }
    }

    /**
     * Instance-level load reporting
     *
     * @param instances the instances
     */
    protected void iLoadReport(BaseInstance instances) {
        if (isDestroyed) {
            return;
        }
        if (isManualTriggered) {
            LrReportHelper.report(instances, OmManager.LOAD_TYPE.MANUAL.getValue(), mPlacement.getWfAbt(),
                    CommonConstants.INSTANCE_LOAD, 0);
        } else {
            LrReportHelper.report(instances, OmManager.LOAD_TYPE.INTERVAL.getValue(), mPlacement.getWfAbt(),
                    CommonConstants.INSTANCE_LOAD, 0);
        }
    }

    /**
     * Instance-level ready reporting
     *
     * @param instances the instances
     */
    void iReadyReport(BaseInstance instances) {
        if (isDestroyed || instances.getHb() == 1) {
            return;
        }
        if (isManualTriggered) {
            LrReportHelper.report(instances, OmManager.LOAD_TYPE.MANUAL.getValue(), mPlacement.getWfAbt(),
                    CommonConstants.INSTANCE_READY, 0);
        } else {
            LrReportHelper.report(instances, OmManager.LOAD_TYPE.INTERVAL.getValue(), mPlacement.getWfAbt(),
                    CommonConstants.INSTANCE_READY, 0);
        }
    }

    /**
     * Instance-level impression reporting
     *
     * @param instances the instances
     */
    protected void insImpReport(BaseInstance instances) {
        try {
            if (isDestroyed) {
                return;
            }
            instances.onInsShow(null);
            int bid = 0;
            if (instances.getHb() == 1) {
                bid = 1;
            }
            if (isManualTriggered) {
                LrReportHelper.report(instances, OmManager.LOAD_TYPE.MANUAL.getValue(), mPlacement.getWfAbt(),
                        CommonConstants.INSTANCE_IMPR, bid);
            } else {
                LrReportHelper.report(instances, OmManager.LOAD_TYPE.INTERVAL.getValue(), mPlacement.getWfAbt(),
                        CommonConstants.INSTANCE_IMPR, bid);
            }
        } catch (Exception e) {
            CrashUtil.getSingleton().saveException(e);
        }
    }

    /**
     * Instance-level click reporting
     *
     * @param instances the instances
     */
    void insClickReport(BaseInstance instances) {
        if (isDestroyed) {
            return;
        }
        instances.onInsClick(null);
        int bid = 0;
        if (instances.getHb() == 1) {
            bid = 1;
        }
        if (isManualTriggered) {
            LrReportHelper.report(instances, OmManager.LOAD_TYPE.MANUAL.getValue(), mPlacement.getWfAbt(),
                    CommonConstants.INSTANCE_CLICK, bid);
        } else {
            LrReportHelper.report(instances, OmManager.LOAD_TYPE.INTERVAL.getValue(), mPlacement.getWfAbt(),
                    CommonConstants.INSTANCE_CLICK, bid);
        }
    }

    /**
     * Checks if callback has been trigged
     *
     * @return the boolean
     */
    boolean hasCallbackToUser() {
        return mLoadTs <= mCallbackTs;
    }

    /**
     * Check act ref boolean.
     *
     * @return the boolean
     */
    protected boolean checkActRef() {
        if (mActRef == null || mActRef.get() == null || !DeviceUtil.isActivityAvailable(mActRef.get())) {
            Activity activity = ActLifecycle.getInstance().getActivity();
            if (activity == null) {
                return false;
            }
            mActRef = new WeakReference<>(activity);
        }
        return true;
    }

    /**
     * Loads ads on UI thread
     */
    private void doLoadOnUiThread() {
        if (isDestroyed) {
            return;
        }
        HandlerUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resetFields();
                dispatchAdRequest();
            }
        });
    }

    private void delayLoad(OmManager.LOAD_TYPE type) {
        try {
            //returns if load can't start
            Error error = checkLoadAvailable();
            if (error != null) {
                AdsUtil.loadBlockedReport(Preconditions.checkNotNull(mPlacement) ? mPlacement.getId() : "", error);
                return;
            }
            AdLog.getSingleton().LogD("Ad load placementId: " + mPlacementId);
            //load start timestamp
            mLoadTs = System.currentTimeMillis();
            mLoadType = type;
            mBs = mPlacement.getBs();
            mPt = mPlacement.getPt();
            isFo = mPlacement.getFo() == 1;
            if (mBs == 0) {
                mBs = 3;
            }
            if (mBidResponses != null) {
                mBidResponses.clear();
            }
            WorkExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        AdTimingAuctionManager.getInstance().bid(mActRef.get(), mPlacement.getId(), mPlacement.getT(),
                                mAdSize, AbstractAd.this);
                    } catch (Exception e) {
                        DeveloperLog.LogD("load ad error", e);
                        CrashUtil.getSingleton().saveException(e);
                    }
                }
            });
        } catch (Exception e) {
            callbackAdErrorOnUiThread(e.getMessage());
            CrashUtil.getSingleton().saveException(e);
        }
    }

    /**
     * Various checks before load can start
     */
    private Error checkLoadAvailable() {
        //empty placementId?
        if (TextUtils.isEmpty(mPlacementId)) {
            callbackAdErrorOnUiThread(ErrorCode.ERROR_PLACEMENT_ID);
            return ErrorBuilder.build(ErrorCode.CODE_LOAD_INVALID_REQUEST
                    , ErrorCode.MSG_LOAD_INVALID_REQUEST, ErrorCode.CODE_INTERNAL_REQUEST_PLACEMENTID);
        }
        //activity effective?
        if (!checkActRef()) {
            callbackAdErrorOnUiThread(ErrorCode.ERROR_ACTIVITY);
            return ErrorBuilder.build(ErrorCode.CODE_LOAD_INVALID_REQUEST
                    , ErrorCode.MSG_LOAD_INVALID_REQUEST, ErrorCode.CODE_INTERNAL_REQUEST_ACTIVITY);
        }

        //network available?
        if (!NetworkChecker.isAvailable(mActRef.get())) {
            callbackAdErrorOnUiThread(ErrorCode.ERROR_NETWORK_NOT_AVAILABLE);
            return ErrorBuilder.build(ErrorCode.CODE_LOAD_NETWORK_ERROR
                    , ErrorCode.MSG_LOAD_INVALID_REQUEST, ErrorCode.CODE_INTERNAL_UNKNOWN_OTHER);
        }

        if (isDestroyed) {
            callbackAdErrorOnUiThread(ErrorCode.ERROR_LOAD_AD_BUT_DESTROYED);
            return ErrorBuilder.build(ErrorCode.CODE_LOAD_INVALID_REQUEST
                    , ErrorCode.MSG_LOAD_INVALID_REQUEST, ErrorCode.CODE_INTERNAL_UNKNOWN_OTHER);
        }
        if (mLoadTs > mCallbackTs) {//loading in progress
            return ErrorBuilder.build(ErrorCode.CODE_LOAD_INVALID_REQUEST
                    , ErrorCode.MSG_LOAD_INVALID_REQUEST, ErrorCode.CODE_INTERNAL_UNKNOWN_OTHER);
        }

        if (mPlacement == null) {
            mPlacement = PlacementUtils.getPlacement(mPlacementId);
            if (mPlacement == null) {
                callbackAdErrorOnUiThread(ErrorCode.ERROR_PLACEMENT_EMPTY);
                return ErrorBuilder.build(ErrorCode.CODE_LOAD_INVALID_REQUEST
                        , ErrorCode.MSG_LOAD_INVALID_REQUEST, ErrorCode.CODE_INTERNAL_UNKNOWN_OTHER);
            }
            if (mPlacement.getT() != getAdType()) {
                callbackAdErrorOnUiThread(ErrorCode.ERROR_PLACEMENT_TYPE);
                return ErrorBuilder.build(ErrorCode.CODE_LOAD_INVALID_REQUEST
                        , ErrorCode.MSG_LOAD_INVALID_REQUEST, ErrorCode.CODE_INTERNAL_UNKNOWN_OTHER);
            }
        }

        if (AdRateUtil.shouldBlockPlacement(mPlacement) || AdRateUtil.isPlacementCapped(mPlacement)) {
            callbackAdErrorOnUiThread(ErrorCode.ERROR_NO_FILL);
            return ErrorBuilder.build(ErrorCode.CODE_LOAD_INVALID_REQUEST
                    , ErrorCode.MSG_LOAD_INVALID_REQUEST, ErrorCode.CODE_INTERNAL_UNKNOWN_OTHER);
        }
        return null;
    }

    private void resetFields() {
        if (mCurrentIns != null) {
            mCurrentIns = null;
        }
        isDestroyed = false;
    }

    private void storeC2sResult(List<AdTimingBidResponse> c2sResult) {
        for (AdTimingBidResponse bidResponse : c2sResult) {
            if (bidResponse == null) {
                continue;
            }
            mBidResponses.put(bidResponse.getIid(), bidResponse);
        }
    }

    private void clearLoadFailedInstances() {
        if (mLastInstances != null) {
            mLastInstances.clear();
        }
    }

    void addLoadFailedInstance(BaseInstance instance) {
        if (mLastInstances == null) {
            mLastInstances = new CopyOnWriteArrayList<>();
        }
        if (instance != null) {
            mLastInstances.add(instance);
        }
    }
}
