我们通过前4篇的分解，分别将插件化设计到的知识点全部梳理了一遍，如果没有看过的，建议先看前面4篇

[Binder机制](http://blog.csdn.net/yulong0809/article/details/56841993)

[插件化知识详细分解及原理 之代理，hook，反射](http://blog.csdn.net/yulong0809/article/details/56842027)，

[类加载及dex加载](http://blog.csdn.net/yulong0809/article/details/58041467)

[应用启动过程及类加载过程](http://blog.csdn.net/yulong0809/article/details/58589715)

好了上面介绍了之前准备的知识点后今天我们做一个真正的可运行的启动插件demo，要知道一个插件可能是随时从网上下载下来的，那么也就是说其实这个apk不会被安装，那么如果不被安装，怎么能被加载呢，
又如何管理插件中四大组件的生命周期呢，没有生命周期的四大组件是没有意义的。而且Activity是必须要在AndroidManifest中注册的，不注册就会抛出异常,那么怎么能绕过这个限制呢，还有，一个apk中肯定会用过各种资源，那么又该如何动态的加载资源呢。下面我们就带着这些问问一一的来解决，实现插件化，或者或是模块化。

先来看一下最终的运行结果
![这里写图片描述](http://img.blog.csdn.net/20170301160936240?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQveXVsb25nMDgwOQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast) ![这里写图片描述](http://img.blog.csdn.net/20170301161132868?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQveXVsb25nMDgwOQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)![这里写图片描述](http://img.blog.csdn.net/20170301161330449?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQveXVsb25nMDgwOQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
###分析思路：
####代码的动态加载：
 apk被安装之后，apk的文件代码以及资源会被系统存放在固定的目录比如/data/app/package_name/base-1.apk)中，系统在进行类加载的时候，会自动去这一个或者几个特定的路径来寻找这个类

![](http://img.blog.csdn.net/20170301160354574?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQveXVsb25nMDgwOQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

但是要知道插件apk是不会被安装的，那么系统也就不会讲我们的代理及资源存在在这个目录下，换句话说系统并不知道我们插件apk中的任何信息，也就根本不可能加载我们插件中的类。我们之前分析过应用的启动过程，其实就是启动了我们的主Activity，然后在ActivityThread的performLaunchActivity方法中创建的Activity对象并回调了attch和onCreate方法，我们再来看一下创建Activity对象时的代码(没看过应用启动过程的建议先看看 [插件化知识详细分解及原理 之应用的启动过程](http://blog.csdn.net/yulong0809/article/details/58589715)),

		java.lang.ClassLoader cl = r.packageInfo.getClassLoader();
		activity = mInstrumentation.newActivity(cl, component.getClassName(), r.intent);
		StrictMode.incrementExpectedActivityCount(activity.getClass());
		r.intent.setExtrasClassLoader(cl);

系统通过待启动的Activity的类名className，然后使用ClassLoader对象cl把这个类加载，最后使用反射创建了这个Activity类的实例对象。我们看一下这个cl对象是通过r.packageInfo.getClassLoader()被赋值的，这个r.packageInfo是一个LoadedApk类型的对象，我们看看这个LoadedApk是什么东西。

	/**
	 * Local state maintained about a currently loaded .apk.
	 * @hide
	 */
	public final class LoadedApk {
		  private static final String TAG = "LoadedApk";

	    private final ActivityThread mActivityThread;
	    private ApplicationInfo mApplicationInfo;
	    final String mPackageName;
	    private final String mAppDir;
	    private final String mResDir;
	    private final String[] mSplitAppDirs;
	    private final String[] mSplitResDirs;
	    private final String[] mOverlayDirs;
	    private final String[] mSharedLibraries;
	    private final String mDataDir;
	    private final String mLibDir;
	    private final File mDataDirFile;
	    private final ClassLoader mBaseClassLoader;
	    private final boolean mSecurityViolation;
	    private final boolean mIncludeCode;
	    private final boolean mRegisterPackage;
	    private final DisplayAdjustments mDisplayAdjustments = new DisplayAdjustments();
	    Resources mResources;
	    private ClassLoader mClassLoader;
	    private Application mApplication;
	
	    private final ArrayMap<Context, ArrayMap<BroadcastReceiver, ReceiverDispatcher>> mReceivers
	        = new ArrayMap<Context, ArrayMap<BroadcastReceiver, LoadedApk.ReceiverDispatcher>>();
	    private final ArrayMap<Context, ArrayMap<BroadcastReceiver, LoadedApk.ReceiverDispatcher>> mUnregisteredReceivers
	        = new ArrayMap<Context, ArrayMap<BroadcastReceiver, LoadedApk.ReceiverDispatcher>>();
	    private final ArrayMap<Context, ArrayMap<ServiceConnection, LoadedApk.ServiceDispatcher>> mServices
	        = new ArrayMap<Context, ArrayMap<ServiceConnection, LoadedApk.ServiceDispatcher>>();
	    private final ArrayMap<Context, ArrayMap<ServiceConnection, LoadedApk.ServiceDispatcher>> mUnboundServices
	        = new ArrayMap<Context, ArrayMap<ServiceConnection, LoadedApk.ServiceDispatcher>>();

		。。。
	}

注释的大概意思是LoadedApk对象是APK文件在内存中的表示。 Apk文件的相关信息，诸如Apk文件的代码和资源，甚至代码里面的Activity，Service等四大组件的信息我们都可以通过此对象获取。我们知道了r.packageInfo是一个LoadedApk对象了，我们再看他是在哪被赋值的，我们顺着代码往前倒，performLaunchActivity，handleLaunchActivity，到了H类里，这一个Handler，
	
	  switch (msg.what) {
                case LAUNCH_ACTIVITY: {
                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
                    final ActivityClientRecord r = (ActivityClientRecord) msg.obj;

                    r.packageInfo = getPackageInfoNoCheck(
                            r.activityInfo.applicationInfo, r.compatInfo);
                    handleLaunchActivity(r, null);
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                } break;

在这里通过getPackageInfoNoCheck方法被赋值，进去看看

	public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai,
            CompatibilityInfo compatInfo) {
        return getPackageInfo(ai, compatInfo, null, false, true, false);
    }

又调用了getPackageInfo，再进去

	private LoadedApk getPackageInfo(ApplicationInfo aInfo, CompatibilityInfo compatInfo,
            ClassLoader baseLoader, boolean securityViolation, boolean includeCode,
            boolean registerPackage) {
        synchronized (mResourcesManager) {
            WeakReference<LoadedApk> ref;

            if (includeCode) {
                ref = mPackages.get(aInfo.packageName);
            } else {
                ref = mResourcePackages.get(aInfo.packageName);
            }
            LoadedApk packageInfo = ref != null ? ref.get() : null;
            if (packageInfo == null || (packageInfo.mResources != null
                    && !packageInfo.mResources.getAssets().isUpToDate())) {
                if (localLOGV) Slog.v(TAG, (includeCode ? "Loading code package "
                        : "Loading resource-only package ") + aInfo.packageName
                        + " (in " + (mBoundApplication != null
                                ? mBoundApplication.processName : null)
                        + ")");
                packageInfo =
                    new LoadedApk(this, aInfo, compatInfo, baseLoader,
                            securityViolation, includeCode &&
                            (aInfo.flags&ApplicationInfo.FLAG_HAS_CODE) != 0, registerPackage);

                if (mSystemThread && "android".equals(aInfo.packageName)) {
                    packageInfo.installSystemApplicationInfo(aInfo,
                            getSystemContext().mPackageInfo.getClassLoader());
                }

                if (includeCode) {
                    mPackages.put(aInfo.packageName,
                            new WeakReference<LoadedApk>(packageInfo));
                } else {
                    mResourcePackages.put(aInfo.packageName,
                            new WeakReference<LoadedApk>(packageInfo));
                }
            }
            return packageInfo;
        }
    }

从一个叫mPackages的map集合中试图获得缓存，如果缓存不存在直接new一个，然后存入map集合。

好了到这里，其实我们就有了大概的思路，而且有两种实现方法。

#####1. 首先如果我们想要加载我们的插件apk我们需要一个Classloader，那么我们知道系统的Classloader是通过LoadedApk对象获得的，而如果我们想要加载我们自己的插件apk，就需要我们自己构建一个LoadedApk对象，然后修改其中的Classloader对象，因为系统的并不知道我们的插件apk的信息，所有我们就要创建自己的ClassLoader对象，然后全盘接管加载的过程，然后通过hook的思想将我们构建的这个LoadedApk对象存入那个叫mPackages的map中，这样的话每次在获取LoadedApk对象时就可以在map中得到了。然后在到创建Activity的时候得到的Classloader对象就是我们自己改造过的cl了，这样就可以加载我们的外部插件了。这种方案需要我们hook掉系统系统的n多个类或者方法，因为创建LoadedApk对象时还需要一个ApplicationInfo的对象，这个对象就是解析AndroidManifest清单得来的，所以还需要我们自己手动解析插件中的AndroidManifest清单，这个过程及其复杂，不过360的DroidPlugin就使用了这种方法。

#####2. 既然我们知道如果想启动插件apk就需一个Classloader，那么我们换一种想法，能不能我们将我们的插件apk的信息告诉系统的这个Classloader，然后让系统的Classloader来帮我们加载及创建呢？答案是肯定，之前我们说过讲过android中的Classloader主要分析PathClassLoader和DexClassLoader，系统通过PathClassLoader来加载系统类和主dex中的类。而DexClassLoader则用于加载其他dex文件中的类。他们都是继承自BaseDexClassLoader。（如果没有看过的建议先看看 [插件化知识详细分解及原理 之ClassLoader及dex加载过程](http://blog.csdn.net/yulong0809/article/details/58041467)）

我们再简单的回顾一下

		这个类中维护这一个dexElements的数组，在findClass的时候会遍历数组来查找
			public Class findClass(String name, List<Throwable> suppressed) {
		   for (Element element : dexElements) {
			   DexFile dex = element.dexFile;

			   if (dex != null) {
				   Class clazz = dex.loadClassBinaryName(name, definingContext, suppressed);
				   if (clazz != null) {
					   return clazz;
				   }
			   }
		   }
		   if (dexElementsSuppressedExceptions != null) {
			   suppressed.addAll(Arrays.asList(dexElementsSuppressedExceptions));
		   }
		   return null;
		}

类在被加载的时候是通过BaseDexClassLoader的findClass的方法，其实最终调用了DexPathList类的findClass，DexPathList类中维护着dexElements的数组，这个数组就是存放我们dex文件的数组，我们只要想办法将我们插件apk的dex文件插入到这个dexElements中系统就可以知道我们的插件apk信息了，也自然就可以帮我们加载并创建对应的类。但是到这里还有一个问题，那就是Activity必须要在AndroidManifest注册才行，这个检查过程是在系统底层的，我们无法干涉，可是我们的插件apk是动态灵活的，宿主中并不固定的写死注册哪几个Activity，如果写死也就失去了插件的动态灵活性。

但是我们可以换一种方式，我们使用hook思想代理startActivity这个方法，使用占坑的方式，也就是说我们可以提前在AndroidManifest中固定写死一个Activity，这个Activity只不过是一个傀儡，我们在启动我们插件apk的时候使用它去系统层校检合法性，然后等真正创建Activity的时候再通过hook思想拦截Activity的创建方法，提前将信息更换回来创建真正的插件apk。


###总结一下分析结果

	1.startActivity的时候最终会走到AMS的startActivity方法，
				
	2.系统会检查一堆的信息验证这个Activity是否合法。
	
	3.然后会回调ActivityThread的Handler里的 handleLaunchActivity
	
	4.在这里走到了performLaunchActivity方法去创建Activity并回调一系列生命周期的方法
	
	5.创建Activity的时候会创建一个LoaderApk对象，然后使用这个对象的getClassLoader来创建Activity
	
	6.我们查看getClassLoader()方法发现返回的是PathClassLoader，然后他继承自BaseDexClassLoader
	
	7.然后我们查看BaseDexClassLoader发现他创建时创建了一个DexPathList类型的pathList对象，然后在findClass时调用了pathList.findClass的方法
	
	8.然后我们查看DexPathList类中的findClass发现他内部维护了一个Element[] dexElements的dex数组，findClass时是从数组中遍历查找的，


###根据我们的分析结果我们整理一下实现步骤,下面有完整的实现demo下载，可运行

##### 1. 首先我们通过DexClassloader创建一个我们自己的DexClassloader对象去加载我们的插件apk，因为之前分析过，只有DexClassloader才能加载其他的dex文件，至于参数的意思之前的篇幅都讲过，不在啰嗦

		//dex优化后路径
		String cachePath = MainActivity.this.getCacheDir().getAbsolutePath();
		
		//插件apk的路径
		String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/chajian_demo.apk";

 		//创建一个属于我们自己插件的ClassLoader，我们分析过只能使用DexClassLoader
		DexClassLoader mClassLoader = new DexClassLoader(apkPath, cachePath,cachePath, getClassLoader());	

#####2. 拿到宿主apk里ClassLoader中的pathList对象和我们Classloader的pathList，因为最终加载时是执行了pathList.findClass方法

		 //拿到本应用的ClassLoader
	    PathClassLoader pathLoader = (PathClassLoader) MyApplication.getContext().getClassLoader();

		//获取宿主pathList
		  Object suZhuPathList = getPathList(pathLoader);

		//获取插件pathList
           Object chaJianPathList = getPathList(loader);
		
#####3. 然后我们拿到宿主pathList对象中的Element[]和我们创建的Classloader中的Element[]
	
		  //获取宿主数组
		Object suzhuElements = getDexElements(suZhuPathList)
		
		//获取插件数组
		 Object chajianElements = getDexElements(chaJianPathList)
        

#####4. 因为我们要加入一个dex文件，那么原数组的长度要增加，所有我们要新建一个新的Element类型的数组，长度是新旧的和
	
		  //获取原数组类型
        Class<?> localClass = suzhu.getClass().getComponentType();
        //获取原数组长度
        int i = Array.getLength(suzhu);
        //插件数组加上原数组的长度
        int j = i + Array.getLength(chajian);
        //创建一个新的数组用来存储
        Object result = Array.newInstance(localClass, j);		


#####5. 将我们的插件dex文件和宿主原来的dex文件都放入我们新建的数组中合并
	
		 //一个个的将dex文件设置到新数组中
        for (int k = 0; k < j; ++k) {
            if (k < i) {
                Array.set(result, k, Array.get(suzhu, k));
            } else {
                Array.set(result, k, Array.get(chajian, k - i));
            }
        }

#####6. 将我们的新数组设值给pathList对象

		setField(suZhuPathList, suZhuPathList.getClass(), "dexElements", result);

#####7. 代理系统启动Activity的方法，然后将要启动的Activity替换成我们占坑的Activity已达到欺骗系统去检查的目的.

这里我们又要在继续分析了,我们要拦截startActivity，之前我们分析启动过程时知道，最终会调用ActivityManagerNative.getDefault().startActivity，其实也就是ActivityManagerService中的startActivity。

	int result = ActivityManagerNative.getDefault()
                .startActivity(whoThread, who.getBasePackageName(), intent,
                        intent.resolveTypeIfNeeded(who.getContentResolver()),
                        token, target != null ? target.mEmbeddedID : null,
                        requestCode, 0, null, options);

我们再看看ActivityManagerNative.getDefault()的方法
	
    static public IActivityManager getDefault() {
        return gDefault.get();
    }

返回了一个gDefault.get()，继续看

	private static final Singleton<IActivityManager> gDefault = new Singleton<IActivityManager>() {
        protected IActivityManager create() {
            IBinder b = ServiceManager.getService("activity");
            if (false) {
                Log.v("ActivityManager", "default service binder = " + b);
            }
            IActivityManager am = asInterface(b);
            if (false) {
                Log.v("ActivityManager", "default service = " + am);
            }
            return am;
        }
    };

看到这个代码是不是很熟悉，因为我们第一篇分析Binder机制的时候就知道了，这其实就是aidl的方式远程通信方式，没看过的可以去看一下 [插件化知识详细分解及原理 之Binder机制](http://blog.csdn.net/yulong0809/article/details/56841993)

Singleton是系统提供的单例辅助类，这个类在4.x加入的，如果需要适配其他版本，请自行查阅源码

由于AMS需要频繁的和我们的应用通信，所有系统使用了一个单例把这个AMS的代理对象保存了起来；这也是AMS这个系统服务与其他普通服务的不同之处，这样我们就不需要通过ServiceManager去Hook，我们只需要简单地Hook掉这个单例即可。 [插件化知识详细分解及原理 之代理，hook，反射](http://blog.csdn.net/yulong0809/article/details/56842027)。

那么也就是说我们要代理ActivityManagerNative.getDefault()的这个返回值就好了，也就是AMS的代理对象，有的朋友可能会问为什么不直接代理AMS，因为AMS是系统的，不在我们的进程中，我们能操作的只有这个AMS的代理类
	 
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
	
现在我们已经拿到了这个ams的代理对象，现在我们需要创建一个我们自己的代理对象去拦截原ams中的方法,


	class IActivityManagerHandler implements InvocationHandler {
		...

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

然后我们使用动态代理去代理上面获取的ams

		 // 创建一个这个对象的代理对象, 然后替换这个字段, 让我们的代理对象帮忙干活,这里我们使用动态代理,

		//动态代理依赖接口，而ams实现与IActivityManager
        Class<?> iActivityManagerInterface = Class.forName("android.app.IActivityManager");
		
		//返回代理对象,IActivityManagerHandler是我们自己的代理对象，具体代码请下载demo
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[] { iActivityManagerInterface }, new IActivityManagerHandler(rawIActivityManager));
		
		//将我们的代理设值给singleton的单例
        mInstanceField.set(gDefault, proxy);

#####8. 等系统检查完了之后，再次代理拦截系统创建Activity的方法将原来我们替换的Activity再次替换回来，已到达启动不在AndroidManifest注册的目的


这里我们继续分析怎么将我们前面存入要打开的Activity再换回来,我们之前分析应用的启动过程时知道，系统检查完了Activity的合法性后，会回调ActivityThread里的scheduleLaunchActivity方法，然后这个方法发送了一个消息到ActivityThread的内部类H里，这是一个Handler，看一下代码

	  private class H extends Handler {

		...
	 public void handleMessage(Message msg) {
		 case LAUNCH_ACTIVITY: {
	                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityStart");
	                    final ActivityClientRecord r = (ActivityClientRecord) msg.obj;
	
	                    r.packageInfo = getPackageInfoNoCheck(
	                            r.activityInfo.applicationInfo, r.compatInfo);
	                    handleLaunchActivity(r, null);
	                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
	                } break;
		...

		}

	}

那么我们如果想替换回我们的信息就要从这里入手了，至于Handler的消息机制这里不会深入，大概我们看一下Handler怎么处理消息的

	/**
     * Handle system messages here.
     */
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }


从上面我们看到，如果这个传递的机制

如果传递的Message本身就有callback，那么直接使用Message对象的callback方法；
如果Handler类的成员变量mCallback不为空，那么首先执行这个mCallback回调；
如果mCallback的回调返回true，那么表示消息已经成功处理；直接结束。
如果mCallback的回调返回false，那么表示消息没有处理完毕，会继续使用Handler类的handleMessage方法处理消息。

通过上面给出的H的部分代码我们看到他只重新了Handler的handleMessage方法，并没有设置Callback，那么我们就可以利用这一点，给这个H设置一个Callback让他在走handleMessage之前先走我们的方法，然后我们替换回之前的信息，再让他走H的handleMessage

    	先获取到当前的ActivityThread对象
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");

        //他有一个方法返回了自己
        Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);

        //执行方法得到ActivityThread对象
        Object currentActivityThread = currentActivityThreadMethod.invoke(null);

        // 由于ActivityThread一个进程只有一个,我们获取这个对象的mH字段，也就是H这个Handler
        Field mHField = activityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);

        //得到H这个Handler
        Handler mH = (Handler) mHField.get(currentActivityThread);
		
		//创建一个我们的CallBack并赋值给mH
        Field mCallBackField = Handler.class.getDeclaredField("mCallback");
        mCallBackField.setAccessible(true);
        //设置我们自己的CallBackField，具体替换信息代码请下载demo查看
        mCallBackField.set(mH, new ActivityThreadHandlerCallback(mH));
		

我们的CallBack的部分代码，具体替换信息代码请下载demo查看

	  try {
            // 把替身恢复成真身
            Field intent = obj.getClass().getDeclaredField("intent");
            intent.setAccessible(true);
            Intent raw = (Intent) intent.get(obj);

            Intent target = raw.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
            raw.setComponent(target.getComponent());
            Log.e("Main","target = " + target);

        } catch (Exception e) {
            throw new RuntimeException("hook launch activity failed", e);
        }


#####9. 最后调用我们之前写的这些代码，越早越好，在Application里调用也行，在Activity的attachBaseContext方法中也行

开始已经给出运行图，不在贴了,如果下载demo，ChaJianHuaTest是宿主应用，请将ChaJianDemo的apk文件放入sd卡根目录，因为demo中直接写死了路径

到这里我们已经把插件apk中的一个activity加载到了宿主中，有的人会问，生命周期没说呢，其实现在我们的这个插件Activity已经有了生命周期，因为我们使用了一个占坑的Activity去欺骗系统检查，后来我们又替换了我们自己真正要启动的Activity，这个时候系统并不知道，所以系统还在傻傻的替我们管理者占坑的Activity的生命周期。有的朋友会问为什么系统可以将占坑的生命周期给了我们真正的Activity呢？

AMS与ActivityThread之间对于Activity的生命周期的交互，并没有直接使用Activity对象进行交互，而是使用一个token来标识，这个token是binder对象，因此可以方便地跨进程传递。Activity里面有一个成员变量mToken代表的就是它，token可以唯一地标识一个Activity对象，这里我们只不过替换了要启动Activity的信息，并没有替换这个token，所以系统并不知道运行的这个Activity并不是原来的那个，

	public final void scheduleLaunchActivity(Intent intent, IBinder token, int ident,
	                ActivityInfo info, Configuration curConfig, CompatibilityInfo compatInfo,
	                String referrer, IVoiceInteractor voiceInteractor, int procState, Bundle state,
	                PersistableBundle persistentState, List<ResultInfo> pendingResults,
	                List<ReferrerIntent> pendingNewIntents, boolean notResumed, boolean isForward,
	                ProfilerInfo profilerInfo) {

	}
	
	public final void scheduleResumeActivity(IBinder token, int processState,
                boolean isForward, Bundle resumeArgs) {
            updateProcessState(processState, false);
            sendMessage(H.RESUME_ACTIVITY, token, isForward ? 1 : 0);
        }

这里我们已经完美的启动了一个插件apk中的Activity，但是还是有缺点，那就是我们插件的Activity中不能使用资源，只能使用代码布局，因为我们的插件apk现在属于宿主，而宿主根本就不知道插件apk中的资源存在，而且每一个apk都有自己的资源对象存在。

给出的demo中已经解决可以加载资源的问题，但是由于篇幅的问题，会在下一篇中再详细原理，下一篇完了后我们的插件化也就彻底说完了，觉得不错希望支持一下，谢谢。敬请期待下一篇，插件化资源的动态加载及使用。

