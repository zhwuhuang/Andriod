// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.bid;

public interface BidCallback {
    void bidSuccess(AdTimingBidResponse response);

    void bidFailed(String error);
}
