package com.jizhenkeji.mainnetinspection.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.NumberPicker;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableField;
import androidx.fragment.app.DialogFragment;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.DialogMissionCreateBinding;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;


/**
 * 巡检任务创建对话框
 */
public class MissionCreateDialogFragment extends DialogFragment {

    private final String MANAGE_CLASS_NAME_KEY = "MANAGE_CLASS_NAME_KEY";

    private final String[] VOLTAGE_LEVEL_DISPLAYS = new String[]{
            "110KV", "220KV", "500KV", "800KV", "1000KV"
    };

    /**
     * 巡检任务线路名称
     */
    public final ObservableField<String> lineName = new ObservableField<>("");

    /**
     * 巡检线路的电压等级
     */
    public final ObservableField<String> voltageLevel = new ObservableField<>("");

    /**
     * 管辖班组
     */
    public final ObservableField<String> manageClassName = new ObservableField<>("");

    private SharedPreferences mSharedPref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        /* 获取默认管辖班组名称 */
        mSharedPref = getContext().getSharedPreferences(
                GlobalUtils.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        manageClassName.set(mSharedPref.getString(MANAGE_CLASS_NAME_KEY, ""));
    }

    @Override
    public void onStart() {
        super.onStart();
        Window dialogWindow = getDialog().getWindow();
        /* 设置窗体背景 */
        dialogWindow.setBackgroundDrawableResource(R.drawable.corner_15_white);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DialogMissionCreateBinding binding = DialogMissionCreateBinding.inflate(inflater, container, false);
        binding.setData(this);

        binding.voltageLevelPicker.setMinValue(0);
        binding.voltageLevelPicker.setMaxValue(4);
        binding.voltageLevelPicker.setDisplayedValues(VOLTAGE_LEVEL_DISPLAYS);
        voltageLevel.set(VOLTAGE_LEVEL_DISPLAYS[binding.voltageLevelPicker.getValue()]);
        binding.voltageLevelPicker.setOnValueChangedListener((NumberPicker picker, int oldVal, int newVal) -> {
            voltageLevel.set(VOLTAGE_LEVEL_DISPLAYS[newVal]);
        });

        return binding.getRoot();
    }

    /**
     * 关闭对话框
     */
    public void closeDialog(){
        dismissAllowingStateLoss();
    }

    /**
     * 确定巡检任务参数，创建巡检任务
     */
    public void confirmParameter(){
        if(lineName.get() == null || lineName.get().isEmpty()){
            Toast.makeText(getContext(), "请重新输入线路名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if(voltageLevel.get() == null || voltageLevel.get().isEmpty()){
            Toast.makeText(getContext(), "请重新输入线路电压等级", Toast.LENGTH_SHORT).show();
            return;
        }
        if(callback != null){
            /* 保存管辖班组作为默认名称 */
            mSharedPref.edit().putString(MANAGE_CLASS_NAME_KEY, manageClassName.get()).apply();
            callback.onCreated(lineName.get(), voltageLevel.get(), manageClassName.get());
        }
        dismiss();
    }

    /**
     * 巡检任务创建成功回调接口
     */
    private OnMissionCreatedCallback callback;

    /**
     * 设置巡检任务创建成功回调接口
     */
    public void setOnMissionCreatedCallback(OnMissionCreatedCallback callback){
        this.callback = callback;
    }

    /**
     * 巡检任务创建成功回调接口
     */
    public interface OnMissionCreatedCallback{

        void onCreated(String lineName, String voltageLevel, String manageClassName);

    }

}
