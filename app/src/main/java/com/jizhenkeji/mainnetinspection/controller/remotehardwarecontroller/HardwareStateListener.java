package com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 遥控器硬件组件状态监听注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HardwareStateListener {

    HardwareType type();

}
