package com.luojilab.component.componentlib.service;

import android.util.Log;
import android.util.LruCache;

import com.luojilab.component.componentlib.router.ISyringe;

import java.util.ArrayList;
import java.util.List;


/**
 * <p><b>Package:</b> com.luojilab.component.componentlib.di.serviceimpl </p>
 * <p><b>Project:</b> DDComponentForAndroid </p>
 * <p><b>Classname:</b> DepInjectServiceImpl </p>
 * <p><b>Description:</b> implement of {@link AutowiredService},used to fetch
 * data from bundles in the intent</p>
 * Created by leobert on 2017/9/18.
 */

public class AutowiredServiceImpl implements AutowiredService {
    // 缓存
    private LruCache<String, ISyringe> classCache = new LruCache<>(50);
    private List<String> blackList = new ArrayList<>();

    //attention! make sure this keeps same with the one in AutowiredProcessor
    // 自动生成类用来注入参数的class的后缀名称
    private static final String SUFFIX_AUTOWIRED = "$$Router$$Autowired";

    @Override
    public void autowire(Object instance) {
        // 目标类名
        String className = instance.getClass().getName();
        try {
            // 判断是否已经注入过
            if (!blackList.contains(className)) {
                // 从缓存中获取ISyringe接口实现
                ISyringe autowiredHelper = classCache.get(className);
                if (null == autowiredHelper) {  // No cache.
                    // 获取不到，说明不存在缓存，通过反射创建目标类，例如ShareActivity$$Router$$Autowired
                    autowiredHelper = (ISyringe) Class.forName(instance.getClass().getName() + SUFFIX_AUTOWIRED)
                            .getConstructor().newInstance();
                }
                // 开始注入实例对象
                autowiredHelper.inject(instance);
                // 保存缓存中
                classCache.put(className, autowiredHelper);
            } else {
                // TODO: 2017/12/21 change to specific log system
                Log.d("[DDComponent]", "[autowire] " + className + "is in blacklist, ignore data inject");
            }
        } catch (Exception ex) {
            if (ex instanceof NullPointerException) { // may define custom exception better
                throw new NullPointerException(ex.getMessage());
            }
            ex.printStackTrace();
            blackList.add(className);    // This instance need not autowired.
        }
    }
}
