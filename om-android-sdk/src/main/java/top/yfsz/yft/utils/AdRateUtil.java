// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils;

import android.text.TextUtils;

import top.yfsz.yft.utils.event.EventId;
import top.yfsz.yft.utils.event.EventUploadManager;
import top.yfsz.yft.utils.model.BaseInstance;
import top.yfsz.yft.utils.model.Placement;
import top.yfsz.yft.utils.model.Scene;
import top.yfsz.yft.utils.cache.DataCache;

/**
 * Ads impression rate control
 */
public class AdRateUtil {
    private static String RATE = "Rate";
    private static String CAP = "CAP";
    private static String CAP_TIME = "CAPTime";

    /**
     * saves placement's and instance's impression time and count
     *
     * @param placementId  the placement id
     * @param instancesKey the instances key
     */
    public static void onInstancesShowed(String placementId, String instancesKey) {
        AdRateUtil.saveShowTime(placementId);
        AdRateUtil.addCAP(placementId);

        AdRateUtil.saveShowTime(placementId + instancesKey);
        AdRateUtil.addCAP(placementId + instancesKey);
        //
        PlacementUtils.savePlacementImprCount(placementId);
    }

    /**
     * On scene showed.
     *
     * @param placementId the placement id
     * @param scene       the scene
     */
    public static void onSceneShowed(String placementId, Scene scene) {
        if (scene != null) {
            AdRateUtil.saveShowTime(placementId + scene.getN());
            AdRateUtil.addCAP(placementId + scene.getN());
        }
    }

    /**
     * @param key
     */
    private static void saveShowTime(String key) {
        DataCache.getInstance().set(RATE + key, System.currentTimeMillis());
    }

    /**
     * Should block placement boolean.
     *
     * @param placement the placement
     * @return the boolean
     */
    public static boolean shouldBlockPlacement(Placement placement) {
        if (placement == null || TextUtils.isEmpty(placement.getId())) {
            return false;
        }
        boolean result = AdRateUtil.isInterval(placement.getId(), placement.getFrequencyInterval());
        if (result) {
            EventUploadManager.getInstance().uploadEvent(EventId.PLACEMENT_CAPPED);
        }
        return result;
    }

    /**
     * Is placement capped boolean.
     *
     * @param placement the placement
     * @return the boolean
     */
    public static boolean isPlacementCapped(Placement placement) {
        if (placement == null || TextUtils.isEmpty(placement.getId())) {
            return false;
        }
        boolean result = AdRateUtil.isCAP(placement.getId(), placement.getFrequencyUnit(), placement.getFrequencyCap());

        if (result) {
            EventUploadManager.getInstance().uploadEvent(EventId.PLACEMENT_CAPPED);
        }
        return result;
    }

    /**
     * Should block instance boolean.
     *
     * @param key      the key
     * @param instance the instance
     * @return the boolean
     */
    public static boolean shouldBlockInstance(String key, BaseInstance instance) {
        if (TextUtils.isEmpty(key)) {
            return false;
        }
        boolean result = AdRateUtil.isInterval(key, instance.getFrequencyInterval())
                || AdRateUtil.isCAP(key, instance.getFrequencyUnit(), instance.getFrequencyCap());
        if (result) {
            EventUploadManager.getInstance().uploadEvent(EventId.INSTANCE_CAPPED, instance.buildReportData());
        }
        return result;
    }

    /**
     * Should block scene boolean.
     *
     * @param placementId the placement id
     * @param scene       the scene
     * @return the boolean
     */
    public static boolean shouldBlockScene(String placementId, Scene scene) {
        boolean result = scene != null && AdRateUtil.isCAP(placementId + scene.getN(), scene.getFrequencyUnit(),
                scene.getFrequencyCap());
        if (result) {
            EventUploadManager.getInstance().uploadEvent(EventId.SCENE_CAPPED, SceneUtil.sceneReport(placementId, scene));
        }
        return result;
    }

    /**
     * is Interval long enough
     *
     * @param key
     * @param rate
     * @return
     */
    private static boolean isInterval(String key, long rate) {
        if (rate == 0) {
            return false;
        }
        Long beforeTime = DataCache.getInstance().get(RATE + key, long.class);
        if (beforeTime == null) {
            return false;
        }
        DeveloperLog.LogD("Interval:" + key + ":" + (System.currentTimeMillis() - beforeTime) + ":" + rate);
        return System.currentTimeMillis() - beforeTime < rate;
    }

    /**
     * saves current Placement/Mediation impression count and 1st impression time
     *
     * @param key
     */
    private static void addCAP(String key) {
        Integer cap = DataCache.getInstance().get(CAP + key, int.class);
        if (cap == null) {
            cap = 0;
        }
        DeveloperLog.LogD("AddCAP:" + key + ":" + cap + 1);
        DataCache.getInstance().set(CAP + key, cap + 1);
        Long capTime = DataCache.getInstance().get(CAP_TIME + key, long.class);
        if (capTime == null) {
            DataCache.getInstance().set(CAP_TIME + key, System.currentTimeMillis());
        }
    }

    /**
     * checks how many impressions exist between current Placement's 1st impression time and now
     *
     * @param key
     * @param time  hours
     * @param count
     * @return
     */
    private static boolean isCAP(String key, int time, int count) {
        if (count <= 0) {
            return false;
        }
        Long capTime = DataCache.getInstance().get(CAP_TIME + key, long.class);
        if (capTime == null) {
            return false;
        }
        Integer cap = DataCache.getInstance().get(CAP + key, int.class);
        DeveloperLog.LogD("CapTime:" + key + ":" + (System.currentTimeMillis() - capTime) +
                ":" + time + ":Cap:" + cap + ":" + count);
        if (System.currentTimeMillis() - capTime < time) {
            return cap >= count;
        } else {
            DataCache.getInstance().delete(CAP_TIME + key);
            DataCache.getInstance().delete(CAP + key);
            return false;
        }
    }
}
