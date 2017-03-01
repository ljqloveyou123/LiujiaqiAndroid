package com.huanju.chajianhuatest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by DELL-PC on 2017/2/23.
 */

public class MyInstrumentation extends Instrumentation {

    private final Context mContext;
    private final Instrumentation mInstr;

    public MyInstrumentation(Instrumentation base, Context context) {
        mInstr = base;
        mContext = context;
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        Log.e("Main", "走了我的callActivityOnCreate");
        Log.e("Main", "activity = " + activity);
        Log.e("Main", "activity class = " + activity.getClass());
        Log.e("Main", "activity resource = " + activity.getResources());

            try {
                //拿到ContextWrapper类中的字段mBase字段，就是Context
                Class<?> aClass = activity.getClass();
                Log.e("Main", "activity aClass = " + aClass);
                Log.e("Main", "activity aClass aClass = " + aClass.getSuperclass());
                Log.e("Main", "activity aClass aClass aClass = " + aClass.getSuperclass().getSuperclass());
                Field mBaseField = Activity.class.getSuperclass().getSuperclass().getDeclaredField("mBase");

                mBaseField.setAccessible(true);
                Context mBase = (Context) mBaseField.get(activity);
                Log.e("Main", "mBase = " + mBase);

                //拿出Context中的Resource字段
                Class<?> mContextImplClass = Class.forName("android.app.ContextImpl");
                Field mResourcesField = mContextImplClass.getDeclaredField("mResources");
                mResourcesField.setAccessible(true);

                //创建我们自己的Resource
                String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/chajian_demo.apk";
                String mPath = mContext.getApplicationContext().getPackageResourcePath();

                AssetManager assetManager = AssetManager.class.newInstance();
                Method addAssetPathMethod = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
                addAssetPathMethod.setAccessible(true);

                addAssetPathMethod.invoke(assetManager, mPath);
                addAssetPathMethod.invoke(assetManager, apkPath);


                Method ensureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks");
                ensureStringBlocks.setAccessible(true);
                ensureStringBlocks.invoke(assetManager);

                Resources supResource = mContext.getResources();
                Log.e("Main", "supResource = " + supResource);
                Resources newResource = new Resources(assetManager, supResource.getDisplayMetrics(), supResource.getConfiguration());
                mResourcesField.set(mBase, newResource);
                Log.e("Main", "设置 getResource = " + activity.getResources());
            } catch (Exception e) {
                Log.e("Main", "走了我的callActivityOnCreate 错了 = " + e.getMessage());
                e.printStackTrace();
            }
        super.callActivityOnCreate(activity, icicle);
    }
}
