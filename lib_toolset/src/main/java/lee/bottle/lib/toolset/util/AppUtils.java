package lee.bottle.lib.toolset.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lee.bottle.lib.toolset.log.LLog;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by Leeping on 2018/4/16.
 * email: 793065165@qq.com
 */
public class AppUtils {

    /** ?????????????????????
     * <uses-permission android:name="android.permission.READ_CONTACTS"/>
     * */
    private List<String> readContacts(Activity activity) {
        List<String> list = new ArrayList<>();
        ContentResolver resolver = activity.getContentResolver();
        //???????????????????????????URI
        Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        // ???????????????
        String[] projection = {ContactsContract.CommonDataKinds.Phone._ID,//Id
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,//???????????????
                ContactsContract.CommonDataKinds.Phone.DATA1, "sort_key",//??????????????????
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,//?????????Id
                ContactsContract.CommonDataKinds.Phone.PHOTO_ID,//?????????Id
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY};
        @SuppressLint("Recycle") Cursor cursor = resolver.query(phoneUri, projection, null, null, null);
        assert cursor != null;
        while ((cursor.moveToNext())) {
            String name = cursor.getString(1);
            String phone = cursor.getString(2);
            list.add(name + ":" + phone);
        }
        return list;
    }

    /**
     * ????????????????????????????????????
     */
    public static boolean checkPermissionExist(Context context, String permissionName) {
        //????????????????????????????????????
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int hasPermission = context.checkSelfPermission(permissionName);
            return hasPermission != PackageManager.PERMISSION_DENIED;
        }
        return true;
    }

    /*
   ????????????????????????
   AppOpsManager.MODE_ALLOWED ?????? ??????????????????????????????????????????????????????
   AppOpsManager.MODE_IGNORED ?????? ???????????????????????????????????????
   AppOpsManager.MODE_ERRORED ?????? ?????????????????????????????????
   AppOpsManager.MODE_DEFAULT ?????? ?????????????????????????????????????????????????????????mode??????????????????
   */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean checkWindowPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsMgr == null)
                return false;
            int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), context
                    .getPackageName());
            return Settings.canDrawOverlays(context) || mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED;
        }
        return Settings.canDrawOverlays(context);
    }

    /**
     * ???????????????
     */
    public static void hideSoftInputFromWindow(@NonNull Activity activity) {
        try {
            View v = activity.getCurrentFocus();
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager inputMethodManager = ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE));
                if (inputMethodManager != null)
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /**
     *????????????????????????
     */
    public static boolean isOpenWifi(@NonNull Context context) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert mWifiManager != null;
        return mWifiManager.isWifiEnabled();
    }

    /**
     * ??????????????????
     * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
     */
    @SuppressLint("MissingPermission")
    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager != null) {
            try {
                NetworkInfo info = manager.getActiveNetworkInfo();
                if (info != null && info.isAvailable()) {
                    return true;
                }
            } catch (Exception ignored) {

            }
        }
        return false;
    }

    /* ??????????????????????????? */
    public static void getLocalFileToOutputStream(File file, OutputStream out) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] b = new byte[4096];
            int n;
            while ((n = fis.read(b)) != -1) {
                out.write(b, 0, n);
            }
        }
    }

    /* ??????????????????Byte?????? */
    public static byte[] getBytesByFile(File file) {
        int len = 1024;
        try (FileInputStream fis = new FileInputStream(file)) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(len)) {
                byte[] b = new byte[len];
                int n;
                while ((n = fis.read(b)) != -1) {
                    bos.write(b, 0, n);
                }
                return bos.toByteArray();
            }
        } catch (Exception e) {
            LLog.error(e);
        }
        return null;
    }

    /* ??????zip */
    public static boolean unZipToFolder(InputStream zipFileStream, File dir) {
        try (ZipInputStream inZip = new ZipInputStream(zipFileStream)) {

            ZipEntry zipEntry;
            String temp;
            while ((zipEntry = inZip.getNextEntry()) != null) {
                temp = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    //???????????????????????????
                    temp = temp.substring(0, temp.length() - 1);
                    File folder = new File(dir, temp);
                    folder.mkdirs();
                } else {
                    File file = new File(dir, temp);
                    if (!file.exists()) {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                    // ????????????????????????
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        int len;
                        byte[] buffer = new byte[1024];
                        while ((len = inZip.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                            out.flush();
                        }
                    }
                }

            }
            return true;
        } catch (IOException e) {
            LLog.error(e);
        }
        return false;
    }

    /* ???????????????????????? */
    private boolean isWirelessNetworkValid(Context context) {
        return AppUtils.isOpenWifi(context) && AppUtils.isNetworkAvailable(context);
    }

    /* ??????GPS???????????? */
    public static boolean isOenGPS(@NonNull Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /* ??????GPS???????????? */
    public static void openGPS(@NonNull Context context) {
        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        // ??????GPS????????????
        context.startActivity(intent);
    }

    /* ??????UI?????? */
    public static boolean checkUIThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /* ????????????????????? */
    public static String getCurrentProcessName(@NonNull Context context) {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
                if (process.pid == pid) {
                    processName = process.processName;
                }
            }
        }
        return processName;
    }

    /* ???????????????????????????????????? */
    public static boolean checkCurrentIsMainProgress(@NonNull Context context) {
        return checkCurrentIsMainProgress(context, AppUtils.getCurrentProcessName(context));
    }

    /* ???????????????????????????????????? */
    public static boolean checkCurrentIsMainProgress(@NonNull Context context, @NonNull String currentProgressName) {
        return context.getPackageName().equals(currentProgressName);
    }

    /* ????????????????????? */
    public static int getVersionCode(@NonNull Context ctx) {
        // ??????packagemanager?????????
        int version = 0;
        try {
            PackageManager packageManager = ctx.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            version = packInfo.versionCode;
        } catch (Exception e) {
            LLog.error(e);
        }
        return version;
    }

    /* ????????????????????? */
    public static String getVersionName(@NonNull Context ctx) {
        // ??????package manager?????????
        String version = "";
        try {
            PackageManager packageManager = ctx.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            version = packInfo.versionName;
        } catch (Exception e) {
            LLog.error(e);
        }
        return version;
    }

    /* ?????????????????? */
    public static void toastLong(@NonNull Context context, @NonNull String message) {
        if (!checkUIThread()) return;

        try {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            if (toast != null) {
                toast.show();
            }
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /* ?????????????????? */
    public static void toastShort(@NonNull Context context, @NonNull String message) {
        if (!checkUIThread()) return;

        try {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            if (toast != null) {
                toast.show();
            }
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /* ?????????????????? */
    public static void toastCustom(@NonNull Context context, @NonNull String message, int duration, int gravity, View view) {
        if (!checkUIThread()) return;

        try {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            if (toast != null) {
                if (gravity > 0) {
                    toast.setGravity(gravity, 0, 0);
                }
                if (view != null) {
                    toast.setView(view);
                }
                toast.show();
            }
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /* ???bitmap ???file */
    public static boolean bitmap2File(Bitmap bitmap, File file) {
        try {
            if (bitmap == null || file == null) return false;
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            return true;
        } catch (IOException e) {
            LLog.error(e);
        }
        return false;
    }

    /*
     * ?????????????????? ; ??????:  <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT"/>
     * */
    public static void addShortcut(Context context, int appIcon, boolean isCheck) {

        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
            if (isCheck) {
                boolean isExist = sharedPreferences.getBoolean("shortcut", false);
                if (isExist) return;
            }
            Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            Intent shortcutIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            final PackageManager pm = context.getPackageManager();
            String title = pm.getApplicationLabel(pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)).toString();
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
            shortcut.putExtra("duplicate", false);
            Parcelable iconResource = Intent.ShortcutIconResource.fromContext(context, appIcon);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
            context.sendBroadcast(shortcut);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("shortcut", true);
            editor.apply();
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    //??????assets????????????????????????
    public static String assetFileContentToText(Context c, String filePath) {
        InputStream in = null;
        try {
            in = c.getAssets().open(filePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                if (line != null) {
                    line = line.replaceAll("\\t", "");
                    line = line.replaceAll("\\s", "");
                    sb.append(line);
                }
            } while (line != null);

            bufferedReader.close();
            in.close();
            return sb.toString();
        } catch (Exception e) {
            LLog.error(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    // ??????apk
    /*
    * <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
     * */
    public static boolean installApk(Context context, File apkFile,String apkUrl) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                boolean isInstallPermission = context.getPackageManager().canRequestPackageInstalls();
                LLog.print("??????APK sdk= " + Build.VERSION.SDK_INT +" ???????????????????????? : "+ isInstallPermission);
                if(!isInstallPermission){
                    // ????????????????????????????????????????????????
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES ,Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return false;
                }
            }

            LLog.print("??????APK file : " + apkFile);
            if (!apkFile.exists()) return false;

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileProvider", apkFile);
            } else {
                //apk??????cache????????????????????????????????????
                String command = "chmod 777 " + apkFile.getAbsolutePath();
                Runtime runtime = Runtime.getRuntime();
                runtime.exec(command);
                uri = Uri.fromFile(apkFile);
            }
            LLog.print("??????APK uri : " + uri);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            LLog.error("??????APK ??????", e);
            // ?????????????????????????????????
            try{
                LLog.print("??????????????????????????? uri : " + apkUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }catch (Exception ex){
                LLog.error("??????APK ?????????????????????????????????", ex);
            }
            return false;
        }
    }



    /**
     * ????????????mac
     * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
     * <uses-permission android:name="android.permission.LOCAL_MAC_ADDRESS"/>
     * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
     *
     */
    @SuppressLint("HardwareIds")
    public static String devMAC(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // ?????????
                    return getNewMac();
                }

                String mac = info.getMacAddress();
                if (mac.equalsIgnoreCase("02:00:00:00:00:00")) return getNewMac();
                return mac;

            }
        }
        return "00:00:00:00:00:00";
    }
    /**
     * ?????????????????????
     */
    private static String getNewMac() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return null;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            LLog.error(ex);
        }
        return null;
    }



    /**
     * ????????????
     *<uses-permission android:name="android.permission.CALL_PHONE" />
     */
    public static void callPhoneNo(Activity activity, String phoneNo){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:"+phoneNo));
        activity.startActivity(intent);
    }

    public static void releaseImageView(ImageView iv) {
        if (iv == null) return;
        Drawable drawable = iv.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }


    /**
     * ?????????????????????
     */
    public static String getClipboardContent(Context context){
        String pasteString = "";

        try {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                ClipData clipData = manager.getPrimaryClip();
                if (clipData!=null && clipData.getItemCount() > 0) {
                    CharSequence text = clipData.getItemAt(0).getText();
                    pasteString = text.toString();
                }
            }
        } catch (Exception e) {
            LLog.error(e);
        }

        return pasteString;
    }

    /* ????????????????????? */
    public static void setClipboardContent(Context context,String content){
        try {
            // ????????????????????????
            ClipboardManager cmb = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);

            // ?????????????????????????????????????????????????????????????????????????????????????????????
            ClipData clipData = ClipData.newPlainText(null, content);
            // ??????????????????????????????????????????
            cmb.setPrimaryClip(clipData);

        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /* ???????????????????????? */
    public static boolean schemeValid(Context context,String scheme) {
        PackageManager manager = context.getPackageManager();
        Intent action = new Intent(Intent.ACTION_VIEW);
        action.setData(Uri.parse(scheme));
        @SuppressLint("QueryPermissionsNeeded")
        List<ResolveInfo> list = manager.queryIntentActivities(action, PackageManager.GET_RESOLVED_FILTER);
        return list != null && list.size() > 0;
    }

    /* ??????????????????activity */
    public static void schemeJump(Context context,String scheme){
        Intent action = new Intent(Intent.ACTION_VIEW);
        action.setFlags(FLAG_ACTIVITY_NEW_TASK);
        action.setData(Uri.parse( scheme ));
        context.startActivity(action);
    }

    /* ????????????????????? */
    public static int statusBarHeight(Context context){
        int statusBarHeight = -1;
        //??????status_bar_height?????????ID
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            //????????????ID????????????????????????
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        if (statusBarHeight == -1){
            try {
                @SuppressLint("PrivateApi")
                Class<?> clazz = Class.forName("com.android.internal.R$dimen");
                Object object = clazz.newInstance();
                int height = Integer.parseInt(
                        String.valueOf(clazz.getField("status_bar_height").get(object))
                );
                statusBarHeight = context.getResources().getDimensionPixelSize(height);
            } catch (Exception e) {
                LLog.error(e);
            }
        }

        return statusBarHeight;
    }



    //????????????????????????????????????????????????
    public static boolean isNotifyEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return isNotifyEnableV26(context);
        } else {
            return isNotifyEnabledV19(context);
        }
    }
    /**
     * 8.0????????????
     * @param context api19  4.4???????????????
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static boolean isNotifyEnableV26(Context context) {

        AppOpsManager mAppOps =
                (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        Class<?> appOpsClass = null;

        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());

            Method checkOpNoThrowMethod =
                    appOpsClass.getMethod("checkOpNoThrow",
                            Integer.TYPE, Integer.TYPE, String.class);

            Field opPostNotificationValue = appOpsClass.getDeclaredField("OP_POST_NOTIFICATION");
            int value = (Integer) opPostNotificationValue.get(Integer.class);

            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) ==
                    AppOpsManager.MODE_ALLOWED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * 8.0???????????????????????????
     * @param context
     * @return
     */
    private static boolean isNotifyEnabledV19(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        try {
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            @SuppressLint("DiscouragedPrivateApi")
            Method sServiceField = notificationManager.getClass().getDeclaredMethod("getService");
            sServiceField.setAccessible(true);
            Object sService = sServiceField.invoke(notificationManager);

            Method method = sService.getClass().getDeclaredMethod("areNotificationsEnabledForPackage",String.class, Integer.TYPE);
            method.setAccessible(true);
            return (boolean) method.invoke(sService, pkg, uid);
        } catch (Exception e) {
            return true;
        }
    }

    /** ??????IP?????? */
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
