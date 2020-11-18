// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package top.yfsz.yft;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import top.yfsz.yft.banner.AdSize;
import top.yfsz.yft.banner.BannerAd;
import top.yfsz.yft.banner.BannerAdListener;

import top.yfsz.yft.utils.NewApiUtils;
import top.yfsz.yft.interstitial.InterstitialAd;
import top.yfsz.yft.interstitial.InterstitialAdListener;
import top.yfsz.yft.nativead.AdIconView;
import top.yfsz.yft.nativead.AdInfo;
import top.yfsz.yft.nativead.MediaView;
import top.yfsz.yft.nativead.NativeAd;
import top.yfsz.yft.nativead.NativeAdListener;
import top.yfsz.yft.nativead.NativeAdView;
import top.yfsz.yft.utils.error.Error;
import top.yfsz.yft.utils.model.Scene;
import top.yfsz.yft.video.RewardedVideoAd;
import top.yfsz.yft.video.RewardedVideoListener;

import top.yfsz.yft.R;

public class MainActivity extends Activity {

    private Button rewardVideoButton;
    private Button interstitialButton;
    private Button bannerButton;
    private Button nativeButton;
    private Button splashButton;

    private LinearLayout adContainer;
    private View adView;
    private NativeAdView nativeAdView;

    private BannerAd bannerAd;
    private NativeAd nativeAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // zhe shi yingyong gang qidong de di fang  nsssssds
        NewApiUtils.ENABLE_LOG = true;
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorStatuBar));
        }
        rewardVideoButton = findViewById(R.id.btn_reward_video);
        interstitialButton = findViewById(R.id.btn_interstitial);
        splashButton = findViewById(R.id.btn_splash);
        bannerButton = findViewById(R.id.btn_banner);
        nativeButton = findViewById(R.id.btn_native);
        adContainer = findViewById(R.id.ad_container);
        //OmAds.setLogEnable(true);
        initSDK();
        if (RewardedVideoAd.isReady()) {
            setRewardVideoButtonStat(true);
        }
        if (InterstitialAd.isReady()) {
            //TODO
            setInterstitialButtonStat(true);
        }
    }

    private void initSDK() {
        NewApiUtils.printLog("start init sdk");
        OmAds.setLogEnable(true);
        OmAds.init(this, NewApiUtils.APPKEY, new InitCallback() {

            @Override
            public void onSuccess() {
                NewApiUtils.printLog("init success");
                setVideoListener();
                setInterstitialListener();
            }

            @Override
            public void onError(Error result) {
                NewApiUtils.printLog("init failed " + result.toString());
            }
        });
    }

    private void setVideoListener() {
        RewardedVideoAd.setAdListener(new RewardedVideoListener() {
            @Override
            public void onRewardedVideoAvailabilityChanged(boolean available) {
                if (available) {
                    setRewardVideoButtonStat(true);
                }
            }

            @Override
            public void onRewardedVideoAdShowed(Scene scene) {
                NewApiUtils.printLog("onRewardedVideoAdShowed " + scene);
            }

            @Override
            public void onRewardedVideoAdShowFailed(Scene scene, Error error) {
                NewApiUtils.printLog("onRewardedVideoAdShowFailed " + scene);
            }

            @Override
            public void onRewardedVideoAdClicked(Scene scene) {
                NewApiUtils.printLog("onRewardedVideoAdClicked " + scene);
            }

            @Override
            public void onRewardedVideoAdClosed(Scene scene) {
                NewApiUtils.printLog("onRewardedVideoAdClosed " + scene);
            }

            @Override
            public void onRewardedVideoAdStarted(Scene scene) {
                NewApiUtils.printLog("onRewardedVideoAdStarted " + scene);
            }

            @Override
            public void onRewardedVideoAdEnded(Scene scene) {
                NewApiUtils.printLog("onRewardedVideoAdEnded " + scene);
            }

            @Override
            public void onRewardedVideoAdRewarded(Scene scene) {
                NewApiUtils.printLog("onRewardedVideoAdRewarded " + scene);
            }
        });
    }


    private void setInterstitialListener() {
        InterstitialAd.setAdListener(new InterstitialAdListener() {
            @Override
            public void onInterstitialAdAvailabilityChanged(boolean available) {
                if (available) {
                    setInterstitialButtonStat(true);
                }
            }

            @Override
            public void onInterstitialAdShowed(Scene scene) {
                NewApiUtils.printLog("onInterstitialAdShowed " + scene);
            }

            @Override
            public void onInterstitialAdShowFailed(Scene scene, Error error) {
                NewApiUtils.printLog("onInterstitialAdShowFailed " + scene);
            }

            @Override
            public void onInterstitialAdClosed(Scene scene) {
                NewApiUtils.printLog("onInterstitialAdClosed " + scene);
            }

            @Override
            public void onInterstitialAdClicked(Scene scene) {
                NewApiUtils.printLog("onInterstitialAdClicked " + scene);
            }
        });
    }

    public void showRewardVideo(View view) {
        RewardedVideoAd.showAd();
        setRewardVideoButtonStat(false);
    }

    public void showInterstitial(View view) {
        InterstitialAd.showAd();
        setInterstitialButtonStat(false);
    }


    public void showSplash(View view) {
        startActivity(new Intent(MainActivity.this, SplashAdActivity.class));
    }

    public void loadAndShowBanner(View view) {
        adContainer.removeAllViews();
        bannerButton.setEnabled(false);
        bannerButton.setText("横幅广告加载...");
        if (bannerAd != null) {
            bannerAd.destroy();
        }
        bannerAd = new BannerAd(this, NewApiUtils.P_BANNER, new BannerAdListener() {
            @Override
            public void onAdReady(View view) {
                try {
                    if (null != view.getParent()) {
                        ((ViewGroup) view.getParent()).removeView(view);
                    }
                    adContainer.removeAllViews();
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                    adContainer.addView(view, layoutParams);
                } catch (Exception e) {
                    Log.e("AdtDebug", e.getLocalizedMessage());
                }
                bannerButton.setEnabled(true);
                bannerButton.setText("加载并显示横幅广告");
            }

            @Override
            public void onAdFailed(String error) {
                bannerButton.setEnabled(true);
                bannerButton.setText("横幅加载失败，请重试");

            }

            @Override
            public void onAdClicked() {

            }
        });
        bannerAd.setAdSize(AdSize.BANNER);
        bannerAd.loadAd();
    }

    public void loadAndShowNative(View view) {
        nativeButton.setEnabled(false);
        nativeButton.setText("本地广告加载...");
        if (nativeAd != null) {
            nativeAd.destroy();
        }
        adContainer.removeAllViews();
        nativeAd = new NativeAd(this, NewApiUtils.P_NATIVE, new NativeAdListener() {
            @Override
            public void onAdFailed(String msg) {
                nativeButton.setEnabled(true);
                nativeButton.setText("本地加载失败，请重试");
            }

            @Override
            public void onAdReady(AdInfo info) {
                adContainer.removeAllViews();
                adView = LayoutInflater.from(MainActivity.this).inflate(R.layout.native_ad_layout, null);


                TextView title = adView.findViewById(R.id.ad_title);
                title.setText(info.getTitle());

                TextView desc = adView.findViewById(R.id.ad_desc);
                desc.setText(info.getDesc());

                Button btn = adView.findViewById(R.id.ad_btn);
                btn.setText(info.getCallToActionText());


                MediaView mediaView = adView.findViewById(R.id.ad_media);

                nativeAdView = new NativeAdView(MainActivity.this);


                AdIconView adIconView = adView.findViewById(R.id.ad_icon_media);


                DisplayMetrics displayMetrics = MainActivity.this.getResources().getDisplayMetrics();
                mediaView.getLayoutParams().height = (int) (displayMetrics.widthPixels / (1200.0 / 627.0));

                nativeAdView.addView(adView);
                nativeAdView.setTitleView(title);
                nativeAdView.setDescView(desc);
                nativeAdView.setAdIconView(adIconView);
                nativeAdView.setCallToActionView(btn);
                nativeAdView.setMediaView(mediaView);


                nativeAd.registerNativeAdView(nativeAdView);
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                adContainer.addView(nativeAdView, layoutParams);
                nativeButton.setEnabled(true);
                nativeButton.setText("加载并显示本地广告");
            }

            @Override
            public void onAdClicked() {

            }
        });

        nativeAd.loadAd();

    }

    private void setRewardVideoButtonStat(boolean isEnable) {
        rewardVideoButton.setEnabled(isEnable);
        if (isEnable) {
            rewardVideoButton.setText("播放奖励视频广告");
        } else {
            rewardVideoButton.setText("奖励视频广告加载...");
        }
    }

    private void setInterstitialButtonStat(boolean isEnable) {
        interstitialButton.setEnabled(isEnable);
        if (isEnable) {
            interstitialButton.setText("播放插屏广告");
        } else {
            interstitialButton.setText("插屏广告加载...");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bannerAd != null) {
            bannerAd.destroy();
        }
        if (nativeAd != null) {
            nativeAd.destroy();
        }
    }
}
