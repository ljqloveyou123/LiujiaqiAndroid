package com.huanju.chajianhuatest.ams;


import android.os.Handler;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author 刘镓旗
 * @date 17/2/21
 */
public class AMSHookHelper {

    public static final String EXTRA_TARGET_INTENT = "extra_target_intent";
    public static int LAUNCH_ACTIVITY;
    /**
     * Hook AMS
     * <p/>
     * 主要完成的操作是  "把真正要启动的Activity临时替换为在AndroidManifest.xml中声明的替身Activity"
     * <p/>
     * 进而骗过AMS
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public static void hookActivityManagerNative() throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, NoSuchFieldException {

        //获取ActivityManagerNative的类
        Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
        //拿到gDefault字段
        Field gDefaultField = activityManagerNativeClass.getDeclaredField("gDefault");
        gDefaultField.setAccessible(true);
        //从gDefault字段中取出这个对象的值
        Object gDefault = gDefaultField.get(null);

        // gDefault是一个 android.util.Singleton对象; 我们取出这个单例里面的字段
        Class<?> singleton = Class.forName("android.util.Singleton");

        //这个gDefault是一个Singleton类型的，我们需要从Singleton中再取出这个单例的AMS代理
        Field mInstanceField = singleton.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        //ams的代理对象
        Object rawIActivityManager = mInstanceField.get(gDefault);

        // 创建一个这个对象的代理对象, 然后替换这个字段, 让我们的代理对象帮忙干活,这里我们使用动态代理
        Class<?> iActivityManagerInterface = Class.forName("android.app.IActivityManager");
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { iActivityManagerInterface }, new IActivityManagerHandler(rawIActivityManager));
        mInstanceField.set(gDefault, proxy);

    }

    /**
     * 由于之前我们用替身欺骗了AMS; 现在我们要换回我们真正需要启动的Activity
     * <p/>
     * 不然就真的启动替身了, 狸猫换太子...
     * <p/>
     * 到最终要启动Activity的时候,会交给ActivityThread 的一个内部类叫做 H 来完成
     * H 会完成这个消息转发; 最终调用它的callback
     */
    public static void hookActivityThreadHandler() throws Exception {

//         先获取到当前的ActivityThread对象
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        //他有一个方法返回了自己
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);
        //执行方法得到ActivityThread对象
        Object currentActivityThread = currentActivityThreadMethod.invoke(null);

        // 由于ActivityThread一个进程只有一个,我们获取这个对象的mH
        Field mHField = activityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        //得到H这个Handler
        Handler mH = (Handler) mHField.get(currentActivityThread);

        Field mCallBackField = Handler.class.getDeclaredField("mCallback");
        mCallBackField.setAccessible(true);
        //设置我们自己的CallBackField
        mCallBackField.set(mH, new ActivityThreadHandlerCallback(mH));

    }
}
