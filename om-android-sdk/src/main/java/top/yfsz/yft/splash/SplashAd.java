package top.yfsz.yft.splash;


import android.view.ViewGroup;

import top.yfsz.yft.core.imp.splash.SplashAdManager;

public class SplashAd {

    /**
     * load timeout
     * @param timeout ms
     */
    public static void setLoadTimeout(long timeout) {
        SplashAdManager.getInstance().setLoadTimeout(timeout);
    }

    public static void loadAd() {
        SplashAdManager.getInstance().load();
    }

    public static void setSize(int width, int height) {
        SplashAdManager.getInstance().setSize(width, height);
    }

    public static void setSplashAdListener(SplashAdListener listener) {
        SplashAdManager.getInstance().setSplashAdListener(listener);
    }

    public static boolean isReady() {
        return SplashAdManager.getInstance().isReady();
    }

    public static void showAd(ViewGroup container) {
        SplashAdManager.getInstance().show(container);
    }
}
