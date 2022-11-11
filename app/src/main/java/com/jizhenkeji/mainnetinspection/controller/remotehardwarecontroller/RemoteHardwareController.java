package com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.ComponentController;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dji.common.Stick;
import dji.common.remotecontroller.HardwareState;
import dji.keysdk.KeyManager;
import dji.keysdk.RemoteControllerKey;

/**
 * 遥控器设备硬件部件状态控制器
 */
public class RemoteHardwareController extends ComponentController {

    /**
     * 长按事务触发周期，单位：毫秒
     */
    private final int LONG_CLICK_ACTIVATE_CYCLE = 100;

    /**
     * 长按事务触发事件，单位：毫秒
     */
    private final int LONG_CLICK_ACTIVATE_TIME = 700;

    /**
     * 按下C1按钮事件
     */
    private final int EVENT_C1BUTTON_DOWN = 1;

    /**
     * 抬起C1按钮事件
     */
    private final int EVENT_C1BUTTON_UP = 2;

    /**
     * 长按C1按钮事件
     */
    private final int EVENT_C1BUTTON_LONG_DOWN = 3;

    /**
     * 按下C2按钮事件
     */
    private final int EVENT_C2BUTTON_DOWN = 4;

    /**
     * 抬起C2按钮事件
     */
    private final int EVENT_C2BUTTON_UP = 5;

    /**
     * 长按C2按钮事件
     */
    private final int EVENT_C2BUTTON_LONG_DOWN = 6;

    /**
     * 按下Playback按钮事件
     */
    private final int EVENT_PLAYBACK_DOWN = 7;

    /**
     * 抬起Playback按钮事件
     */
    private final int EVENT_PLAYBACK_UP = 8;

    /**
     * 长按Playback按钮事件
     */
    private final int EVENT_PLAYBACK_LONG_DOWN = 9;

    /**
     * 左侧滚轮事件
     */
    private final int EVENT_LEFT_DIAL = 10;

    /**
     * 左侧摇杆事件
     */
    private final int EVENT_LEFT_STICK = 11;

    /**
     * 五维按钮上推事件
     */
    private final int EVENT_FIVEDTOP_STICK = 51;

    /**
     * 五维按钮下推事件
     */
    private final int EVENT_FIVEDBOTTOM_STICK = 52;

    /**
     * 五维按钮左推事件
     */
    private final int EVENT_FIVEDLEFT_STICK = 53;

    /**
     * 五维按钮右推事件
     */
    private final int EVENT_FIVEDRIGHT_STICK = 54;

    /**
     * 五维按钮按下事件
     */
    private final int EVENT_FIVEDDOWN_STICK = 550;

    /**
     * 五维按钮抬起事件
     */
    private final int EVENT_FIVEDUP_STICK = 551;

    /**
     * 右侧摇杆事件
     */
    private final int EVENT_RIGHT_STICK = 12;



    public static RemoteHardwareController getInstance() {
        return Controler.INSTANCE;
    }

    private static class Controler {
        private static final RemoteHardwareController INSTANCE = new RemoteHardwareController();
    }

    private RemoteHardwareController() {
    }

    /**
     * 内部事务分发处理线程
     */
    private HandlerThread transactionDispatchThread;

    /**
     * 事务处理者
     */
    private Handler mHandler;

    /**
     * 事务处理回调
     */
    private Handler.Callback mCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_FIVEDTOP_STICK:
                    Log.d("djdjdj",msg+"");
                    break;
                case EVENT_FIVEDBOTTOM_STICK:
                    Log.d("djdjdj",msg+"");
                    break;
                case EVENT_FIVEDLEFT_STICK:
                    Log.d("djdjdj",msg+"");
                    break;
                case EVENT_FIVEDRIGHT_STICK:
                    Log.d("djdjdj",msg+"");
                    break;
                case EVENT_FIVEDDOWN_STICK:
                    dispatchHardwareEvent(HardwareType.FIVED_STICK, ButtonEvent.DOWN);
                    sendDelayMessage(EVENT_FIVEDDOWN_STICK, LONG_CLICK_ACTIVATE_TIME);
                    break;
                case EVENT_FIVEDUP_STICK:
                    dispatchHardwareEvent(HardwareType.FIVED_STICK, ButtonEvent.UP);
                    sendDelayMessage(EVENT_FIVEDUP_STICK, LONG_CLICK_ACTIVATE_TIME);
                    break;
                case EVENT_C1BUTTON_DOWN:
                    dispatchHardwareEvent(HardwareType.C1BUTTON, ButtonEvent.DOWN);
                    sendDelayMessage(EVENT_C1BUTTON_LONG_DOWN, LONG_CLICK_ACTIVATE_TIME);
                    break;
                case EVENT_C1BUTTON_LONG_DOWN:
                    dispatchHardwareEvent(HardwareType.C1BUTTON, ButtonEvent.LONG_DOWN);
                    sendDelayMessage(EVENT_C1BUTTON_LONG_DOWN, LONG_CLICK_ACTIVATE_CYCLE);
                    break;
                case EVENT_C1BUTTON_UP:
                    dispatchHardwareEvent(HardwareType.C1BUTTON, ButtonEvent.UP);
                    removeMessage(EVENT_C1BUTTON_LONG_DOWN);
                    break;
                case EVENT_C2BUTTON_DOWN:
                    dispatchHardwareEvent(HardwareType.C2BUTTON, ButtonEvent.DOWN);
                    sendDelayMessage(EVENT_C2BUTTON_LONG_DOWN, LONG_CLICK_ACTIVATE_TIME);
                    break;
                case EVENT_C2BUTTON_LONG_DOWN:
                    dispatchHardwareEvent(HardwareType.C2BUTTON, ButtonEvent.LONG_DOWN);
                    sendDelayMessage(EVENT_C2BUTTON_LONG_DOWN, LONG_CLICK_ACTIVATE_CYCLE);
                    break;
                case EVENT_C2BUTTON_UP:
                    dispatchHardwareEvent(HardwareType.C2BUTTON, ButtonEvent.UP);
                    removeMessage(EVENT_C2BUTTON_LONG_DOWN);
                    break;
                case EVENT_PLAYBACK_DOWN:
                    dispatchHardwareEvent(HardwareType.PLAYBACK, ButtonEvent.DOWN);
                    sendDelayMessage(EVENT_PLAYBACK_LONG_DOWN, LONG_CLICK_ACTIVATE_TIME);
                    break;
                case EVENT_PLAYBACK_LONG_DOWN:
                    dispatchHardwareEvent(HardwareType.PLAYBACK, ButtonEvent.LONG_DOWN);
                    sendDelayMessage(EVENT_PLAYBACK_LONG_DOWN, LONG_CLICK_ACTIVATE_CYCLE);
                    break;
                case EVENT_PLAYBACK_UP:
                    dispatchHardwareEvent(HardwareType.PLAYBACK, ButtonEvent.UP);
                    removeMessage(EVENT_PLAYBACK_LONG_DOWN);
                    break;
                case EVENT_LEFT_DIAL:
                    dispatchHardwareEvent(HardwareType.LEFT_DIAL, msg.obj);
                    break;
                case EVENT_LEFT_STICK:
                    dispatchHardwareEvent(HardwareType.LEFT_STICK, msg.obj);
                    break;
                case EVENT_RIGHT_STICK:
                    dispatchHardwareEvent(HardwareType.RIGHT_STICK, msg.obj);
                    break;
                default:
                    return false;
            }
            return true;
        }
    };

    @Override
    public JZIError init() {
        /* 开启消息循环处理 */
        transactionDispatchThread = new HandlerThread("transactionDispatchThread");
        transactionDispatchThread.setDaemon(true);
        transactionDispatchThread.start();
        mHandler = new Handler(transactionDispatchThread.getLooper(), mCallback);
        /* 初始化硬件状态监听 */
        initHardwareStateListener();
        return null;
    }

    /**
     * 初始化遥控器硬件状态监听
     */
    private void initHardwareStateListener() {
        KeyManager keyManager = KeyManager.getInstance();
        /* 五维按钮事件 */
        keyManager.addListener(RemoteControllerKey.create(RemoteControllerKey.FIVE_D_BUTTON), (Object oldValue, Object newValue) -> {
            HardwareState.FiveDButton fiveDButton = (HardwareState.FiveDButton) newValue;
            sendMessage(fiveDButton.isClicked() ? EVENT_FIVEDDOWN_STICK : EVENT_FIVEDUP_STICK);
        });

        /* 监听C1按钮 */
        keyManager.addListener(RemoteControllerKey.create(RemoteControllerKey.CUSTOM_BUTTON_1), (Object oldValue, Object newValue) -> {
            HardwareState.Button c1Button = (HardwareState.Button) newValue;
            sendMessage(c1Button.isClicked() ? EVENT_C1BUTTON_DOWN : EVENT_C1BUTTON_UP);
        });
        /* 监听C2按钮 */
        keyManager.addListener(RemoteControllerKey.create(RemoteControllerKey.CUSTOM_BUTTON_2), (Object oldValue, Object newValue) -> {
            HardwareState.Button c2Button = (HardwareState.Button) newValue;
            sendMessage(c2Button.isClicked() ? EVENT_C2BUTTON_DOWN : EVENT_C2BUTTON_UP);
        });
        /* 监听Playback按钮 */
        keyManager.addListener(RemoteControllerKey.create(RemoteControllerKey.PLAYBACK_BUTTON), (Object oldValue, Object newValue) -> {
            HardwareState.Button playbackButton = (HardwareState.Button) newValue;
            sendMessage(playbackButton.isClicked() ? EVENT_PLAYBACK_DOWN : EVENT_PLAYBACK_UP);
        });
        /* 监听左侧滚轮 */
        keyManager.addListener(RemoteControllerKey.create(RemoteControllerKey.LEFT_DIAL), (Object oldValue, Object newValue) -> {
            int leftDialValue = (int) newValue;
            sendMessageWithObj(EVENT_LEFT_DIAL, leftDialValue);
        });
        /* 监听左侧摇杆 */
        keyManager.addListener(RemoteControllerKey.create(RemoteControllerKey.LEFT_STICK_VALUE), (Object oldValue, Object newValue) -> {
            Stick leftStick = (Stick) newValue;
            sendMessageWithObj(EVENT_LEFT_STICK, leftStick);
        });
        /* 监听右侧摇杆 */
        keyManager.addListener(RemoteControllerKey.create(RemoteControllerKey.RIGHT_STICK_VALUE), (Object oldValue, Object newValue) -> {
            Stick rightStick = (Stick) newValue;
            sendMessageWithObj(EVENT_RIGHT_STICK, rightStick);
        });
    }

    /**
     * 分发硬件组件状态变化事件
     *
     * @param type    当前触发事件所属的硬件类型
     * @param objects 反馈的格外参数
     */
    private void dispatchHardwareEvent(HardwareType type, Object... objects) {
        for (WeakReference<Object> listener : mListeners) {
            /* 获取监听对象 */
            Object object = listener.get();
            if (object == null) {
                /* 监听对象已被垃圾回收期回收 */
                continue;
            }
            /* 通过反射将硬件触发事件分发给监听对象 */
            Method[] methods = object.getClass().getMethods();
            for (Method method : methods) {
                try {
                    HardwareStateListener hardwareStateListener = method.getAnnotation(HardwareStateListener.class);
                    if (hardwareStateListener == null) {
                        continue;
                    }
                    if (hardwareStateListener.type() == type) {
                        method.invoke(object, objects);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 存储所有监听硬件状态的虚对象
     */
    private List<WeakReference<Object>> mListeners = new ArrayList<>();

    /**
     * 添加硬件组件状态监听
     *
     * @param object 包含监听注释{@link HardwareStateListener}方法的实例
     */
    public void addHardwareStateListener(Object object) {
        WeakReference<Object> weakReferenceObject = new WeakReference<>(object);
        mListeners.add(weakReferenceObject);
        cleanWeakReferenceList();
    }

    /**
     * 移除硬件组件状态监听
     *
     * @param object
     */
    public void removeHardwareStateListener(Object object) {
        Iterator<WeakReference<Object>> listenerIterator = mListeners.iterator();
        while (listenerIterator.hasNext()) {
            WeakReference<Object> compareObject = listenerIterator.next();
            if (compareObject.get() != null && compareObject.get() == object) {
                listenerIterator.remove();
            }
        }
    }

    /**
     * 删除弱引用列表中已被垃圾回收器回收的对象
     */
    private void cleanWeakReferenceList() {
        Iterator<WeakReference<Object>> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<Object> weakReferenceObject = iterator.next();
            if (weakReferenceObject.get() == null) {
                iterator.remove();
            }
        }
    }

    /**
     * 发送事件消息
     *
     * @param what
     */
    private void sendMessage(int what) {
        mHandler.sendEmptyMessage(what);
    }

    /**
     * 移除事件消息
     *
     * @param what
     */
    private void removeMessage(int what) {
        mHandler.removeMessages(what);
    }

    /**
     * 发送带对象的事件消息
     *
     * @param what
     * @param obj
     */
    private void sendMessageWithObj(int what, Object obj) {
        Message eventMessage = mHandler.obtainMessage(what, obj);
        mHandler.sendMessage(eventMessage);
    }

    /**
     * 发送延迟消息
     *
     * @param what
     * @param delay
     */
    private void sendDelayMessage(int what, long delay) {
        mHandler.sendEmptyMessageDelayed(what, delay);
    }

    @Override
    public void release() {

    }

}
