package com.jizhenkeji.mainnetinspection.initsdk;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.VideoView;
import com.bumptech.glide.Glide;
import com.jizhenkeji.mainnetinspection.MAppCompatActivity;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.ActivityInitBinding;
import com.jizhenkeji.mainnetinspection.main.MainActivity;
import java.util.ArrayList;
import java.util.List;

public class InitActivity extends MAppCompatActivity {

    private InitViewModel mViewModel;

    private ActivityInitBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(InitViewModel.class);
        mViewModel.getProductConnected().observe(this, (Boolean aBoolean) -> {
            if(aBoolean){
                openMainTransaction();
            }
        });

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_init);
        mBinding.setViewModel(mViewModel);
        mBinding.setLifecycleOwner(this);
        /* 设置初始化界面logo */
        Glide.with(this).load(R.drawable.jzi).into(mBinding.logo);
        /* 初始化背景视频 */
        VideoView videoView = mBinding.videoBackground;
        videoView.setOnPreparedListener((MediaPlayer mp) -> {
            mp.setLooping(true);
            mp.start();
        });
        String path = "android.resource://" + getPackageName() + "/" + R.raw.background;
        videoView.setVideoURI(Uri.parse(path));
        /* 检测权限 */
        checkPermission();
    }

    private void openMainTransaction(){
        Intent enterMainActivityIntent = new Intent(InitActivity.this, MainActivity.class);
        startActivity(enterMainActivityIntent);
        finish();
    }

    /**
     * 检测是否拥有权限
     */
    private void checkPermission(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            /* 开始注册SDK */
            mViewModel.registerSDK();
            return;
        }


        /* 检查权限 */
        for(String eachPermission:Build.VERSION.SDK_INT<30?REQUIRED_PERMISSION_MOBILELIST:REQUIRED_PERMISSION_LIST){
            if (ContextCompat.checkSelfPermission(InitActivity.this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        if(missingPermission.isEmpty()){
            /* 开始注册SDK */
            mViewModel.registerSDK();
        }else{
            requestPermissions(Build.VERSION.SDK_INT<30?REQUIRED_PERMISSION_MOBILELIST:REQUIRED_PERMISSION_LIST, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode != PERMISSION_REQUEST_CODE){
            return;
        }
        for(int i = 0; i < permissions.length; i++){
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                missingPermission.remove(permissions[i]);
            }
        }
        if(missingPermission.isEmpty()){
            /* 开始注册SDK */
            mViewModel.registerSDK();
        }else{
            Toast.makeText(InitActivity.this, "权限缺失", Toast.LENGTH_SHORT).show();
            for(String permission:missingPermission){
            }
        }
    }

    /**
     * 权限请求码
     */
    private final int PERMISSION_REQUEST_CODE = 0x01;

    /**
     * 存储缺失的权限
     */
    private List<String> missingPermission = new ArrayList<>();

    /**
     * 所需获取的权限列表
     */
    private String[] REQUIRED_PERMISSION_LIST = new String[] {
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };

    /**
     * 所需获取的权限列表
     */
    private String[] REQUIRED_PERMISSION_MOBILELIST = new String[] {
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };

}