package top.yfsz.yft.core.runnable;

import top.yfsz.yft.core.AbstractAdsManager;
import top.yfsz.yft.utils.DeveloperLog;
import top.yfsz.yft.utils.WorkExecutor;
import top.yfsz.yft.utils.crash.CrashUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AdsScheduleTask implements Runnable {
    private AbstractAdsManager adsManager;
    private int delay = 0;

    public AdsScheduleTask(AbstractAdsManager manager) {
        adsManager = manager;
    }

    @Override
    public void run() {
        try {
            if (adsManager == null) {
                return;
            }
            adsManager.loadAdWithInterval();
            int count = adsManager.getAllLoadFailedCount();
            Map<Integer, Integer> rfs = adsManager.getRfs();
            if (rfs == null) {
                return;
            }
            Set<Integer> keys = rfs.keySet();
            for (Integer key : keys) {
                if (count < key) {
                    delay = rfs.get(key);
                    break;
                }
            }
            DeveloperLog.LogD("execute adsScheduleTask delay : " + delay);
            WorkExecutor.execute(this, delay, TimeUnit.SECONDS);
        } catch (Exception e) {
            CrashUtil.getSingleton().saveException(e);
        }
    }
}
