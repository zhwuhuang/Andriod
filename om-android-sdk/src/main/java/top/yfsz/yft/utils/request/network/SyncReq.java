// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.utils.request.network;

import top.yfsz.yft.utils.DeveloperLog;

class SyncReq extends BaseTask{

     SyncReq(Request request) {
        super(request);
    }

     Response start() {
        try {
            if (mConnection == null) {
                return null;
            }
            return mConnection.intercept(mRequest);
        } catch (Exception e) {
            DeveloperLog.LogD("SyncReq", e);
            return null;
        }
    }
}
