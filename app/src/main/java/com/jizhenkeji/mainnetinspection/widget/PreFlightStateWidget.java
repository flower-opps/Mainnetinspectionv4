package com.jizhenkeji.mainnetinspection.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import dji.common.error.DJIError;
import dji.common.logics.warningstatuslogic.WarningStatusItem;
import dji.keysdk.DiagnosticsKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.GetCallback;

/**
 * 无人机状态显示组件
 */
public class PreFlightStateWidget extends androidx.appcompat.widget.AppCompatButton {

    private Context mContext;

    public PreFlightStateWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initView();
        initDiagnosticsListener();
    }

    /**
     * 初始化视图
     */
    private void initView(){
        /* 设置最小默认尺寸 */
        setMinimumWidth(GlobalUtils.dpToPx(70));
        int horizontalPadding = GlobalUtils.dpToPx(10);
        setPadding(horizontalPadding, 0, horizontalPadding, 0);
        /* 设置背景透明 */
        setBackgroundColor(0x00FFFFFF);
        /* 设置文字 */
        setTypeface(null, Typeface.BOLD);
        /* 获取状态信息 */
        DiagnosticsKey diagnosticsKey = DiagnosticsKey.create(DiagnosticsKey.SYSTEM_STATUS);
        KeyManager.getInstance().getValue(diagnosticsKey, new GetCallback() {
            @Override
            public void onSuccess(Object item) {
                if(!(item instanceof WarningStatusItem)){
                    return;
                }
                post(() -> {
                    updateWarningStatus((WarningStatusItem) item);
                });
            }
            @Override
            public void onFailure(DJIError djiError) {}
        });
    }

    /**
     * 初始化诊断信息监听器
     */
    private void initDiagnosticsListener(){
        DiagnosticsKey diagnosticsKey = DiagnosticsKey.create(DiagnosticsKey.SYSTEM_STATUS);
        KeyManager keyManager = KeyManager.getInstance();
        keyManager.addListener(diagnosticsKey, (Object oldValue, Object newValue) -> {
            if(!(newValue instanceof WarningStatusItem)){
                return;
            }
            post(() -> {
                updateWarningStatus((WarningStatusItem) newValue);
            });
        });
    }

    /**
     * 是否设置返航点标志位
     */
    private boolean isSetHomeLocation = false;

    /**
     * 更新诊断信息
     */
    private void updateWarningStatus(WarningStatusItem warningStatusItem){
        switch (warningStatusItem.getWarningLevel()){
            case GOOD:
                setTextColor(0xFFFFFFFF);
                /* 设置返航点 */
                if(!isSetHomeLocation){
                    KeyManager.getInstance().performAction(FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_USING_CURRENT_AIRCRAFT_LOCATION), new ActionCallback() {
                        @Override
                        public void onSuccess() {
                            isSetHomeLocation = true;
                        }

                        @Override
                        public void onFailure(DJIError djiError) {}
                    });
                }
                break;
            case WARNING:
                setTextColor(0xFFFFCF44);
                break;
            case ERROR:
                setTextColor(0xFFFF0000);
                break;
        }
        setText(warningStatusItem.getMessage());
    }

}
