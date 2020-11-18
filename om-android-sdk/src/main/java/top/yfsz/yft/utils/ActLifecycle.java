// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Base64;

import top.yfsz.yft.utils.constant.CommonConstants;
import top.yfsz.yft.utils.crash.CrashUtil;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

public class ActLifecycle implements Application.ActivityLifecycleCallbacks {

    private static String[] ADS_ACT = new String[]{
            new String(Base64.decode("Y29tLmFkdGltaW5nLm1lZGlhdGlvbnNkaw==", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8)),
            new String(Base64.decode("Y29tLmdvb2dsZS5hbmRyb2lkLmdtcy5hZHM=", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8)),
            new String(Base64.decode("Y29tLmZhY2Vib29r", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8)),
            new String(Base64.decode("Y29tLnVuaXR5M2Q=", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8)),
            new String(Base64.decode("Y29tLnZ1bmdsZQ==", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8)),
            new String(Base64.decode("Y29tLmFkY29sb255", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8)),
            new String(Base64.decode("Y29tLmFwcGxvdmlu", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8)),
            new String(Base64.decode("Y29tLm1vcHVi", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8)),
            new String(Base64.decode("Y29tLnRhcGpveQ==", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8)),
            new String(Base64.decode("Y29tLmNoYXJ0Ym9vc3Q=", Base64.NO_WRAP),
                    Charset.forName(CommonConstants.CHARTSET_UTF8))
    };
    private AtomicReference<Activity> mThisActivity = new AtomicReference<>(null);

    private static final class DKLifecycleHolder {
        private static final ActLifecycle INSTANCE = new ActLifecycle();
    }

    private ActLifecycle() {
        try {
            AdtUtil.getApplication().registerActivityLifecycleCallbacks(this);
        } catch (Exception e) {
            CrashUtil.getSingleton().saveException(e);
        }
    }

    public static ActLifecycle getInstance() {
        return DKLifecycleHolder.INSTANCE;
    }

    public void init(Activity activity) {
        mThisActivity.set(activity);
    }

    public Activity getActivity() {
        return mThisActivity.get();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        mThisActivity.set(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        DeveloperLog.LogD("onActivityResumed: " + activity.toString());
        if (isAdActivity(activity)) {
            return;
        }
        Activity old = mThisActivity.get();
        if (old == null || old != activity) {
            mThisActivity.set(activity);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        DeveloperLog.LogD("onActivityDestroyed: " + activity.toString());
        Activity current = mThisActivity.get();
        if (current == activity) {
            mThisActivity.set(null);
        }
    }

    private boolean isAdActivity(Activity activity) {
        String address = activity.toString();
        for (String s : ADS_ACT) {
            if (address.contains(s)) {
                return true;
            }
        }
        return false;
    }
}
