// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils.event;

import android.content.Context;
import android.text.TextUtils;

import top.yfsz.yft.utils.constant.CommonConstants;
import top.yfsz.yft.utils.model.Configurations;
import top.yfsz.yft.utils.request.network.Headers;
import top.yfsz.yft.utils.request.network.util.NetworkChecker;
import top.yfsz.yft.utils.AdtUtil;
import top.yfsz.yft.utils.DeveloperLog;
import top.yfsz.yft.utils.request.HeaderUtils;
import top.yfsz.yft.utils.request.RequestBuilder;
import top.yfsz.yft.utils.crash.CrashUtil;
import top.yfsz.yft.utils.model.Events;
import top.yfsz.yft.utils.request.network.AdRequest;
import top.yfsz.yft.utils.request.network.ByteRequestBody;
import top.yfsz.yft.utils.request.network.Request;
import top.yfsz.yft.utils.request.network.Response;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The type Event upload manager.
 */
public class EventUploadManager implements Request.OnRequestCallback {

    private AtomicBoolean isReporting = new AtomicBoolean(false);
    private AtomicInteger mMaxReportEventsCount = new AtomicInteger(5);
    private ConcurrentLinkedQueue<Event> mEvents;
    private ConcurrentLinkedQueue<Event> mDelayEvents;
    private DataBaseEventsStorage mEventDataBase;
    private List<Integer> mAllowedEvents;
    private ConcurrentLinkedQueue<Event> mReportEvents;
    private Events mEventsSettings;

    private EventUploadManager() {
    }

    private static class DataBaseUtilHolder {
        private static final EventUploadManager INSTANCE = new EventUploadManager();
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static EventUploadManager getInstance() {
        return EventUploadManager.DataBaseUtilHolder.INSTANCE;
    }

    /**
     * Needs to be called with getInstance().init() before other methods can be called
     *
     * @param context the context
     */
    public void init(final Context context) {
        mEvents = new ConcurrentLinkedQueue<>();
        mDelayEvents = new ConcurrentLinkedQueue<>();
        mReportEvents = new ConcurrentLinkedQueue<>();
        if (mEventDataBase == null) {
            mEventDataBase = DataBaseEventsStorage.getInstance(context, CommonConstants.DB_NAME, CommonConstants.DB_VERSION);
            mEventDataBase.createTable();
        }
        EventExecutor.execute(new LastStayEvent());
    }

    /**
     *
     */
    private void loadEvents() {
        if (mEvents == null) {
            mEvents = new ConcurrentLinkedQueue<>();
        }
        mEvents.addAll(mEventDataBase.loadEvents());
    }

    /**
     * Upload event.
     *
     * @param eventId the event id
     */
    public void uploadEvent(int eventId) {
        uploadEvent(eventId, null);
    }

    /**
     * Upload event.
     *
     * @param jsonObject the json object
     */
    public void uploadEvent(JSONObject jsonObject) {
        uploadEvent(0, jsonObject);
    }

    /**
     * Upload event.
     *
     * @param eventId    the event id
     * @param jsonObject the json object
     */
    public void uploadEvent(final int eventId, final JSONObject jsonObject) {
        EventExecutor.execute(new Runnable() {
            @Override
            public void run() {
                uploadEvent(buildEvent(eventId, jsonObject));
            }
        });
    }

    /**
     * @param event to report
     */
    private void uploadEvent(final Event event) {
        //
        if (mAllowedEvents == null) {//not yet got allowed list from server
            mDelayEvents.add(event);
            return;
        }

        //
        if (mAllowedEvents.isEmpty()) {//nothing is allowed to report
            mDelayEvents.clear();
            return;
        }

        //
        if (!mAllowedEvents.contains(event.getEid())) {//not in allowed list
            return;
        }
        if (mEvents == null) {
            mEvents = new ConcurrentLinkedQueue<>();
        }
        DeveloperLog.LogD("save event " + event.toString());
        mEvents.add(event);
        if (mEventDataBase != null) {
            mEventDataBase.addEvent(event);
        }
        if (mEvents.size() >= mMaxReportEventsCount.get() && NetworkChecker.isAvailable(AdtUtil.getApplication())) {
            DeveloperLog.LogD("update events by reached max events count");
            uploadEvents();
        }
    }

    /**
     * Update report settings.
     *
     * @param configurations the configurations
     */
    public synchronized void updateReportSettings(Configurations configurations) {
        mAllowedEvents = new ArrayList<>();
        if (configurations.getEvents() == null) {
            return;
        }
        mEventsSettings = configurations.getEvents();
        mMaxReportEventsCount.set(mEventsSettings.getMn());
        if (mEventsSettings.getIds() != null) {
            mAllowedEvents.addAll(mEventsSettings.getIds());
        }
        if (mEventsSettings.getCi() != 0) {
            EventExecutor.scheduleWithFixedDelay(new EventRunnable(), mEventsSettings.getCi(),
                    mEventsSettings.getCi(), TimeUnit.SECONDS);
        }
        if (!mAllowedEvents.isEmpty()) {
            uploadEventDelay();
        }
    }

    private void uploadEventDelay() {
        if (mDelayEvents != null && !mDelayEvents.isEmpty()) {
            for (Event event : mDelayEvents) {
                uploadEvent(event);
            }
            mDelayEvents.clear();
        }
    }

    private Event buildEvent(int eventId, JSONObject object) {
        Event event = new Event(object);
        if (eventId != 0) {
            event.setEid(eventId);
        }
        event.setTs(System.currentTimeMillis());
        return event;
    }

    /**
     *
     */
    private void uploadEvents() {
        try {
            if (mEventsSettings == null || mEvents == null || mEvents.isEmpty() || isReporting.get()) {
                return;
            }
            isReporting.set(true);
            Iterator<Event> iterator = mEvents.iterator();
            int i = 0;
            while (i < mMaxReportEventsCount.get()) {
                if (iterator.hasNext()) {
                    mReportEvents.add(iterator.next());
                }
                i++;
            }
            if (mReportEvents.isEmpty()) {
                return;
            }
            String url = RequestBuilder.buildEventUrl(mEventsSettings.getUrl());
            if (TextUtils.isEmpty(url)) {
                return;
            }
            byte[] body = RequestBuilder.buildEventRequestBody(mReportEvents);
            if (body == null) {
                DeveloperLog.LogD("build events request data error");
                return;
            }

            ByteRequestBody requestBody = new ByteRequestBody(body);
            Headers headers = HeaderUtils.getBaseHeaders();
            AdRequest.post().url(url).body(requestBody).headers(headers).connectTimeout(50000)
                    .readTimeout(100000)
                    .callback(this)
                    .performRequest(AdtUtil.getApplication());
            clearEvents();
        } catch (Exception e) {
            isReporting.set(false);
            DeveloperLog.LogD("update events exception : " + e.getMessage());
            CrashUtil.getSingleton().saveException(e);
        }
    }

    /**
     *
     */
    private void clearEvents() {
        if (mReportEvents.isEmpty()) {
            return;
        }
        if (mEvents != null) {
            mEvents.removeAll(mReportEvents);
        }
        if (mEventDataBase != null) {
            mEventDataBase.clearEvents(mReportEvents);
        }

        mReportEvents.clear();
    }

    @Override
    public void onRequestSuccess(Response response) {
        isReporting.set(false);
        if (mEvents.size() >= mMaxReportEventsCount.get() && NetworkChecker.isAvailable(AdtUtil.getApplication())) {
            DeveloperLog.LogD("update events after upload success");
            uploadEvents();
        }
    }

    @Override
    public void onRequestFailed(String error) {
        //
        DeveloperLog.LogD("uploadEvent error : " + error);
        //
        isReporting.set(false);
    }

    private class EventRunnable implements Runnable {
        @Override
        public void run() {
            if (!mEvents.isEmpty()) {
                DeveloperLog.LogD("update events by reached interval");
                uploadEvents();
            }
        }
    }

    private class LastStayEvent implements Runnable {

        @Override
        public void run() {
            if (mEventDataBase != null) {
                loadEvents();
            }
        }
    }
}
