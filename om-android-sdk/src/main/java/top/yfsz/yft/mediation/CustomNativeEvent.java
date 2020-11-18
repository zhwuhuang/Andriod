// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft.mediation;

import top.yfsz.yft.nativead.AdInfo;
import top.yfsz.yft.nativead.NativeAdView;

public abstract class CustomNativeEvent extends CustomAdEvent {
    protected AdInfo mAdInfo = new AdInfo();

    public abstract void registerNativeView(NativeAdView adView);
}
