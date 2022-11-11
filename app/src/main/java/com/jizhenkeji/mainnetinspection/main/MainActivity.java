package com.jizhenkeji.mainnetinspection.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.jizhenkeji.mainnetinspection.MAppCompatActivity;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.controller.gimbalcontroller.GimbalController;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.ButtonEvent;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.HardwareStateListener;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.HardwareType;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.RemoteHardwareController;
import com.jizhenkeji.mainnetinspection.databinding.ActivityMainBinding;
import com.jizhenkeji.mainnetinspection.datamanage.DataManageActivity;
import com.jizhenkeji.mainnetinspection.dialog.BluetoothConnectDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.CommonConfirmDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.DataSourceConfigureDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.ParameterConfigureDialogFragment;
import com.jizhenkeji.mainnetinspection.missionexecute.MissionExecuteActivity;
import com.jizhenkeji.mainnetinspection.missionmanage.MissionManageActivity;
import com.jizhenkeji.mainnetinspection.common.CommonCallbackWith;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.radar.RadarAdapter;
import com.jizhenkeji.mainnetinspection.radar.RadarManager;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;

import dji.common.product.Model;

public class MainActivity extends MAppCompatActivity {

    private MainViewModel mViewModel;

    private ActivityMainBinding mBinding;

    public static Model mModel;

    private boolean backToTheMiddle=true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.settings.setOnClickListener((View view) -> {
            openSettingsFragment();           // 开启设置对话框
        });
        mBinding.help.setOnClickListener((View view) -> {
            openHelpDialog();
        });
        mBinding.startFlight.setOnClickListener(this::startFlight);

        mBinding.dataManage.setOnClickListener(view -> openDataManageActivity());

        mBinding.recordMission.setOnClickListener(view -> openRecordMissionActivity());

        /* 初始化背景视频 */
        VideoView videoView = mBinding.videoBackground;
        videoView.setOnPreparedListener((MediaPlayer mp) -> {
            mp.setLooping(true);
            mp.start();
        });
        String path = "android.resource://" + getPackageName() + "/" + R.raw.background;
        videoView.setVideoURI(Uri.parse(path));

        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mViewModel.getProductModel().observe(this, this::initModelBackground);
        mViewModel.initState();

        mBinding.setViewModel(mViewModel);
        mBinding.setLifecycleOwner(this);
    }


//    @HardwareStateListener(type = HardwareType.C1BUTTON)
//    public void c1Click(ButtonEvent event){
//        switch (event){
//            case DOWN:
//            case LONG_DOWN:
//                break;
//            case UP:
//                if (!(backToTheMiddle)) {
//                    GimbalController.getInstance().rotatePitchTo(-90, null);
//                    backToTheMiddle=true;
//                } else if (backToTheMiddle) {
//                    GimbalController.getInstance().rotatePitchTo(0, null);
//                    backToTheMiddle=false;
//                }
//                break;
//        }
//    }

    private void initModelBackground(Model model){
        mModel = model;
        switch (model){
            case MAVIC_2:
            case MAVIC_2_PRO:
            case MAVIC_2_ENTERPRISE_ADVANCED:
                mBinding.aircraftPhoto.setImageResource(R.drawable.mavic_2_pro);
                break;
            case MATRICE_300_RTK:
                mBinding.aircraftPhoto.setImageResource(R.drawable.m300);
                break;
            case PHANTOM_4_RTK:
                mBinding.aircraftPhoto.setImageResource(R.drawable.p4r);
                break;
            case MAVIC_2_ENTERPRISE_DUAL:
                mBinding.aircraftPhoto.setImageResource(R.drawable.mavic_2_enterprise_dual);
                break;
            case MAVIC_2_ZOOM:
            case MAVIC_2_ENTERPRISE:
                mBinding.aircraftPhoto.setImageResource(R.drawable.mavic_2_zoom);
                break;
        }
    }

    private void openSettingsFragment(){
        getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, new MainMenuFragment()).commit();
    }

    private void openHelpDialog(){
        Intent helperIntent = getPackageManager().getLaunchIntentForPackage("com.jizhenkeji.jzihelper");
        if(helperIntent != null){
            startActivity(helperIntent);
        }
    }

    private void openDataManageActivity(){
        DataSourceConfigureDialogFragment dialogFragment = new DataSourceConfigureDialogFragment();
        dialogFragment.setOnConfigureCallback(() -> {
            Intent dataManageIntent = new Intent(this, DataManageActivity.class);
            startActivity(dataManageIntent);
        });
        dialogFragment.show(getSupportFragmentManager(), "DataSourceConfigureDialogFragment");
    }

    private void openRecordMissionActivity(){
        Intent recordMissionIntent = new Intent(this, MissionManageActivity.class);
        startActivity(recordMissionIntent);
    }

    private void startFlight(View view){
        /* 如果雷达已经连接，则直接进入执行任务 */
        RadarManager radarManager = RadarAdapter.getRadarManager();
        if(radarManager != null && radarManager.isConnect()){
            openMissionExecuteActivity();
            return;
        }
        /* 根据当前无人机型号初始化雷达连接 */
        switch (mModel){
//            case MATRICE_300_RTK:
//                RadarAdapter.initPlayloadRadar();
//                openMissionExecuteActivity();
//                break;
            default:
                BluetoothConnectDialogFragment bluetoothConnectDialog = new BluetoothConnectDialogFragment();
                bluetoothConnectDialog.setOnConnectCallback(new BluetoothConnectDialogFragment.OnConnectCallback() {
                    @Override
                    public void onConnect(BluetoothDevice device) {
                        if(device == null){
                            /* 点击跳过按钮 */
                            skipRadarConfigure();
                        }else{
                            RadarAdapter.initBluetoothRadar(device);
                            RadarAdapter.getRadarManager().conenct();
                            openMissionExecuteActivity();
                        }
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(MainActivity.this,
                                GlobalUtils.getString(R.string.please_connect_bluetooth), Toast.LENGTH_SHORT).show();
                    }
                });
                bluetoothConnectDialog.show(getSupportFragmentManager(), "BluetoothConnectDialogFragment");
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteHardwareController.getInstance().removeHardwareStateListener(this);
    }

    /**
     * 进入巡检事务活动
     */
    private void openMissionExecuteActivity(){
        ParameterConfigureDialogFragment parameterConfigureDialog = new ParameterConfigureDialogFragment();
        parameterConfigureDialog.setConfigureCallback(new CommonCallbackWith<Bundle>() {
            @Override
            public void onSuccess(Bundle bundle) {
                Intent missionExecuteIntent = new Intent(MainActivity.this, MissionExecuteActivity.class);
                missionExecuteIntent.putExtra(MissionExecuteActivity.INSPECTION_PARAMETER, bundle);
                startActivity(missionExecuteIntent);
            }

            @Override
            public void onFailure(JZIError jziError) {
                Toast.makeText(MainActivity.this, jziError.getDescription(), Toast.LENGTH_SHORT).show();
            }
        });
        parameterConfigureDialog.show(getSupportFragmentManager(), "ParameterConfigureDialogFragment");
    }

    /**
     * 跳过雷达配置事务，直接进入事务管理活动
     */
    private void skipRadarConfigure(){
        CommonConfirmDialogFragment commonConfirmDialog = new CommonConfirmDialogFragment();
        commonConfirmDialog.setDescription(GlobalUtils.getString(R.string.is_continue_skip_radar_configure));
        commonConfirmDialog.setOnResultCallback(new CommonConfirmDialogFragment.OnResultCallback() {
            @Override
            public void onConfirm() {
                RadarAdapter.initEmptyRadar();
                openMissionExecuteActivity();
            }

            @Override
            public void onCancel() {

            }
        });
        commonConfirmDialog.show(getSupportFragmentManager(), "CommonConfirmDialogFragment");
    }


}