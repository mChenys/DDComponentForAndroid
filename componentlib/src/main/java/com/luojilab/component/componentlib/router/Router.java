package com.luojilab.component.componentlib.router;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.luojilab.component.componentlib.applicationlike.IApplicationLike;

import java.util.HashMap;

/**
 * Center router, works for component-dynamic-load/remove and services add/remove/get
 * Created by mrzhang on 2017/6/15.
 */
public class Router {

    private HashMap<String, Object> services = new HashMap<>();
    //注册的组件的集合
    private static HashMap<String, IApplicationLike> components = new HashMap<>();

    private static volatile Router instance;

    private Router() {
    }

    public static Router getInstance() {
        if (instance == null) {
            synchronized (Router.class) {
                if (instance == null) {
                    instance = new Router();
                }
            }
        }
        return instance;
    }

    //==================服务操作相关========================================
    public synchronized void addService(String serviceName, Object serviceImpl) {
        if (serviceName == null || serviceImpl == null) {
            return;
        }
        services.put(serviceName, serviceImpl);
    }

    public synchronized Object getService(String serviceName) {
        if (serviceName == null) {
            return null;
        }
        return services.get(serviceName);
    }

    public synchronized void removeService(String serviceName) {
        if (serviceName == null) {
            return;
        }
        services.remove(serviceName);
    }
    //==================服务操作相关========================================

    /**
     * 注册组件
     *
     * @param classname 组件名，对应IApplicationLike的实现类
     */
    public static void registerComponent(@Nullable String classname) {
        if (TextUtils.isEmpty(classname)) {
            return;
        }
        if (components.keySet().contains(classname)) {
            return;
        }
        try {
            // 获取Class对象
            Class clazz = Class.forName(classname);
            // 创建实列
            IApplicationLike applicationLike = (IApplicationLike) clazz.newInstance();
            // 调用onCreate方法
            applicationLike.onCreate();
            // 保存缓存中
            components.put(classname, applicationLike);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 反注册组件
     *
     * @param classname 组件名，对应IApplicationLike的实现类
     */
    public static void unregisterComponent(@Nullable String classname) {
        if (TextUtils.isEmpty(classname)) {
            return;
        }
        if (components.keySet().contains(classname)) {
            components.get(classname).onStop();
            components.remove(classname);
            return;
        }
        try {
            Class clazz = Class.forName(classname);
            IApplicationLike applicationLike = (IApplicationLike) clazz.newInstance();
            // 调用onStop方法
            applicationLike.onStop();
            // 从缓存中移除
            components.remove(classname);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
