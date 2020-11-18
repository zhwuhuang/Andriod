// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils;

import android.net.Uri;
import android.text.TextUtils;

import top.yfsz.yft.utils.cache.DataCache;
import top.yfsz.yft.utils.constant.CommonConstants;
import top.yfsz.yft.utils.constant.KeyConstants;
import top.yfsz.yft.utils.crash.CrashUtil;
import top.yfsz.yft.utils.model.BaseInstance;
import top.yfsz.yft.utils.model.Configurations;
import top.yfsz.yft.utils.model.ImpRecord;
import top.yfsz.yft.utils.model.Placement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The type Placement utils.
 */
public class PlacementUtils {

    /**
     * Gets placement info.
     *
     * @param placementId the placement id
     * @param instances   the instances
     * @param payload     the payload
     * @return the placement info
     */
    public static Map<String, String> getPlacementInfo(String placementId, BaseInstance instances, String payload) {
        Configurations config = DataCache.getInstance().getFromMem(KeyConstants.KEY_CONFIGURATION, Configurations.class);
        Map<String, String> maps = new HashMap<>();
        maps.put("AppKey", config.getMs().get(instances.getMediationId()).getK());
        maps.put("PlacementId", placementId);
        maps.put("InstanceKey", instances.getKey());
        maps.put("InstanceId", String.valueOf(instances.getId()));
        if (!TextUtils.isEmpty(payload)) {
            maps.put("pay_load", payload);
        }
        return maps;
    }

    /**
     * Gets 1st Placement of the adType in config
     *
     * @param adType the ad type
     * @return placement
     */
    public static Placement getPlacement(int adType) {
        Configurations config = DataCache.getInstance().getFromMem(KeyConstants.KEY_CONFIGURATION, Configurations.class);
        if (config == null) {
            return null;
        }

        Set<String> keys = config.getPls().keySet();
        for (String key : keys) {
            Placement placement = config.getPls().get(key);
            if (placement != null && placement.getT() == adType && placement.getMain() == 1) {
                return placement;
            }
        }
        return null;
    }

    /**
     * Gets the 1st Placement if PlacementId is null
     *
     * @param placementId the placement id
     * @return placement
     */
    public static Placement getPlacement(String placementId) {
        Configurations config = DataCache.getInstance().getFromMem(KeyConstants.KEY_CONFIGURATION, Configurations.class);
        if (config == null) {
            return null;
        }
        return config.getPls().get(placementId);
    }


    /**
     * Placement event params json object.
     *
     * @param placementId the placement id
     * @return the json object
     */
    public static JSONObject placementEventParams(String placementId) {
        JSONObject jsonObject = new JSONObject();
        JsonUtil.put(jsonObject, "pid", placementId);
        return jsonObject;
    }

    private static List<ImpRecord.DayImp> jsonToDayImps(JSONArray array) {
        if (array == null || array.length() == 0) {
            return null;
        }
        List<ImpRecord.DayImp> dayImps = new ArrayList<>();
        int len = array.length();
        for (int i = 0; i < len; i++) {
            JSONObject object = array.optJSONObject(i);
            ImpRecord.DayImp dayImp = new ImpRecord.DayImp();
            dayImp.setImpCount(object.optInt("imp_count"));
            dayImp.setTime(object.optString("time"));
            dayImps.add(dayImp);
        }
        return dayImps;
    }

    /**
     * Save placement impr count.
     *
     * @param placementId the placement id
     */
    public static void savePlacementImprCount(String placementId) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
            Date date = new Date();

            String today = dateFormat.format(date);

            ImpRecord impRecord = parseDayImpFromJson(DataCache.getInstance().get("DayImpRecord", String.class));
            if (impRecord == null) {
                impRecord = new ImpRecord();
            }

            Map<String, List<ImpRecord.DayImp>> impsMap = impRecord.getDayImp();
            if (impsMap == null) {
                impsMap = new HashMap<>();
            }

            String tpmKey = placementId.trim().concat("day_impr");

            List<ImpRecord.DayImp> imps = impsMap.get(tpmKey);

            if (imps != null && !imps.isEmpty()) {
                ImpRecord.DayImp imp = imps.get(0);
                if (imp == null) {
                    imp = new ImpRecord.Imp();
                    imp.setTime(today);
                    imp.setImpCount(1);
                } else {
                    if (today.equals(imp.getTime())) {
                        imp.setImpCount(imp.getImpCount() + 1);
                    } else {
                        imp.setTime(today);
                        imp.setImpCount(1);
                    }
                    imps.clear();
                }
                imps.add(imp);
            } else {
                imps = new ArrayList<>();
                ImpRecord.Imp imp = new ImpRecord.Imp();
                imp.setTime(today);
                imp.setImpCount(1);
                imps.add(imp);
            }

            impsMap.put(tpmKey, imps);
            impRecord.setDayImp(impsMap);

            DataCache.getInstance().set("DayImpRecord", Uri.encode(transDayImpToString(impRecord)));
        } catch (Throwable e) {
            DeveloperLog.LogD("PlacementUtils", e);
            CrashUtil.getSingleton().saveException(e);
        }
    }

    /**
     * Gets placement impr count.
     *
     * @param placementId the placement id
     * @return the placement impr count
     */
    public static int getPlacementImprCount(String placementId) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
            Date date = new Date();

            String today = dateFormat.format(date);

            ImpRecord impRecord = parseDayImpFromJson(DataCache.getInstance().get("DayImpRecord", String.class));
            if (impRecord == null) {
                return 0;
            }

            Map<String, List<ImpRecord.DayImp>> impsMap = impRecord.getDayImp();
            if (impsMap == null) {
                return 0;
            }

            String tpmKey = placementId.trim().concat("day_impr");

            List<ImpRecord.DayImp> imps = impsMap.get(tpmKey);

            if (imps != null && !imps.isEmpty()) {
                ImpRecord.DayImp imp = imps.get(0);
                if (imp != null && imp.getTime().equals(today)) {
                    return imp.getImpCount();
                }
            }

            return 0;
        } catch (Throwable e) {
            DeveloperLog.LogD("PlacementUtils", e);
            CrashUtil.getSingleton().saveException(e);
        }
        return 0;
    }

    private static ImpRecord parseDayImpFromJson(String s) {
        if (TextUtils.isEmpty(s)) {
            return null;
        }
        ImpRecord impRecord = new ImpRecord();
        try {
            JSONObject object = new JSONObject(Uri.decode(s));
            Map<String, List<ImpRecord.DayImp>> dayImpMap = new HashMap<>();
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                List<ImpRecord.DayImp> imps = jsonToDayImps(object.optJSONArray(key));
                if (imps != null && !imps.isEmpty()) {
                    dayImpMap.put(key, imps);
                }
            }
            impRecord.setDayImp(dayImpMap);
            return impRecord;
        } catch (JSONException e) {
            DeveloperLog.LogD("PlacementUtils", e);
            CrashUtil.getSingleton().saveException(e);
            return null;
        }
    }

    private static String transDayImpToString(ImpRecord impRecord) {
        try {
            JSONObject object = new JSONObject();
            Map<String, List<ImpRecord.DayImp>> impMap = impRecord.getDayImp();
            Set<String> keys = impMap.keySet();
            for (String key : keys) {
                List<ImpRecord.DayImp> dayImps = impMap.get(key);
                if (dayImps == null || dayImps.isEmpty()) {
                    continue;
                }
                JSONArray array = new JSONArray();
                for (ImpRecord.DayImp imp : dayImps) {
                    if (imp == null) {
                        continue;
                    }
                    JSONObject o = new JSONObject();
                    o.put("imp_count", imp.getImpCount());
                    o.put("time", imp.getTime());

                    array.put(o);
                }
                object.put(key, array);
            }
            return object.toString();
        } catch (Exception e) {
            DeveloperLog.LogD("PlacementUtils", e);
            CrashUtil.getSingleton().saveException(e);
            return null;
        }
    }

    public static boolean isCacheAdsType(int adType) {
        switch (adType) {
            case CommonConstants.BANNER:
            case CommonConstants.NATIVE:
                return false;
            default:
                return true;
        }
    }
}
