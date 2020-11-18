// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.core.imp.rewardedvideo;

import top.yfsz.yft.utils.error.Error;

public interface RvManagerListener {
    void onRewardedVideoInitSuccess(RvInstance rvInstance);

    void onRewardedVideoInitFailed(Error error, RvInstance rvInstance);

    void onRewardedVideoAdShowFailed(Error error, RvInstance rvInstance);

    void onRewardedVideoAdShowSuccess(RvInstance rvInstance);

    void onRewardedVideoAdClosed(RvInstance rvInstance);

    void onRewardedVideoLoadSuccess(RvInstance rvInstance);

    void onRewardedVideoLoadFailed(Error error, RvInstance rvInstance);

    void onRewardedVideoAdStarted(RvInstance rvInstance);

    void onRewardedVideoAdEnded(RvInstance rvInstance);

    void onRewardedVideoAdRewarded(RvInstance rvInstance);

    void onRewardedVideoAdClicked(RvInstance rvInstance);
}
