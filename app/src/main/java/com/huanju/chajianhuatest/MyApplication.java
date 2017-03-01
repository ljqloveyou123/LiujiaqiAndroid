package com.huanju.chajianhuatest;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * @author 刘镓旗
 * @date 17/2/21
 */
public class MyApplication extends Application {
    private static Context sContext;

    public static DexClassLoader mClassLoader;
    private AssetManager assetManager;
    private Resources newResource;
    private Resources.Theme mTheme;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        MyHookHelper.hookActivityResource(base);
        sContext = base;

        try {
//            //拿到ContextWrapper类中的字段mBase字段，就是Context
//            Class<?> aClass = activity.getClass();
//            Log.e("Main", "activity aClass = " + aClass);
//            Log.e("Main", "activity aClass aClass = " + aClass.getSuperclass());
//            Log.e("Main", "activity aClass aClass aClass = " + aClass.getSuperclass().getSuperclass());
//            Field mBaseField = Activity.class.getSuperclass().getSuperclass().getDeclaredField("mBase");
//
//            mBaseField.setAccessible(true);
//            Context mBase = (Context) mBaseField.get(activity);
//            Log.e("Main", "mBase = " + mBase);
//
//            //拿出Context中的Resource字段
//            Class<?> mContextImplClass = Class.forName("android.app.ContextImpl");
//            Field mResourcesField = mContextImplClass.getDeclaredField("mResources");
//            mResourcesField.setAccessible(true);

            //创建我们自己的Resource
            String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/chajian_demo.apk";
            String mPath = getPackageResourcePath();

            assetManager = AssetManager.class.newInstance();
            Method addAssetPathMethod = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);

//            addAssetPathMethod.invoke(assetManager, mPath);
            addAssetPathMethod.invoke(assetManager, apkPath);


            Method ensureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocks.setAccessible(true);
            ensureStringBlocks.invoke(assetManager);

            Resources supResource = getResources();
            Log.e("Main", "supResource = " + supResource);
            newResource = new Resources(assetManager, supResource.getDisplayMetrics(), supResource.getConfiguration());
            Log.e("Main", "设置 getResource = " + getResources());

            mTheme = newResource.newTheme();
            mTheme.setTo(super.getTheme());
        } catch (Exception e) {
            Log.e("Main", "走了我的callActivityOnCreate 错了 = " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    public AssetManager getAssets() {
        return assetManager == null ? super.getAssets() : assetManager;
    }

    @Override
    public Resources getResources() {
        return newResource == null ? super.getResources() : newResource;
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }

    public static Context getContext() {
        return sContext;
    }
}
