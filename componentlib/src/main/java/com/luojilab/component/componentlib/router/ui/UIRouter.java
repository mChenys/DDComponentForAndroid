package com.luojilab.component.componentlib.router.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.luojilab.router.facade.utils.RouteUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Singleton implement of {@link IUIRouter}
 * provides services for UI-Component
 * <p>
 * Created by mrzhang on 2017/6/20.
 */

public class UIRouter implements IUIRouter {
    // IComponentRouter缓存，避免多次反射创建实例，每个模块只有一个，由编译器自动生成
    // key=com.luojilab.gen.router.<hostName>UiRouter，value=IComponentRouter接口实现，例如AppUiRouter
    private static Map<String, IComponentRouter> routerInstanceCache = new HashMap<>();
    // 所有IComponentRouter的集合
    private List<IComponentRouter> uiRouters = new ArrayList<>();
    // 优先级map，key=IComponentRouter，value=优先级
    private HashMap<IComponentRouter, Integer> priorities = new HashMap<>();

    private static volatile UIRouter instance;

    private UIRouter() {
    }

    public static UIRouter getInstance() {
        if (instance == null) {
            synchronized (UIRouter.class) {
                if (instance == null) {
                    instance = new UIRouter();
                }
            }
        }
        return instance;
    }

    /**
     * 组册UIRouter类
     * @param router
     * @param priority 优先级
     */
    @Override
    public void registerUI(IComponentRouter router, int priority) {
        if (priorities.containsKey(router) && priority == priorities.get(router)) {
            // 已存在优先级map中，不处理
            return;
        }
        // 移除已存在的IComponentRouter
        removeOldUIRouter(router);
        int i = 0;
        for (IComponentRouter temp : uiRouters) {
            Integer tp = priorities.get(temp);
            if (tp == null || tp <= priority) {
                break;
            }
            i++; // 根据优先级来获取应该存放在uiRouters列表中的index
        }
        // 保存到列表中
        uiRouters.add(i, router);
        // 同时保存到优先级map中
        priorities.put(router, priority);
    }

    @Override
    public void registerUI(IComponentRouter router) {
        registerUI(router, PRIORITY_NORMAL);
    }

    @Override
    public void registerUI(String host) {
        IComponentRouter router = fetch(host);
        if (router != null) {
            registerUI(router, PRIORITY_NORMAL);
        }
    }

    /**
     * 按优先级注册IComponentRouter
     * @param host
     * @param priority
     */
    @Override
    public void registerUI(String host, int priority) {
        IComponentRouter router = fetch(host);
        if (router != null) {
            registerUI(router, priority);
        }
    }

    @Override
    public void unregisterUI(IComponentRouter router) {
        for (int i = 0; i < uiRouters.size(); i++) {
            if (router == uiRouters.get(i)) {
                uiRouters.remove(i);
                priorities.remove(router);
                break;
            }
        }
    }

    /**
     * 反注册IComponentRouter
     * @param host
     */
    @Override
    public void unregisterUI(String host) {
        // 根据host查找已生成的IComponentRouter实现类
        IComponentRouter router = fetch(host);
        if (router != null) {
            unregisterUI(router);
        }
    }

    //====================页面跳转处理开始==================
    @Override
    public boolean openUri(Context context, String url, Bundle bundle) {
        return openUri(context, url, bundle, 0);
    }

    @Override
    public boolean openUri(Context context, Uri uri, Bundle bundle) {
        return openUri(context, uri, bundle, 0);
    }

    @Override
    public boolean openUri(Context context, String url, Bundle bundle, Integer requestCode) {
        url = url.trim();
        if (!TextUtils.isEmpty(url)) {
            if (!url.contains("://") &&
                    (!url.startsWith("tel:") ||
                            !url.startsWith("smsto:") ||
                            !url.startsWith("file:"))) {
                url = "http://" + url;
            }
            Uri uri = Uri.parse(url);
            return openUri(context, uri, bundle, requestCode);
        }
        return true;
    }

    @Override
    public boolean openUri(Context context, Uri uri, Bundle bundle, Integer requestCode) {
        // 遍历IComponentRouter列表
        for (IComponentRouter temp : uiRouters) {
            try {
                // 校验uri和跳转，具体要看父类BaseCompRouter
                if (temp.verifyUri(uri) && temp.openUri(context, uri, bundle, requestCode)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    //====================页面跳转处理结束==================

    @Override
    public boolean verifyUri(Uri uri) {
        for (IComponentRouter temp : uiRouters) {
            if (temp.verifyUri(uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从IComponentRouter列表中移除IComponentRouter
     * @param router
     */
    private void removeOldUIRouter(IComponentRouter router) {
        Iterator<IComponentRouter> iterator = uiRouters.iterator();
        while (iterator.hasNext()) {
            IComponentRouter tmp = iterator.next();
            if (tmp == router) {
                iterator.remove();
                priorities.remove(tmp);
            }
        }
    }

    /**
     * 获取IComponentRouter的实现类，是由编译器自动生成的，命名格式：HostnameUiRouter
     * @param host
     * @return
     */
    private IComponentRouter fetch(@NonNull String host) {

        String path = RouteUtils.genHostUIRouterClass(host);

        if (routerInstanceCache.containsKey(path))
            return routerInstanceCache.get(path); // 缓存有

        try {
            // 缓存无，通过反射创建
            Class cla = Class.forName(path);
            IComponentRouter instance = (IComponentRouter) cla.newInstance();
            // 保存缓存
            routerInstanceCache.put(path, instance);
            return instance;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }
}
