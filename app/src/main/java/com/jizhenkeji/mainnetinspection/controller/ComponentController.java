package com.jizhenkeji.mainnetinspection.controller;

import com.jizhenkeji.mainnetinspection.common.JZIError;
/**
 * 各控制组件的抽象基类
 */
public abstract class ComponentController {

    public abstract JZIError init();

    public void release(){}

}
