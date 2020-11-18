// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils;

import android.util.Log;

public class NewApiUtils {

    public static final String TAG = "AdtDebug";
    public static boolean ENABLE_LOG = false;

    public static final String APPKEY = "rtMjfcX1IrmHTmeF26VIM7OBNhH0U9FV";

    //om
//    public static final String APPKEY = "OtnCjcU7ERE0D21GRoquiQBY6YXR3YLl";

    public static final String P_BANNER = "233";
    public static final String P_NATIVE = "235";

    public static void printLog(String msg) {
        if (ENABLE_LOG) {
            Log.e(TAG, msg);
        }
    }
}
