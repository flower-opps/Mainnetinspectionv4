package com.jizhenkeji.mainnetinspection.widget;

import android.content.Context;
import android.util.AttributeSet;

import java.text.DecimalFormat;

import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.keysdk.callback.KeyListener;

public class RemainingFlightTimeWidget extends androidx.appcompat.widget.AppCompatTextView {

    public RemainingFlightTimeWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initRemainingFlightTimeListener();
    }

    private KeyListener mkeyListener = new KeyListener() {
        @Override
        public void onValueChange(Object oldValue, Object newValue) {
            int remainingFlightTime = (int) newValue;
            post(() -> {
                setText(getRemainFlightTimeDescription(remainingFlightTime));
            });
        }
    };

    private void initRemainingFlightTimeListener(){
        FlightControllerKey remainFlightTimeKey = FlightControllerKey.create(FlightControllerKey.REMAINING_FLIGHT_TIME);
        KeyManager.getInstance().addListener(remainFlightTimeKey, mkeyListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyManager.getInstance().removeListener(mkeyListener);
    }

    private String getRemainFlightTimeDescription(int seconds){
        DecimalFormat df = new DecimalFormat("##");
        String timeDescription = df.format(seconds / 3600) + "时"
                + df.format(seconds % 3600 / 60) + "分" + df.format(seconds % 3600 % 60) + "秒";
        return timeDescription;
    }

}
