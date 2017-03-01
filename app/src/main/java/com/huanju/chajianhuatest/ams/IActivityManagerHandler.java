package com.huanju.chajianhuatest.ams;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import com.huanju.chajianhuatest.MyApplication;
import com.huanju.chajianhuatest.ZhanKengActivitiy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author 刘镓旗
 * @date 17/2/21
 */
class IActivityManagerHandler implements InvocationHandler {

    private static final String TAG = "IActivityManagerHandler";

    Object mBase;

    public IActivityManagerHandler(Object base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if ("startActivity".equals(method.getName())) {
            Log.e("Main","startActivity方法拦截了");

            // 找到参数里面的第一个Intent 对象
            Intent raw;
            int index = 0;

            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            raw = (Intent) args[index];
            //创建一个要被掉包的Intent
            Intent newIntent = new Intent();
            // 替身Activity的包名, 也就是我们自己的"包名"
            String stubPackage = MyApplication.getContext().getPackageName();

            // 这里我们把启动的Activity临时替换为 ZhanKengActivitiy
            ComponentName componentName = new ComponentName(stubPackage, ZhanKengActivitiy.class.getName());
            newIntent.setComponent(componentName);

            // 把我们原始要启动的TargetActivity先存起来
            newIntent.putExtra(AMSHookHelper.EXTRA_TARGET_INTENT, raw);

            // 替换掉Intent, 达到欺骗AMS的目的
            args[index] = newIntent;
            Log.e("Main","startActivity方法 hook 成功");
            Log.e("Main","args[index] hook = " + args[index]);
            return method.invoke(mBase, args);
        }

        return method.invoke(mBase, args);
    }
}
