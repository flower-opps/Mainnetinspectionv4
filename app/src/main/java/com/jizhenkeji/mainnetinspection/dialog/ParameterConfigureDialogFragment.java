package com.jizhenkeji.mainnetinspection.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableFloat;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.adapter.MissionSpinnerAdapter;
import com.jizhenkeji.mainnetinspection.common.InspectionMode;
import com.jizhenkeji.mainnetinspection.common.WirePhaseNumber;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.ButtonEvent;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.HardwareStateListener;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.HardwareType;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.RemoteHardwareController;
import com.jizhenkeji.mainnetinspection.databinding.DialogParameterConfigureStep1Binding;
import com.jizhenkeji.mainnetinspection.databinding.DialogParameterConfigureStep2Binding;
import com.jizhenkeji.mainnetinspection.databinding.DialogParameterConfigureStep3Binding;
import com.jizhenkeji.mainnetinspection.databinding.DialogParameterConfigureStep4Binding;
import com.jizhenkeji.mainnetinspection.databinding.DialogParameterConfigureStep5Binding;
import com.jizhenkeji.mainnetinspection.databinding.DialogParameterConfigureStep6Binding;
import com.jizhenkeji.mainnetinspection.databinding.DialogParameterConfigureStep7Binding;
import com.jizhenkeji.mainnetinspection.databinding.DialogParameterConfigureStep8Binding;
import com.jizhenkeji.mainnetinspection.model.AppDatabase;
import com.jizhenkeji.mainnetinspection.model.entity.MissionEntity;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.common.CommonCallbackWith;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;
import com.wx.wheelview.adapter.ArrayWheelAdapter;
import com.wx.wheelview.widget.WheelView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParameterConfigureDialogFragment extends DialogFragment {

    public static final String KEY_MISSION = "KEY_MISSION";

    public static final String KEY_INSPECTION_MODE = "KEY_INSPECTION_MODE";

    public static final String KEY_PHASE_NUMBER = "KEY_PHASE_NUMBER";

    public static final String KEY_START_TOWER_NUM = "KEY_START_TOWER_NUM";

    public static final String KEY_END_TOWER_NUM = "KEY_END_TOWER_NUM";

    public static final String KEY_FLIGHT_SPEED = "KEY_FLIGHT_SPEED";

    public static final String KEY_HORIZONTAL_DISTANCE = "KEY_HORIZONTAL_DISTANCE";

    public static final String KEY_VERITICAL_DISTANCE = "KEY_VERITICAL_DISTANCE";

    public static final String KEY_TREE_BARRIER_DISTANCE = "KEY_TREE_BARRIER_DISTANCE";

    /**
     * 对话框默认宽度
     */
    private final int DIALOG_WIDTH = 450;

    /**
     * 对话框默认高度
     */
    private final int DIALOG_HEIGHT = 350;

    /**
     * 用于执行巡检任务的任务实体
     */
    private MissionWithTowers mMissionWithTowers;

    /**
     * 用于执行巡检任务的巡检模式
     */
    private InspectionMode mInspectionMode;

    /**
     * 当前执行巡检导线的相号
     */
    private WirePhaseNumber mWirePhaseNumber;

    /**
     * 当前执行巡检任务的起始塔号
     */
    private ObservableInt mStartTowerNum = new ObservableInt(0);

    /**
     * 当前执行巡检任务的塔数量
     */
    private ObservableInt mTowerSize;

    /**
     * 当前执行巡检任务的终止塔号
     */
    private ObservableField<String> mEndTowerNum = new ObservableField<>();

    /**
     * 当前执行巡检任务的巡检速度
     */
    private ObservableFloat mFlightSpeed = new ObservableFloat(1f);

    /**
     * 当前执行巡检任务的水平离线距离
     */
    private ObservableFloat mHorizontalDistance = new ObservableFloat(3.5f);

    /**
     * 当前执行巡检任务的垂直离线距离
     */
    private ObservableFloat mVerticalDistance = new ObservableFloat(0.2f);

    /**
     * 当前执行巡检任务的树障警报阈值
     */
    private ObservableInt mTreeBarrierDistance = new ObservableInt(7);

    @Override
    public void onStart() {
        super.onStart();
        Window dialogWindow = getDialog().getWindow();
        /* 固定窗体大小 */
        dialogWindow.setLayout(GlobalUtils.dpToPx(DIALOG_WIDTH), GlobalUtils.dpToPx(DIALOG_HEIGHT));
        /* 去除窗体模糊背景 */
        dialogWindow.setDimAmount(0);
        /* 设置窗体背景 */
        dialogWindow.setBackgroundDrawableResource(R.drawable.parameter_configure_window_background);
        /* 设置对话框不能被取消 */
        setCancelable(false);
    }

    /* 配置巡检任务视图绑定对象 */
    private DialogParameterConfigureStep1Binding mConfigureMissionBinding;

    /* 配置巡检模式视图绑定对象 */
    private DialogParameterConfigureStep2Binding mConfigureInspectionModeBinding;

    /* 配置巡检任务相号视图绑定对象 */
    private DialogParameterConfigureStep3Binding mConfigurePhaseNumberBinding;

    /* 配置巡检任务塔号视图绑定对象 */
    private DialogParameterConfigureStep4Binding mConfigureTowerNumberBinding;

    /* 配置巡检速度视图绑定对象 */
    private DialogParameterConfigureStep5Binding mConfigureFlightSpeedBinding;

    /* 配置无人机水平离线距离视图绑定对象 */
    private DialogParameterConfigureStep6Binding mConfigureHorizontalDistanceToLineBinding;

    /* 配置无人机垂直离线距离视图绑定对象 */
    private DialogParameterConfigureStep7Binding mConfigureVerticalDistanceToLineBinding;

    /* 配置无人机树障检测距离 */
    private DialogParameterConfigureStep8Binding mConfigureTreeBarrierDistanceBinding;

    /* 当前显示的弹出框编号 */

    private  volatile int nowDiaframent = 1;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RemoteHardwareController.getInstance().addHardwareStateListener(this);
        setContentView(createConfigureFlightSpeedView());
        setContentView(createConfigureHorizontalDistanceToLineView());
        setContentView(createConfigureVerticalDistanceToLineView());
        setContentView(createConfigureMissionView());
        return createConfigureMissionView();
    }

    @HardwareStateListener(type = HardwareType.C1BUTTON)
    public void c1click(ButtonEvent event){
        switch (event){
            case DOWN:
            case LONG_DOWN:
                break;
            case UP:
                switch (nowDiaframent){
                    case 2:
                        setContentView(createConfigureMissionView());
                        break;
                    case 3:
                        setContentView(createConfigurePhaseNumberView());
                        break;
                    case 4:
                        setContentView(createConfigureTowerNumberView());
                        break;
                    case 5:
                        setContentView(createConfigureFlightSpeedView());
                        break;
                    case 6:
                        setContentView(createConfigureHorizontalDistanceToLineView());
                        break;
                    case 7:
                        setContentView(createConfigureVerticalDistanceToLineView());
                        break;
                }
                break;
        }
    }

    @HardwareStateListener(type = HardwareType.C2BUTTON)
    public void c2click(ButtonEvent event){
        switch (event){
            case DOWN:
            case LONG_DOWN:
                break;
            case UP:
                switch (nowDiaframent){
                    case 1:
                        mInspectionMode = InspectionMode.WIRE_MODE;
                        setContentView(createConfigurePhaseNumberView());
                        break;
                    case 2:
                        setContentView(createConfigureTowerNumberView());
                        break;
                    case 3:
                        setContentView(createConfigureFlightSpeedView());
                        break;
                    case 4:
                        setContentView(createConfigureHorizontalDistanceToLineView());
                        break;
                    case 5:
                        setContentView(createConfigureVerticalDistanceToLineView());
                        break;
                    case 6:
                        if(mInspectionMode == InspectionMode.WIRE_TREE_MODE || mInspectionMode == InspectionMode.TREE_MODE){
                            setContentView(createConfigureTreeBarrierDistanceView());
                            mConfigureVerticalDistanceToLineBinding.next.setText(GlobalUtils.getString(R.string.next_step));
                        }else if(mInspectionMode == InspectionMode.WIRE_MODE){
                            onParameterConfigured();
                            mConfigureVerticalDistanceToLineBinding.next.setText(GlobalUtils.getString(R.string.confirm_parameter));
                        }
                        break;
                    case 7:
                        onParameterConfigured();
                        break;
                }
                break;
        }
    }


    /**
     * 配置巡检任务
     * @return
     */
    private View createConfigureMissionView(){
        nowDiaframent=1;
        if(mConfigureMissionBinding != null){
            return mConfigureMissionBinding.getRoot();
        }
        mConfigureMissionBinding = DialogParameterConfigureStep1Binding.inflate(getLayoutInflater());
        mConfigureMissionBinding.setLifecycleOwner(this);
        mConfigureMissionBinding.closeMenuButton.setOnClickListener(view -> dismiss());

        AppDatabase.getInstance().getMissionDao().getAllMissions().observe(getViewLifecycleOwner(), (List<MissionWithTowers> missionWithTowers) -> {
            MissionSpinnerAdapter adapter = new MissionSpinnerAdapter(getContext());
            adapter.setMissionWithTowers(missionWithTowers);
            mConfigureMissionBinding.lineMissionNameList.setAdapter(adapter);
            mConfigureMissionBinding.lineMissionNameList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    /* 设置选中文本颜色 */
                    TextView lineNameView = view.findViewById(R.id.lineName);
                    lineNameView.setTextColor(getResources().getColor(R.color.white));
                    /* 设置选中的巡检任务对象 */
                    mMissionWithTowers = missionWithTowers.get(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        });

        mConfigureMissionBinding.next.setOnClickListener(view -> {
            // setContentView(createConfigureInspectionModeView());
            mInspectionMode = InspectionMode.WIRE_MODE;
            setContentView(createConfigurePhaseNumberView());
        });

        Log.d("djdjdj",getView()+"");
        return mConfigureMissionBinding.getRoot();
    }

    /**
     * 配置巡检模式
     * @return
     */
    private View createConfigureInspectionModeView(){
        if(mConfigureInspectionModeBinding != null){
            return mConfigureInspectionModeBinding.getRoot();
        }
        mConfigureInspectionModeBinding = DialogParameterConfigureStep2Binding.inflate(getLayoutInflater());
        mConfigureInspectionModeBinding.setLifecycleOwner(this);
        mConfigureInspectionModeBinding.closeMenuButton.setOnClickListener(view -> dismiss());
        mConfigureInspectionModeBinding.next.setOnClickListener(view -> {
            setContentView(createConfigurePhaseNumberView());
        });
        mConfigureInspectionModeBinding.prev.setOnClickListener(view -> {
            setContentView(createConfigureMissionView());
        });
        mConfigureInspectionModeBinding.inspectionModeGroup.setOnCheckedChangeListener((RadioGroup group, int checkedId) -> {
            if(checkedId == R.id.wireMode){
                mInspectionMode = InspectionMode.WIRE_MODE;
            }else if(checkedId == R.id.wireTreeMode){
                mInspectionMode = InspectionMode.WIRE_TREE_MODE;
            }else if(checkedId == R.id.treeMode){
                mInspectionMode = InspectionMode.TREE_MODE;
            }
        });
        mConfigureInspectionModeBinding.inspectionModeGroup.check(R.id.wireMode);

        return mConfigureInspectionModeBinding.getRoot();
    }

    /**
     * 配置相号
     * @return
     */
    private View createConfigurePhaseNumberView(){
        nowDiaframent=2;
        if(mConfigurePhaseNumberBinding != null){
            return mConfigurePhaseNumberBinding.getRoot();
        }
        mConfigurePhaseNumberBinding = DialogParameterConfigureStep3Binding.inflate(getLayoutInflater());
        mConfigurePhaseNumberBinding.setLifecycleOwner(this);
        mConfigurePhaseNumberBinding.closeMenuButton.setOnClickListener(view -> dismiss());

        mConfigurePhaseNumberBinding.next.setOnClickListener(view -> {
            setContentView(createConfigureTowerNumberView());
        });
        mConfigurePhaseNumberBinding.prev.setOnClickListener(view -> {
            // setContentView(createConfigureInspectionModeView());
            setContentView(createConfigureMissionView());
        });

        mConfigurePhaseNumberBinding.phaseNumberGroup.setOnCheckedChangeListener((RadioGroup group, int checkedId) -> {
            if(checkedId == R.id.phaseNumberA){
                mWirePhaseNumber = WirePhaseNumber.PHASE_A;
            }else if(checkedId == R.id.phaseNumberB){
                mWirePhaseNumber = WirePhaseNumber.PHASE_B;
            }else if(checkedId == R.id.phaseNumberC){
                mWirePhaseNumber = WirePhaseNumber.PHASE_C;
            }else if(checkedId == R.id.phaseLeftNumberGround){
                mWirePhaseNumber = WirePhaseNumber.PHASE_LEFTGROUND;
            }else if(checkedId == R.id.phaseRightNumberGround){
                mWirePhaseNumber = WirePhaseNumber.PHASE_RIGHTGROUND;
            }
        });
        mConfigurePhaseNumberBinding.phaseNumberGroup.check(R.id.phaseNumberA);
        return mConfigurePhaseNumberBinding.getRoot();
    }

    /**
     * 配置塔号
     * @return
     */
    private View createConfigureTowerNumberView(){
        nowDiaframent=3;
        if(mConfigureTowerNumberBinding != null){
            return mConfigureTowerNumberBinding.getRoot();
        }
        mConfigureTowerNumberBinding = DialogParameterConfigureStep4Binding.inflate(getLayoutInflater());
        mConfigureTowerNumberBinding.setLifecycleOwner(this);
        mConfigureTowerNumberBinding.closeMenuButton.setOnClickListener(view -> dismiss());

        mConfigureTowerNumberBinding.next.setOnClickListener(view -> {
            setContentView(createConfigureFlightSpeedView());
        });
        mConfigureTowerNumberBinding.prev.setOnClickListener(view -> {
            setContentView(createConfigurePhaseNumberView());
        });
        List<String> list=new ArrayList<>();
        list.add("0");
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");
        list.add("6");
        list.add("7");
        list.add("8");
        list.add("9");
        mConfigureTowerNumberBinding.startTowerNum.setWheelAdapter(new ArrayWheelAdapter(getActivity()));
        mConfigureTowerNumberBinding.startTowerNum2.setWheelAdapter(new ArrayWheelAdapter(getActivity()));
        mConfigureTowerNumberBinding.startTowerNum3.setWheelAdapter(new ArrayWheelAdapter(getActivity()));
        mConfigureTowerNumberBinding.startTowerNum.setSkin(WheelView.Skin.Common); // common皮肤
        mConfigureTowerNumberBinding.startTowerNum2.setSkin(WheelView.Skin.Common); // common皮肤
        mConfigureTowerNumberBinding.startTowerNum3.setSkin(WheelView.Skin.Common); // common皮肤
        mConfigureTowerNumberBinding.startTowerNum.setWheelData(list);
        mConfigureTowerNumberBinding.startTowerNum2.setWheelData(list);
        mConfigureTowerNumberBinding.startTowerNum3.setWheelData(list);
        // mConfigureTowerNumberBinding.setEndTowerNumValue(mEndTowerNum);
        return mConfigureTowerNumberBinding.getRoot();
    }

    /**
     * 配置飞行速度
     * @return
     */
    private View createConfigureFlightSpeedView(){
        nowDiaframent=4;
        if(mConfigureFlightSpeedBinding != null){
            return mConfigureFlightSpeedBinding.getRoot();
        }
        mConfigureFlightSpeedBinding = DialogParameterConfigureStep5Binding.inflate(getLayoutInflater());
        mConfigureFlightSpeedBinding.setLifecycleOwner(this);
        mConfigureFlightSpeedBinding.setFlightSpeedValue(mFlightSpeed);
        mConfigureFlightSpeedBinding.closeMenuButton.setOnClickListener(view -> dismiss());

        mConfigureFlightSpeedBinding.next.setOnClickListener(view -> {
            setContentView(createConfigureHorizontalDistanceToLineView());
        });
        mConfigureFlightSpeedBinding.prev.setOnClickListener(view -> {
            setContentView(createConfigureTowerNumberView());
        });

        Log.d("djdjdj",getLayoutInflater()+"");
        return mConfigureFlightSpeedBinding.getRoot();
    }

    /**
     * 配置水平离线距离
     * @return
     */
    private View createConfigureHorizontalDistanceToLineView(){
        nowDiaframent=5;
        if(mConfigureHorizontalDistanceToLineBinding != null){
            return mConfigureHorizontalDistanceToLineBinding.getRoot();
        }
        mConfigureHorizontalDistanceToLineBinding = DialogParameterConfigureStep6Binding.inflate(getLayoutInflater());
        mConfigureHorizontalDistanceToLineBinding.setLifecycleOwner(this);
        mConfigureHorizontalDistanceToLineBinding.setHorizontalDistance(mHorizontalDistance);
        mConfigureHorizontalDistanceToLineBinding.closeMenuButton.setOnClickListener(view -> dismiss());

        mConfigureHorizontalDistanceToLineBinding.next.setOnClickListener(view -> {
            setContentView(createConfigureVerticalDistanceToLineView());
        });
        mConfigureHorizontalDistanceToLineBinding.prev.setOnClickListener(view -> {
            setContentView(createConfigureFlightSpeedView());
        });

        Log.d("djdjdj",getLayoutInflater()+"");
        return mConfigureHorizontalDistanceToLineBinding.getRoot();
    }

    /**
     * 配置垂直距离
     * @return
     */
    private View createConfigureVerticalDistanceToLineView(){
        nowDiaframent=6;
        if(mConfigureVerticalDistanceToLineBinding != null){
            return mConfigureVerticalDistanceToLineBinding.getRoot();
        }
        mConfigureVerticalDistanceToLineBinding = DialogParameterConfigureStep7Binding.inflate(getLayoutInflater());
        mConfigureVerticalDistanceToLineBinding.setLifecycleOwner(this);
        mConfigureVerticalDistanceToLineBinding.setVerticalDistance(mVerticalDistance);
        mConfigureVerticalDistanceToLineBinding.closeMenuButton.setOnClickListener(view -> dismiss());

        mConfigureVerticalDistanceToLineBinding.next.setOnClickListener(view -> {
            if(mInspectionMode == InspectionMode.WIRE_TREE_MODE || mInspectionMode == InspectionMode.TREE_MODE){
                setContentView(createConfigureTreeBarrierDistanceView());
                mConfigureVerticalDistanceToLineBinding.next.setText(GlobalUtils.getString(R.string.next_step));
            }else if(mInspectionMode == InspectionMode.WIRE_MODE){
                onParameterConfigured();
                mConfigureVerticalDistanceToLineBinding.next.setText(GlobalUtils.getString(R.string.confirm_parameter));
            }
        });
        mConfigureVerticalDistanceToLineBinding.prev.setOnClickListener(view -> {
            setContentView(createConfigureHorizontalDistanceToLineView());
        });

        Log.d("djdjdj",getLayoutInflater()+"");
        return mConfigureVerticalDistanceToLineBinding.getRoot();
    }

    /**
     * 配置树障距离
     * @return
     */
    private View createConfigureTreeBarrierDistanceView(){
        nowDiaframent=7;
        if(mConfigureTreeBarrierDistanceBinding != null){
            return mConfigureTreeBarrierDistanceBinding.getRoot();
        }
        mConfigureTreeBarrierDistanceBinding = DialogParameterConfigureStep8Binding.inflate(getLayoutInflater());
        mConfigureTreeBarrierDistanceBinding.setLifecycleOwner(this);
        mConfigureTreeBarrierDistanceBinding.setTreeBarrierDistanceValue(mTreeBarrierDistance);
        mConfigureTreeBarrierDistanceBinding.closeMenuButton.setOnClickListener(view -> dismiss());

        mConfigureTreeBarrierDistanceBinding.next.setOnClickListener(view -> {
            onParameterConfigured();
        });
        mConfigureTreeBarrierDistanceBinding.prev.setOnClickListener(view -> {
            setContentView(createConfigureVerticalDistanceToLineView());
        });

        return mConfigureTreeBarrierDistanceBinding.getRoot();
    }

    private void onParameterConfigured(){
        /* 检测参数配置是否异常 */
        if(mMissionWithTowers == null){
            if(mCallback != null) mCallback.onFailure(new JZIError(0, "请选择巡检任务"));
            return;
        }
        try{
            int startTowerNum = Integer.parseInt(mConfigureTowerNumberBinding.startTowerNum.getSelectionItem().toString()+mConfigureTowerNumberBinding.startTowerNum2.getSelectionItem().toString()+mConfigureTowerNumberBinding.startTowerNum3.getSelectionItem().toString());
            int endTowerNum = startTowerNum + 1;
            if(Math.abs(startTowerNum - endTowerNum) != 1){
                if(mCallback != null) mCallback.onFailure(new JZIError(0, "始末杆塔号范围异常"));
                return;
            }
            Bundle parameterData = new Bundle();
            parameterData.putSerializable(KEY_MISSION, mMissionWithTowers);                 // 配置巡检任务
            parameterData.putSerializable(KEY_INSPECTION_MODE, mInspectionMode);            // 配置巡检模式
            parameterData.putSerializable(KEY_PHASE_NUMBER, mWirePhaseNumber);              // 配置相号
            parameterData.putInt(KEY_START_TOWER_NUM, startTowerNum);                       // 配置起始塔号
            parameterData.putInt(KEY_END_TOWER_NUM, endTowerNum);                           // 配置终止塔号
            parameterData.putFloat(KEY_FLIGHT_SPEED, mFlightSpeed.get());                   // 配置巡检飞行速度
            parameterData.putFloat(KEY_HORIZONTAL_DISTANCE, mHorizontalDistance.get());     // 配置水平离线距离
            parameterData.putFloat(KEY_VERITICAL_DISTANCE, mVerticalDistance.get());        // 配置垂直离线距离
            parameterData.putInt(KEY_TREE_BARRIER_DISTANCE, mTreeBarrierDistance.get());    // 配置树障警报阈值
            if(mCallback != null) mCallback.onSuccess(parameterData);
            dismiss();
        }catch (NumberFormatException e){
            if(mCallback != null) mCallback.onFailure(new JZIError(0, "杆塔号输入内容异常"));
        }
    }

    private void setContentView(View view){
        TaskExecutors.getInstance().runInMainThread(() -> {
            getDialog().setContentView(view);
        });
    }




    private CommonCallbackWith<Bundle> mCallback;

    public void setConfigureCallback(CommonCallbackWith<Bundle> callback){
        mCallback = callback;
    }


    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        RemoteHardwareController.getInstance().removeHardwareStateListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        RemoteHardwareController.getInstance().removeHardwareStateListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RemoteHardwareController.getInstance().removeHardwareStateListener(this);
    }
}
