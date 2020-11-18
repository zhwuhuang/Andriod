// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.core;

import android.app.Activity;
import android.text.TextUtils;

import top.yfsz.yft.InitCallback;
import top.yfsz.yft.bid.AdTimingAuctionManager;
import top.yfsz.yft.utils.ActLifecycle;
import top.yfsz.yft.utils.AdLog;
import top.yfsz.yft.utils.AdtUtil;
import top.yfsz.yft.utils.DeveloperLog;
import top.yfsz.yft.utils.HandlerUtil;
import top.yfsz.yft.utils.IOUtil;
import top.yfsz.yft.utils.JsonUtil;
import top.yfsz.yft.utils.SdkUtil;
import top.yfsz.yft.utils.WorkExecutor;
import top.yfsz.yft.utils.cache.DataCache;
import top.yfsz.yft.utils.constant.CommonConstants;
import top.yfsz.yft.utils.constant.KeyConstants;
import top.yfsz.yft.utils.crash.CrashUtil;
import top.yfsz.yft.utils.device.DeviceUtil;
import top.yfsz.yft.utils.error.Error;
import top.yfsz.yft.utils.error.ErrorBuilder;
import top.yfsz.yft.utils.error.ErrorCode;
import top.yfsz.yft.utils.event.EventId;
import top.yfsz.yft.utils.event.EventUploadManager;
import top.yfsz.yft.utils.helper.ConfigurationHelper;
import top.yfsz.yft.utils.model.Configurations;
import top.yfsz.yft.utils.request.network.Request;
import top.yfsz.yft.utils.request.network.Response;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Init imp.
 */
public final class InitImp {
    private static AtomicBoolean hasInit = new AtomicBoolean(false);
    private static AtomicBoolean isInitRunning = new AtomicBoolean(false);
    private static InitCallback mCallback;
    private static long sInitStart;

    /**
     * init method
     *
     * @param activity the activity
     * @param appKey   the app key
     * @param callback the callback
     */
    public static void init(final Activity activity, final String appKey, String channel, final InitCallback callback) {
        //
        if (hasInit.get()) {
            return;
        }

        if (isInitRunning.get()) {
            return;
        }

        if (activity == null) {
            Error error = new Error(ErrorCode.CODE_INIT_INVALID_REQUEST
                    , ErrorCode.MSG_INIT_INVALID_REQUEST, ErrorCode.CODE_INTERNAL_REQUEST_ACTIVITY);
            DeveloperLog.LogE(error.toString() + ", init failed because activity is null");
            callbackInitErrorOnUIThread(error);
            return;
        }
        isInitRunning.set(true);
        sInitStart = System.currentTimeMillis();
        mCallback = callback;



        AdtUtil.init(activity);
//        SensorManager.getSingleton();
        ActLifecycle.getInstance().init(activity);
        EventUploadManager.getInstance().init(activity.getApplicationContext());
        EventUploadManager.getInstance().uploadEvent(EventId.INIT_START);
        WorkExecutor.execute(new InitAsyncRunnable(appKey, channel));
    }

    /**
     * Re init sdk.
     *
     * @param activity the activity
     * @param callback the callback
     */
    static void reInitSDK(Activity activity, final InitCallback callback) {
        if (DataCache.getInstance().containsKey(KeyConstants.KEY_APP_KEY)) {
            String appKey = DataCache.getInstance().getFromMem(KeyConstants.KEY_APP_KEY, String.class);
            String appChannel = DataCache.getInstance().getFromMem(KeyConstants.KEY_APP_CHANNEL, String.class);
            InitImp.init(activity, appKey, appChannel, new InitCallback() {
                @Override
                public void onSuccess() {
                    DeveloperLog.LogD("reInitSDK success");
                    callback.onSuccess();
                }

                @Override
                public void onError(Error error) {
                    callback.onError(error);
                }
            });
        } else {
            Error error = ErrorBuilder.build(ErrorCode.CODE_LOAD_INVALID_REQUEST
                    , ErrorCode.ERROR_NOT_INIT, ErrorCode.CODE_INTERNAL_REQUEST_APPKEY);
            callback.onError(error);
        }
    }

    /**
     * init success?
     *
     * @return the boolean
     */
    public static boolean isInit() {
        return hasInit.get();
    }

    /**
     * Is init running boolean.
     *
     * @return the boolean
     */
    static boolean isInitRunning() {
        return isInitRunning.get();
    }

    private static void requestConfig(Activity activity, String appKey) throws Exception {
        DeveloperLog.LogD("Om init request config");
        //requests Config
        ConfigurationHelper.getConfiguration(appKey, new InitRequestCallback(activity, appKey));
    }

    /**
     * Inits global utils
     */
    private static void initUtil() {
        AdapterRepository.getInstance().init();
        DataCache.getInstance().init(AdtUtil.getApplication());
        DataCache.getInstance().set(DeviceUtil.preFetchDeviceInfo(AdtUtil.getApplication()));
//        OaidHelper.initOaidServer(AdtUtil.getApplication());
    }

    private static void doAfterGetConfig(String appKey, Configurations config) {
        try {
            DeveloperLog.enableDebug(AdtUtil.getApplication(), config.getD() == 1);
            EventUploadManager.getInstance().updateReportSettings(config);
            //reports error logs
            CrashUtil.getSingleton().uploadException(config, appKey);
        } catch (Exception e) {
            DeveloperLog.LogD("doAfterGetConfig  exception : ", e);
            CrashUtil.getSingleton().saveException(e);
        }
    }

    private static void callbackInitErrorOnUIThread(final Error result) {
        AdLog.getSingleton().LogE("Init Failed: " + result);
        HandlerUtil.runOnUiThread(new InitFailRunnable(result));
    }

    private static void callbackInitSuccessOnUIThread() {
        AdLog.getSingleton().LogD("Init Success");
        HandlerUtil.runOnUiThread(new InitSuccessRunnable());
    }

    private static void initCompleteReport(int eventId, Error error) {
        JSONObject jsonObject = new JSONObject();
        if (error != null) {
            JsonUtil.put(jsonObject, "msg", error);
        }
        if (sInitStart != 0) {
            int dur = (int) (System.currentTimeMillis() - sInitStart) / 1000;
            JsonUtil.put(jsonObject, "duration", dur);
        }
        EventUploadManager.getInstance().uploadEvent(eventId, jsonObject);
    }

    private static class InitSuccessRunnable implements Runnable {

        @Override
        public void run() {
            DeveloperLog.LogD("Om init Success ");
            hasInit.set(true);
            isInitRunning.set(false);
            if (mCallback != null) {
                mCallback.onSuccess();
            }
            initCompleteReport(EventId.INIT_COMPLETE, null);
        }
    }

    private static class InitAsyncRunnable implements Runnable {

        private String appKey;
        private String appChannel;

        private InitAsyncRunnable(String appKey, String appChannel) {
            this.appKey = appKey;
            this.appChannel = appChannel;
        }

        @Override
        public void run() {
            try {
                initUtil();
            } catch (Exception e) {
                CrashUtil.getSingleton().saveException(e);
                DeveloperLog.LogD("initUtil exception : ", e);
            }
            try {
                DataCache.getInstance().setMEM(KeyConstants.KEY_APP_KEY, appKey);
                if (TextUtils.isEmpty(appChannel)) {
                    appChannel = "";
                }
                DataCache.getInstance().setMEM(KeyConstants.KEY_APP_CHANNEL, appChannel);
                Activity activity = ActLifecycle.getInstance().getActivity();
                Error error = SdkUtil.banRun(activity, appKey);
                if (error != null) {
                    callbackInitErrorOnUIThread(error);
                    return;
                }
                //TODO 初始化调用
                requestConfig(activity, appKey);
            } catch (Exception e) {
                DeveloperLog.LogD("initOnAsyncThread  exception : ", e);
                CrashUtil.getSingleton().saveException(e);
                Error error = new Error(ErrorCode.CODE_INIT_UNKNOWN_INTERNAL_ERROR
                        , ErrorCode.MSG_INIT_UNKNOWN_INTERNAL_ERROR, ErrorCode.CODE_INTERNAL_UNKNOWN_OTHER);
                DeveloperLog.LogE(error.toString() + ", initOnAsyncThread");
                callbackInitErrorOnUIThread(error);
            }
        }
    }

    private static class InitFailRunnable implements Runnable {
        private Error mError;

        /**
         * Instantiates a new Init fail runnable.
         *
         * @param result the result
         */
        InitFailRunnable(Error result) {
            mError = result;
        }

        @Override
        public void run() {
            DeveloperLog.LogD("Om init error  " + mError);
            hasInit.set(false);
            isInitRunning.set(false);
            if (mCallback != null) {
                mCallback.onError(mError);
            }
            initCompleteReport(EventId.INIT_FAILED, mError);
        }
    }

    private static class InitRequestCallback implements Request.OnRequestCallback {

        private String appKey;
        private Activity mActivity;

        /**
         * Instantiates a new Init request callback.
         *
         * @param appKey the app key
         */
        InitRequestCallback(Activity activity, String appKey) {
            this.appKey = appKey;
            this.mActivity = activity;
        }

        @Override
        public void onRequestSuccess(Response response) {
            try {
                if (response.code() != HttpURLConnection.HTTP_OK) {
                    Error error = new Error(ErrorCode.CODE_INIT_SERVER_ERROR
                            , ErrorCode.MSG_INIT_SERVER_ERROR, ErrorCode.CODE_INTERNAL_SERVER_ERROR);
                    DeveloperLog.LogE(error.toString() + "Om init request config response code not 200 : " + response.code());
                    callbackInitErrorOnUIThread(error);
                    return;
                }

                String requestData = new String(ConfigurationHelper.checkResponse(response), Charset.forName(CommonConstants.CHARTSET_UTF8));
                if (TextUtils.isEmpty(requestData)) {
                    Error error = new Error(ErrorCode.CODE_INIT_SERVER_ERROR
                            , ErrorCode.MSG_INIT_SERVER_ERROR, ErrorCode.CODE_INTERNAL_SERVER_ERROR);
                    DeveloperLog.LogE(error.toString() + ", Om init response data is null: " + requestData);
                    callbackInitErrorOnUIThread(error);
                    return;
                }
                //adds global data to memory
                //TODO set返回值
                Configurations config = ConfigurationHelper.parseFormServerResponse(requestData);
                if (config != null) {
                    DeveloperLog.LogD("Om init request config success");
                    DataCache.getInstance().setMEM(KeyConstants.KEY_CONFIGURATION, config);
                    try {
                        AdTimingAuctionManager.getInstance().initBid(mActivity, config);
                    } catch (Exception e) {
                        DeveloperLog.LogD("initBid  exception : ", e);
                        CrashUtil.getSingleton().saveException(e);
                    }
                    callbackInitSuccessOnUIThread();
                    doAfterGetConfig(appKey, config);
                } else {
                    Error error = new Error(ErrorCode.CODE_INIT_SERVER_ERROR
                            , ErrorCode.MSG_INIT_SERVER_ERROR, ErrorCode.CODE_INTERNAL_SERVER_ERROR);
                    DeveloperLog.LogE(error.toString() + ", Om init format config is null");
                    callbackInitErrorOnUIThread(error);
                }
            } catch (Exception e) {
                CrashUtil.getSingleton().saveException(e);
                Error error = new Error(ErrorCode.CODE_INIT_SERVER_ERROR
                        , ErrorCode.MSG_INIT_SERVER_ERROR, ErrorCode.CODE_INTERNAL_UNKNOWN_OTHER);
                DeveloperLog.LogE(error.toString() + ", request config exception:" + e);
                callbackInitErrorOnUIThread(error);
            } finally {
                IOUtil.closeQuietly(response);
            }
        }

        @Override
        public void onRequestFailed(String error) {
            Error result = new Error(ErrorCode.CODE_INIT_SERVER_ERROR
                    , ErrorCode.MSG_INIT_SERVER_ERROR, ErrorCode.CODE_INTERNAL_SERVER_FAILED);
            DeveloperLog.LogD("request config failed : " + result + ", error:" + error);
            callbackInitErrorOnUIThread(result);
        }
    }
}
