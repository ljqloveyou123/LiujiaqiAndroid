package com.huanju.chajianhuatest;



import android.app.Instrumentation;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * @author 刘镓旗
 * @date 17/2/21
 */
public class MyHookHelper {

    /**
     * 加载插件
     * @param loader
     */
    public static void inject(DexClassLoader loader){
        //拿到本应用的ClassLoader
        PathClassLoader pathLoader = (PathClassLoader) MyApplication.getContext().getClassLoader();
        try {
            //获取宿主pathList
            Object suZhuPathList = getPathList(pathLoader);
            Object chaJianPathList = getPathList(loader);
            Object dexElements = combineArray(
                    //获取本应用ClassLoader中的dex数组
                    getDexElements(suZhuPathList),
                    //获取插件CassLoader中的dex数组
                    getDexElements(chaJianPathList));
//            //获取合并后的pathList
//            Object pathList = getPathList(pathLoader);
            //将合并的pathList设置到本应用的ClassLoader
            setField(suZhuPathList, suZhuPathList.getClass(), "dexElements", dexElements);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取pathList字段
     * @param baseDexClassLoader 需要获取pathList字段的ClassLoader
     * @return 返回pathList字段
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private static Object getPathList(Object baseDexClassLoader)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
//        ClassLoader bc = (ClassLoader)baseDexClassLoader;
        //通过这个ClassLoader获取pathList字段
        return getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    /**
     * 反射需要获取的字段
     * @param obj 需要字段获取的对象
     * @param cl 需要获取字段的类
     * @param field 需要获取的字段名称
     * @return 获取后的字段
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private static Object getField(Object obj, Class<?> cl, String field)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        //反射需要获取的字段
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(obj);
    }

    /**
     * 获取DexElements
     * @param paramObject 需要获取字段的对象
     * @return 返回获取的字段
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static Object getDexElements(Object paramObject)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return getField(paramObject, paramObject.getClass(), "dexElements");
    }

    /**
     * 反射需要设置字段的类并设置新字段
     * @param obj
     * @param cl
     * @param field
     * @param value
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private static void setField(Object obj, Class<?> cl, String field,
                                 Object value) throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {

        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        localField.set(obj, value);
    }

    /**
     * 合成dex数组
     * @param suzhu 宿主应用的dex数组
     * @param chajian 插件应用的dex数组
     * @return
     */
    private static Object combineArray(Object suzhu, Object chajian) {
        //获取原数组类型
        Class<?> localClass = suzhu.getClass().getComponentType();
        //获取原数组长度
        int i = Array.getLength(suzhu);
        //插件数组加上原数组的长度
        int j = i + Array.getLength(chajian);
        //创建一个新的数组用来存储
        Object result = Array.newInstance(localClass, j);
        //一个个的将dex文件设置到新数组中
        for (int k = 0; k < j; ++k) {
            if (k < i) {
                Array.set(result, k, Array.get(suzhu, k));
            } else {
                Array.set(result, k, Array.get(chajian, k - i));
            }
        }
        return result;
    }

    /**
     * hookActivity的Resource，如果想马上生效需要针对使用Resource的组件hook
     * @param context
     */
    public static void hookActivityResource(Context context){
        try {
            //获取ActiivtiyThread类
            Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");

            //获取当前ActivityThread
            Method currentActivityThread = mActivityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThread.setAccessible(true);
            Object mCurrentActivityThread = currentActivityThread.invoke(null);

            //获取mInstrumentation字段
            Field mInstrumentationField = mActivityThreadClass.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation baseInstrumentation = (Instrumentation) mInstrumentationField.get(mCurrentActivityThread);

            //设置我们自己的mInstrumentation
            Instrumentation proxy = new MyInstrumentation(baseInstrumentation,context);
            //替换
            mInstrumentationField.set(mCurrentActivityThread,proxy);
            Log.e("Main","替换Resource成功");

        } catch (Exception e) {
            Log.e("Main","替换Resource失败 = " + e.getMessage());
            e.printStackTrace();
        }
    }
}
