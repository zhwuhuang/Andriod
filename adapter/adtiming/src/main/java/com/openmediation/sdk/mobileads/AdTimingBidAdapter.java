// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.openmediation.sdk.mobileads;

import android.app.Activity;
import android.content.Context;

import com.adtiming.mediationsdk.AdTimingAds;
import com.adtiming.mediationsdk.adt.bid.BidderTokenProvider;
import top.yfsz.yft.bid.BidAdapter;
import top.yfsz.yft.bid.BidCallback;
import top.yfsz.yft.bid.BidConstance;

import java.util.Map;

public class AdTimingBidAdapter extends BidAdapter {

    @Override
    public void initBid(Context context, Map<String, Object> dataMap, BidCallback callback) {
        super.initBid(context, dataMap, callback);
        if (!(context instanceof Activity)) {
            return;
        }
        if (dataMap != null && dataMap.containsKey(BidConstance.BID_APP_KEY)) {
            String appKey = String.valueOf(dataMap.get(BidConstance.BID_APP_KEY));
            AdTimingAds.init((Activity) context, appKey, null);
        }
    }

    @Override
    public String getBiddingToken(Context context) {
        return BidderTokenProvider.getBidderToken();
    }
}
