/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.util.custom;

import android.app.NotificationManager;
import android.app.ActivityManager;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import com.android.internal.R;
import com.android.internal.statusbar.IStatusBarService;

import java.util.List;
import java.util.Locale;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

public class Utils {

    private static OverlayManager mOverlayService;

    public static void killForegroundApp() {
        FireActions.killForegroundApp();
    }

    private static final class FireActions {
        private static IStatusBarService mStatusBarService = null;

        private static IStatusBarService getStatusBarService() {
            synchronized (FireActions.class) {
                if (mStatusBarService == null) {
                    mStatusBarService = IStatusBarService.Stub.asInterface(
                            ServiceManager.getService("statusbar"));
                }
                return mStatusBarService;
            }
        }

        public static void killForegroundApp() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.killForegroundApp();
                } catch (RemoteException e) {
                    // do nothing.
                }
            }
        }

        // Toggle notifications panel
        public static void toggleNotifications() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.togglePanel();
                } catch (RemoteException e) {}
            }
        }

        // Toggle qs panel
        public static void toggleQsPanel() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.expandSettingsPanel(null);
                } catch (RemoteException e) {}
            }
        }

        // Clear notifications
        public static void clearAllNotifications() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.onClearAllNotifications(ActivityManager.getCurrentUser());
                } catch (RemoteException e) {}
            }
        }
    }

    public static void clearAllNotifications() {
        FireActions.clearAllNotifications();
    }

    public static void toggleQsPanel() {
        FireActions.toggleQsPanel();
    }

    public static void toggleNotifications() {
        FireActions.toggleNotifications();
    }

    // Cycle ringer modes
    public static void toggleRingerModes (Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Vibrator mVibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                if (mVibrator.hasVibrator()) {
                    am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                }
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
        }
    }

    // Volume panel
    public static void toggleVolumePanel(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    // Method to detect whether an overlay is enabled or not
    public static boolean isThemeEnabled(String packageName) {
        mOverlayService = new OverlayManager();
        try {
            List<OverlayInfo> infos = mOverlayService.getOverlayInfosForTarget("android",
                    UserHandle.myUserId());
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (infos.get(i).packageName.equals(packageName)) {
                    return infos.get(i).isEnabled();
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class OverlayManager {
        private final IOverlayManager mService;

        public OverlayManager() {
            mService = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
        }

        public void setEnabled(String pkg, boolean enabled, int userId)
                throws RemoteException {
            mService.setEnabled(pkg, enabled, userId);
        }

        public List<OverlayInfo> getOverlayInfosForTarget(String target, int userId)
                throws RemoteException {
            return mService.getOverlayInfosForTarget(target, userId);
        }
    }
}
