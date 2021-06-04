package com.luojilab.component.componentlib.router.ui;

/**
 * Created by mrzhang on 2017/12/19.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseCompRouter implements IComponentRouter {
    // key=路径名，例如：/main， value=Class，例如：MainActivity.class
    protected Map<String, Class> routeMapper = new HashMap<String, Class>();
    // 参数类map，key=Class，例如：MainActivity.class，value=Map<字段名, 字段类型>
    protected Map<Class, Map<String, Integer>> paramsMapper = new HashMap<>();

    protected boolean hasInitMap = false; // 标记是否初始化路由表routeMapper

    /**
     * 子类实现，返回模块的名称，例如app
     * @return
     */
    protected abstract String getHost();

    /**
     * 重点方法，自动生成的子类，例如AppUiRouter会重写此方法，来初始化路由表（往routeMapper中添加内容）
     */
    protected void initMap() {
        hasInitMap = true;
    }

    //=====================跳转处理开始==============================
    @Override
    public boolean openUri(Context context, String url, Bundle bundle) {
        if (TextUtils.isEmpty(url) || context == null) {
            return true;
        }
        return openUri(context, Uri.parse(url), bundle, 0);
    }

    @Override
    public boolean openUri(Context context, Uri uri, Bundle bundle) {
        return openUri(context, uri, bundle, 0);
    }

    @Override
    public boolean openUri(Context context, String url, Bundle bundle, Integer requestCode) {
        if (TextUtils.isEmpty(url) || context == null) {
            return true;
        }
        return openUri(context, Uri.parse(url), bundle, requestCode);
    }

    @Override
    public boolean openUri(Context context, Uri uri, Bundle bundle, Integer requestCode) {
        if (!hasInitMap) {
            initMap();
        }
        if (uri == null || context == null) {
            return true;
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!getHost().equals(host)) {
            // 校验host是否相同，不同则不处理
            return false;
        }
        // 从uri中获取路径集合
        List<String> pathSegments = uri.getPathSegments();
        // 拼接完整path
        String path = "/" + TextUtils.join("/", pathSegments);
        if (routeMapper.containsKey(path)) {
            // 如果路径存在路由表，那么取出目标class
            Class target = routeMapper.get(path);
            if (bundle == null) {
                bundle = new Bundle();
            }
            // 解析uri中的参数，key=value
            Map<String, String> params = com.luojilab.component.componentlib.utils.UriUtils.parseParams(uri);
            // 从参数map中取出map[key=字段名称，value=Integer]
            Map<String, Integer> paramsType = paramsMapper.get(target);
            // 给bundle赋值
            com.luojilab.component.componentlib.utils.UriUtils.setBundleValue(bundle, params, paramsType);
            // 创建intent
            Intent intent = new Intent(context, target);
            // 添加bundle
            intent.putExtras(bundle);
            // 进行activity的跳转
            if (requestCode > 0 && context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, requestCode);
                return true;
            }
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    //=====================跳转处理结束==============================

    /**
     * 校验Uri，首先会判断host是否相同，然后再判断path是否存在路由表routeMapper中
     * @param uri
     * @return
     */
    @Override
    public boolean verifyUri(Uri uri) {
        String host = uri.getHost();
        if (!getHost().equals(host)) {
            // 对比host是否相同，不相同不处理
            return false;
        }
        if (!hasInitMap) {
            // 没有初始化路由表，则初始化
            initMap();
        }
        // 拼接完整的path
        List<String> pathSegments = uri.getPathSegments();
        String path = "/" + TextUtils.join("/", pathSegments);
        return routeMapper.containsKey(path);
    }
}

