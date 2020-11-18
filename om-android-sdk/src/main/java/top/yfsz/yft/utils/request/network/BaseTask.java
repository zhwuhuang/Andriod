// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils.request.network;

import top.yfsz.yft.utils.request.network.connect.AbstractUrlConnection;
import top.yfsz.yft.utils.request.network.connect.HttpConnection;
import top.yfsz.yft.utils.request.network.connect.HttpsConnection;
import top.yfsz.yft.utils.DeveloperLog;
import top.yfsz.yft.utils.crash.CrashUtil;

import java.net.URL;

abstract class BaseTask {
    protected Request mRequest;
    protected AbstractUrlConnection mConnection;

    BaseTask(Request request) {
        try {
            mRequest = request;
            String protocol = new URL(request.getUrl()).getProtocol();
            if ("http".equalsIgnoreCase(protocol)) {
                mConnection = new HttpConnection();
            } else if ("https".equalsIgnoreCase(protocol)) {
                mConnection = new HttpsConnection();
            }
        } catch (Exception e) {
            DeveloperLog.LogD("BaseTask", e);
            CrashUtil.getSingleton().saveException(e);
        }
    }
}
