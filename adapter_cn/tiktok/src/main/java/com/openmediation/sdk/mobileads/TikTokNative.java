// Copyright 2020 ADTIMING TECHNOLOGY COMPANY LIMITED
// Licensed under the GNU Lesser General Public License Version 3

package com.openmediation.sdk.mobileads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.bytedance.sdk.openadsdk.multipro.b;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import top.yfsz.yft.mediation.AdapterErrorBuilder;
import top.yfsz.yft.mediation.CustomNativeEvent;
import top.yfsz.yft.mediation.MediationInfo;
import top.yfsz.yft.nativead.AdIconView;
import top.yfsz.yft.nativead.MediaView;
import top.yfsz.yft.nativead.NativeAdView;


public class TikTokNative extends CustomNativeEvent implements TTAdNative.NativeExpressAdListener  {
    private static String TAG = "OM-TikTok: ";
    private AtomicBoolean mDidCallInit = new AtomicBoolean(false);
    private TTAdNative mTTAdNative;
    private TTNativeAd ttNativeAd;
    private TTNativeExpressAd mTTAd;
    private View mBannerView;
    private Activity mActivity;
    private NativeAdView mListView;
    private View viewww;

    private MediaView mediaView;

    private AdIconView adIconView;

    private FrameLayout mExpressContainer;

    @Override
    public void registerNativeView(NativeAdView adView) {
        List<View> views = new ArrayList<>();
        if (adView.getMediaView() != null) {
            mediaView = adView.getMediaView();
            views.add(mediaView);
        }
        if (adView.getAdIconView() != null) {
            adIconView = adView.getAdIconView();
            views.add(adIconView);
        }
        if (adView.getTitleView() != null) {
            views.add(adView.getTitleView());
        }
        if (adView.getDescView() != null) {
            views.add(adView.getDescView());
        }
        if (adView.getCallToActionView() != null) {
            views.add(adView.getCallToActionView());
        }
        System.out.println("请求成功===============");
        adView.addView(viewww);
        //this.mListView = adView;
    }



    @Override
    public void loadAd(Activity activity, Map<String, String> config) throws Throwable {
        System.out.println("loadAd请求");
        super.loadAd(activity, config);
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (!check(activity, config)) {
            return;
        }
        this.mActivity = activity;
        initTTSDKConfig(activity, config);
        //int[] size = getAdSize(activity, config);
        //int width = size[0], height = size[1];
        loadNativeAd(mInstancesKey);
    }

    @Override
    public int getMediation() {
        return MediationInfo.MEDIATION_ID_13;
    }

    private void loadNativeAd(String codeId) {
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(codeId)
                .setAdCount(1)
                .build();
        mTTAdNative.loadNativeExpressAd(adSlot, new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                System.out.println(message);
            }
            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0){
                    return;
                }
                System.out.println("成功");
                onInsClicked();
                mTTAd = ads.get(0);
                bindAdListener(mTTAd);
                //这边出错了
                mAdInfo.setDesc(ttNativeAd.getDescription());
                mAdInfo.setType(getMediation());
                mAdInfo.setCallToActionText(ttNativeAd.getButtonText());
                mAdInfo.setTitle(ttNativeAd.getTitle());
                onInsReady(mAdInfo);
                if (mListView != null) {
                    mListView.removeAllViews();
                    mListView.addView(mTTAd.getExpressAdView());
                }
                mTTAd.render();
            }
        });
    }
    @Override
    public void destroy(Activity activity) {
        isDestroyed = true;
        if (mTTAd != null) {
            mTTAd.destroy();
        }
    }



    private void initTTSDKConfig(Activity activity, Map<String, String> config) {
        TTAdManagerHolder.init(activity.getApplication(), config.get("AppKey"));
        if (mTTAdNative == null) {
            mTTAdNative = TTAdManagerHolder.get().createAdNative(activity);
        }
    }


    @Override
    public void onError(int i, String s) {
        if (isDestroyed) {
            return;
        }
        onInsError(AdapterErrorBuilder.buildLoadError(
                AdapterErrorBuilder.AD_UNIT_BANNER, mAdapterName, i, s));
    }

    @Override
    public void onNativeExpressAdLoad(List<TTNativeExpressAd> list) {

    }

    private static class InnerAdInteractionListener implements TTNativeExpressAd.ExpressAdInteractionListener {

        private WeakReference<TikTokNative> mReference;

        private InnerAdInteractionListener(TikTokNative banner) {
            mReference = new WeakReference<>(banner);
        }
        @Override
        public void onAdClicked(View view, int type) {
            if (mReference == null || mReference.get() == null) {
                return;
            }
            TikTokNative banner = mReference.get();
            if (banner.isDestroyed) {
                return;
            }

        }

        @Override
        public void onAdShow(View view, int type) {
        }

        @Override
        public void onRenderFail(View view, String msg, int code) {
            TikTokNative banner = mReference.get();
            if (banner.isDestroyed) {
                return;
            }
            banner.onInsError(AdapterErrorBuilder.buildLoadError(
                    AdapterErrorBuilder.AD_UNIT_BANNER, "TikTokBanner", code, msg));
        }

        @Override
        public void onRenderSuccess(View view, float width, float height) {
            if (mReference == null || mReference.get() == null) {
                return;
            }
            TikTokNative banner = mReference.get();
            if (banner.isDestroyed) {
                return;
            }
            banner.mBannerView = view;
            banner.onInsReady(view);
        }
    }
   /* private void bindDislike(Activity activity, TTNativeExpressAd ad) {
        if (activity == null || ad == null) {
            return;
        }
        ad.setDislikeCallback(activity, new TTAdDislike.DislikeInteractionCallback() {
            @Override
            public void onSelected(int position, String value) {
                if (mBannerView != null && mBannerView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) mBannerView.getParent()).removeView(mBannerView);
                    mBannerView = null;
                }
            }

            @Override
            public void onCancel() {
            }
        });
    }*/

   private void bindAdListener(TTNativeExpressAd ad) {
       System.out.println("view");
       ad.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
           @Override
           public void onAdClicked(View view, int type) {
               //TToast.show(mContext, "广告被点击");
           }
           @Override
           public void onAdShow(View view, int type) {
               if (mediaView != null) {
                   mediaView.removeAllViews();
                   mediaView.addView(view);
               }
               //TToast.show(mContext, "广告展示");
           }
           @Override
           public void onRenderFail(View view, String msg, int code) {
               //Log.e("ExpressView","render fail:"+(System.currentTimeMillis() - startTime));
               //TToast.show(mContext, msg+" code:"+code);
           }

           @Override
           public void onRenderSuccess(View view, float width, float height) {
               System.out.println("view");
               //Log.e("ExpressView","render suc:"+(System.currentTimeMillis() - startTime));
               //返回view的宽高 单位 dp
               //TToast.show(mContext, "渲染成功");
               //mExpressContainer.removeAllViews();
               //mExpressContainer.addView(view);
               viewww = view;
               if (mediaView != null) {
                   mediaView.removeAllViews();
                   mediaView.addView(view);
               }
           }
       });
//       //dislike设置
//       bindDislike(ad, false);
//       if (ad.getInteractionType() != TTAdConstant.INTERACTION_TYPE_DOWNLOAD){
//           return;
//       }
       ad.setDownloadListener(new TTAppDownloadListener() {
           @Override
           public void onIdle() {
               //TToast.show(NativeExpressActivity.this, "点击开始下载", Toast.LENGTH_LONG);
           }

           @Override
           public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {

           }

           @Override
           public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
               //TToast.show(NativeExpressActivity.this, "下载暂停，点击继续", Toast.LENGTH_LONG);
           }

           @Override
           public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
               //TToast.show(NativeExpressActivity.this, "下载失败，点击重新下载", Toast.LENGTH_LONG);
           }

           @Override
           public void onInstalled(String fileName, String appName) {
               //TToast.show(NativeExpressActivity.this, "安装完成，点击图片打开", Toast.LENGTH_LONG);
           }

           @Override
           public void onDownloadFinished(long totalBytes, String fileName, String appName) {
               //TToast.show(NativeExpressActivity.this, "点击安装", Toast.LENGTH_LONG);
           }
       });
   }
}
