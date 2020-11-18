// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.bid;

import java.util.List;

public interface AuctionCallback {
    void onBidComplete(List<AdTimingBidResponse> c2sResponses, List<AdTimingBidResponse> s2sResponses);
}
