package com.timefix.xposed;

import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;

public class Main implements IXposedHookLoadPackage {

    private static final String PACKAGE_NAME = "com.example";
    private static final String PREFS_NAME = "autotimefix_prefs";

    @Override
    public void handleLoadPackage(final de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Self-activation check hook
        if (lpparam.packageName.equals(PACKAGE_NAME)) {
            XposedHelpers.findAndHookMethod(
                    "com.example.XposedChecker",
                    lpparam.classLoader,
                    "isModuleActive",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(true);
                        }
                    }
            );
            return;
        }

        // Hook only the system server (android package)
        if (!lpparam.packageName.equals("android")) {
            return;
        }

        XposedBridge.log("AutoTimeFix: Loaded system package, hooking systemReady");

        XposedHelpers.findAndHookMethod(
                "com.android.server.am.ActivityManagerService",
                lpparam.classLoader,
                "systemReady",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("AutoTimeFix: systemReady hooked successfully! Starting background time sync thread...");

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Load preferences dynamically
                                    XSharedPreferences prefs = new XSharedPreferences(PACKAGE_NAME, PREFS_NAME);
                                    prefs.makeWorldReadable();
                                    prefs.reload();

                                    // Read configuration with default values
                                    int bootDelaySec = 30;
                                    try {
                                        bootDelaySec = Integer.parseInt(prefs.getString("boot_delay", "30"));
                                    } catch (Exception e) {
                                        XposedBridge.log("AutoTimeFix: Error parsing boot_delay, using default (30s)");
                                    }

                                    int netTimeoutSec = 30;
                                    try {
                                        netTimeoutSec = Integer.parseInt(prefs.getString("net_timeout", "150")); // Up to 150s (30 * 5s)
                                    } catch (Exception e) {
                                        XposedBridge.log("AutoTimeFix: Error parsing net_timeout, using default (150s)");
                                    }

                                    int retryCount = 10;
                                    try {
                                        retryCount = Integer.parseInt(prefs.getString("retry_count", "10"));
                                    } catch (Exception e) {
                                        XposedBridge.log("AutoTimeFix: Error parsing retry_count, using default (10)");
                                    }

                                    String ntpServer = prefs.getString("ntp_server", "time.google.com");
                                    if (ntpServer == null || ntpServer.trim().isEmpty()) {
                                        ntpServer = "time.google.com";
                                    }

                                    XposedBridge.log("AutoTimeFix: Config read: bootDelay=" + bootDelaySec 
                                            + "s, netTimeout=" + netTimeoutSec 
                                            + "s, retryCount=" + retryCount 
                                            + ", ntpServer=" + ntpServer);

                                    // 1. Wait for boot to stabilize
                                    XposedBridge.log("AutoTimeFix: Waiting " + bootDelaySec + " seconds for system boot to stabilize...");
                                    Thread.sleep(bootDelaySec * 1000L);

                                    // 2. Wait for internet connection
                                    int checkIntervalSec = 5;
                                    int maxNetworkChecks = netTimeoutSec / checkIntervalSec;
                                    if (maxNetworkChecks <= 0) maxNetworkChecks = 30;

                                    XposedBridge.log("AutoTimeFix: Checking internet connection (timeout: " + netTimeoutSec + "s)...");
                                    boolean connected = false;
                                    for (int i = 0; i < maxNetworkChecks; i++) {
                                        if (isInternetAvailable()) {
                                            XposedBridge.log("AutoTimeFix: Internet is available!");
                                            connected = true;
                                            break;
                                        }
                                        XposedBridge.log("AutoTimeFix: Internet not available yet, retrying in " + checkIntervalSec + "s... (" + (i + 1) + "/" + maxNetworkChecks + ")");
                                        Thread.sleep(checkIntervalSec * 1000L);
                                    }

                                    if (!connected) {
                                        XposedBridge.log("AutoTimeFix: Internet check timed out. Proceeding to force sync anyway in case ping failed...");
                                    }

                                    // 3. Force Sync Time
                                    XposedBridge.log("AutoTimeFix: Starting NTP time sync to " + ntpServer + " (Max retries: " + retryCount + ")...");
                                    for (int i = 0; i < retryCount; i++) {
                                        XposedBridge.log("AutoTimeFix: Executing time sync command (Attempt " + (i + 1) + "/" + retryCount + ")...");
                                        try {
                                            // Execute with root shell
                                            Process p = Runtime.getRuntime().exec(new String[]{
                                                    "su", "-c", "busybox ntpd -n -q -p " + ntpServer
                                            });
                                            int exitCode = p.waitFor();
                                            XposedBridge.log("AutoTimeFix: ntpd command completed with exit code: " + exitCode);
                                            
                                            // Fallback with standard date/time sync or toolbox/toybox if ntpd failed or busybox is missing
                                            if (exitCode != 0) {
                                                XposedBridge.log("AutoTimeFix: ntpd command failed. Trying sntp or manual date sync...");
                                                // Try sntp or modern clock settings if available
                                                Process p2 = Runtime.getRuntime().exec(new String[]{
                                                        "su", "-c", "settings put global ntp_server " + ntpServer
                                                });
                                                p2.waitFor();
                                            } else {
                                                XposedBridge.log("AutoTimeFix: NTP synchronization successful!");
                                                break;
                                            }
                                        } catch (Exception cmdEx) {
                                            XposedBridge.log("AutoTimeFix: Command execution failed: " + cmdEx.getMessage());
                                        }
                                        Thread.sleep(5000);
                                    }

                                } catch (Exception e) {
                                    XposedBridge.log("AutoTimeFix error: " + e);
                                }
                            }
                        }).start();
                    }
                }
        );
    }

    private boolean isInternetAvailable() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 8.8.8.8");
            return p.waitFor() == 0;
        } catch (Exception e) {
            XposedBridge.log("AutoTimeFix ping error: " + e.getMessage());
            return false;
        }
    }
}
