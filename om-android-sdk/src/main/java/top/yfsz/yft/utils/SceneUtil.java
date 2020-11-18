// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils;

import android.text.TextUtils;

import top.yfsz.yft.utils.event.EventId;
import top.yfsz.yft.utils.event.EventUploadManager;
import top.yfsz.yft.utils.model.Placement;
import top.yfsz.yft.utils.model.Scene;

import org.json.JSONObject;

import java.util.Map;


/**
 *
 */
public class SceneUtil {
    public static Scene getScene(Placement placement, String sceneName) {
        if (placement == null || TextUtils.isEmpty(placement.getId())) {
            return null;
        }
        Scene sceneValue = null;
        Map<String, Scene> sceneMap = placement.getScenes();
        if (sceneMap != null) {
            if (TextUtils.isEmpty(sceneName)) {
                sceneValue = getDefaultScene(sceneMap);
            } else {
                sceneValue = sceneMap.get(sceneName);
                if (sceneValue == null) {
                    EventUploadManager.getInstance().uploadEvent(EventId.SCENE_NOT_FOUND,
                            sceneReport(placement.getId(), sceneValue));
                    sceneValue = getDefaultScene(sceneMap);
                }
            }
        }
        return sceneValue;
    }

    public static JSONObject sceneReport(String placementId, Scene scene) {
        JSONObject jsonObject = new JSONObject();
        JsonUtil.put(jsonObject, "pid", placementId);
        JsonUtil.put(jsonObject, "scene", scene != null ? scene.getId() : 0);
        return jsonObject;
    }

    private static Scene getDefaultScene(Map<String, Scene> sceneMap) {
        Scene sceneValue = null;
        for (Map.Entry<String, Scene> sceneEntry : sceneMap.entrySet()) {
            sceneValue = sceneEntry.getValue();
            if (sceneValue != null && 1 == sceneValue.getIsd()) {
                break;
            }
        }
        return sceneValue;
    }
}
