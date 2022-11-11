package com.jizhenkeji.mainnetinspection.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.radar.EmptyRadarManager;
import com.jizhenkeji.mainnetinspection.radar.RadarAdapter;
import com.jizhenkeji.mainnetinspection.radar.RadarManager;
import com.jizhenkeji.mainnetinspection.utils.AnimationUtils;

import java.text.DecimalFormat;


public class RadarInformationWidget extends ConstraintLayout {

    private final DecimalFormat df = new DecimalFormat("#.##");

    private TextView mHorizontalDistanceText;

    private TextView mVerticalDistanceText;

    private TextView mPowerText;

    private RadarManager mRadarManager;

    private ImageView radar;

    public RadarInformationWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayoutUI();
    }

    private void initLayoutUI(){
        LayoutInflater.from(getContext()).inflate(R.layout.widget_radar_information, this, true);
        mHorizontalDistanceText = findViewById(R.id.horizontalDistanceToLine);
        mVerticalDistanceText = findViewById(R.id.verticalDistanceToLine);
        radar = findViewById(R.id.radar);
        mPowerText = findViewById(R.id.power);
        mRadarManager = RadarAdapter.getRadarManager();
        dispatchRadarHandleTransaction();
    }

    private void dispatchRadarHandleTransaction(){
        postDelayed(() -> {
            if(mRadarManager.getLastDelay() > 1000||mRadarManager.getLastDelay()==0) {
                mHorizontalDistanceText.setTextColor(ColorStateList.valueOf(0xFFFFFFFF));
                mVerticalDistanceText.setTextColor(ColorStateList.valueOf(0xFFFFFFFF));
                mPowerText.setTextColor(ColorStateList.valueOf(0xFFFFFFFF));
            }else {
                AnimationUtils.flicker(radar);
                if(mRadarManager.getLineX() <= 2.5f) {
                    mHorizontalDistanceText.setTextColor(ColorStateList.valueOf(0xFFFC0803));
                    mVerticalDistanceText.setTextColor(ColorStateList.valueOf(0xFFFC0803));
                    mPowerText.setTextColor(ColorStateList.valueOf(0xFFFC0803));
                }else {
                    mHorizontalDistanceText.setTextColor(ColorStateList.valueOf(0xFF3DDE2B));
                    mVerticalDistanceText.setTextColor(ColorStateList.valueOf(0xFF3DDE2B));
                    mPowerText.setTextColor(ColorStateList.valueOf(0xFF3DDE2B));
                }
            }
            mHorizontalDistanceText.setText("水平：" + df.format(mRadarManager.getLineX()) + "m");
            mVerticalDistanceText.setText("垂直：" + df.format(mRadarManager.getLineY()) + "m");
            mPowerText.setText("电量：" + df.format(mRadarManager.getElectricQuantity()) + "%");
            dispatchRadarHandleTransaction();
        }, 100);

//        mRadarManager.radarCall(new RadarManager.RadarCall() {
//            @Override
//            public void radarDataCallBack(float x, float y, float electric, long mLastUpdateTime) {
//                if(System.currentTimeMillis()-mLastUpdateTime > 1000) {
//                    mHorizontalDistanceText.setTextColor(ColorStateList.valueOf(0xFFFFFFFF));
//                    mVerticalDistanceText.setTextColor(ColorStateList.valueOf(0xFFFFFFFF));
//                    mPowerText.setTextColor(ColorStateList.valueOf(0xFFFFFFFF));
//                }else {
//                    AnimationUtils.flicker(radar);
//                    if(x <= 2.5f) {
//                        mHorizontalDistanceText.setTextColor(ColorStateList.valueOf(0xFFFC0803));
//                        mVerticalDistanceText.setTextColor(ColorStateList.valueOf(0xFFFC0803));
//                        mPowerText.setTextColor(ColorStateList.valueOf(0xFFFC0803));
//                    }else {
//                        mHorizontalDistanceText.setTextColor(ColorStateList.valueOf(0xFF3DDE2B));
//                        mVerticalDistanceText.setTextColor(ColorStateList.valueOf(0xFF3DDE2B));
//                        mPowerText.setTextColor(ColorStateList.valueOf(0xFF3DDE2B));
//                    }
//                }
//                mHorizontalDistanceText.setText("水平：" + df.format(x) + "m");
//                mVerticalDistanceText.setText("垂直：" + df.format(y) + "m");
//                mPowerText.setText("电量：" + df.format(electric) + "%");
//            }
//        });

    }

}

