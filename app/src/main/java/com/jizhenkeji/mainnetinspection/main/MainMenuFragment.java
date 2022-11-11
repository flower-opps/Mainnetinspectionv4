package com.jizhenkeji.mainnetinspection.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.jizhenkeji.mainnetinspection.MApplication;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.FragmentMainMenuBinding;
import com.jizhenkeji.mainnetinspection.dialog.CommonConfirmDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.VersionDetectionDialogFragment;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraController;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import java.io.File;
import java.util.Objects;

import dji.common.camera.SettingsDefinitions.StorageLocation;
import dji.common.camera.StorageState;
import dji.common.error.DJIError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.useraccount.UserAccountManager;


public class MainMenuFragment extends Fragment {

    /* 用户登陆标志位 */
    private final ObservableBoolean observableIsUserLogin = new ObservableBoolean(false);

    /* 用户登陆状态 */
    private final ObservableField<String> observableUserState = new ObservableField<>(GlobalUtils.getString(R.string.user_state_error));

    /* 日志存储状态 */
    private final ObservableField<String> observableLogState = new ObservableField<>(GlobalUtils.getString(R.string.log_state_unknown));

    /* 图片缓存状态 */
    private final ObservableField<String> observablePhotoCacheState = new ObservableField<>(GlobalUtils.getString(R.string.log_state_unknown));

    private FragmentMainMenuBinding mBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentMainMenuBinding.inflate(inflater, container, false);
        /* DJI账号管理部分 */
        mBinding.setUserState(observableUserState);
        mBinding.setIsUserLogin(observableIsUserLogin);
        mBinding.userStateMessage.setOnClickListener(this::loginDjiUserAccount);
        /* 日志管理状态 */
        mBinding.setLogState(observableLogState);
        mBinding.deleteLogs.setOnClickListener(this::deleteAllLogs);
        /* 图片缓存状态 */
        mBinding.setPhotoCacheState(observablePhotoCacheState);
        mBinding.deletePhotos.setOnClickListener(this::deletePhotoCache);
        /* 软件版本状态 */
        // mBinding.detectionUpdateButton.setOnClickListener(this::updateVersion);
        /* 关闭对话框 */
        mBinding.closeButton.setOnClickListener(this::closeFragment);
        return mBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        initUserAccountState();
        initLogStorageState();
        initPhotoCacheState();
    }

    /**
     * 初始化用户状态
     */
    private void initUserAccountState(){
        UserAccountManager.getInstance().getLoggedInDJIUserAccountName(new CommonCallbacks.CompletionCallbackWith<String>() {
            @Override
            public void onSuccess(String s) {
                observableIsUserLogin.set(true);
                observableUserState.set(s);
            }

            @Override
            public void onFailure(DJIError djiError) {
                observableIsUserLogin.set(false);
                observableUserState.set(GlobalUtils.getString(R.string.user_login_failure));
            }
        });
    }

    private void loginDjiUserAccount(View view){
        UserAccountManager.getInstance().logIntoDJIUserAccount(getActivity(), new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
            @Override
            public void onSuccess(UserAccountState userAccountState) {
                initUserAccountState();
            }
            @Override
            public void onFailure(DJIError djiError) {
                observableUserState.set(GlobalUtils.getString(R.string.user_login_failure));
                observableIsUserLogin.set(false);
            }
        });
    }

    public void deleteAllLogs(View view){
        CommonConfirmDialogFragment dialogFragment = new CommonConfirmDialogFragment();
        dialogFragment.setDescription(GlobalUtils.getString(R.string.is_delete_all_log));
        dialogFragment.setOnResultCallback(new CommonConfirmDialogFragment.OnResultCallback() {
            @Override
            public void onConfirm() {
                File logDir = MApplication.LOG_ROOT_PATH;
                if(logDir == null || !logDir.exists() || !logDir.isDirectory()){
                    return;
                }
                File[] logFiles = logDir.listFiles();
                if(logFiles == null || logFiles.length == 0){
                    return;
                }
                for(File logFile:logFiles){
                    logFile.delete();
                }
                initLogStorageState();
            }

            @Override
            public void onCancel() {}
        });
        dialogFragment.show(getChildFragmentManager(), "CommonConfirmDialogFragment");
    }

    private void initLogStorageState(){
        File logDir = MApplication.LOG_ROOT_PATH;
        if(logDir == null || !logDir.exists() || !logDir.isDirectory()){
            observableLogState.set("0 KB");
            return;
        }
        long logDirSize = getFolderSize(logDir) / 1024;
        if(logDirSize > 1024){
            logDirSize = logDirSize / 1024;
            observableLogState.set(logDirSize + " MB");
        }else{
            observableLogState.set(logDirSize + " KB");
        }
    }

    public void deletePhotoCache(View view){
        CommonConfirmDialogFragment dialogFragment = new CommonConfirmDialogFragment();
        dialogFragment.setDescription(GlobalUtils.getString(R.string.is_delete_photo_cache));
        dialogFragment.setOnResultCallback(new CommonConfirmDialogFragment.OnResultCallback() {
            @Override
            public void onConfirm() {
                File photoCacheDir = Glide.getPhotoCacheDir(getContext());
                if(photoCacheDir == null || !photoCacheDir.exists() || !photoCacheDir.isDirectory()){
                    return;
                }
                File[] photoCacheFiles = photoCacheDir.listFiles();
                if(photoCacheFiles == null || photoCacheFiles.length == 0){
                    return;
                }
                for(File photoCacheFile : photoCacheFiles){
                    photoCacheFile.delete();
                }
                initPhotoCacheState();
            }

            @Override
            public void onCancel() {}
        });
        dialogFragment.show(getChildFragmentManager(), "CommonConfirmDialogFragment");
    }

    private void initPhotoCacheState(){
        File photoCacheDir = Glide.getPhotoCacheDir(getContext());
        if(photoCacheDir == null || !photoCacheDir.exists() || !photoCacheDir.isDirectory()){
            observablePhotoCacheState.set("0 KB");
            return;
        }
        long photoCacheSize = getFolderSize(photoCacheDir) / 1024;
        if(photoCacheSize > 1024){
            photoCacheSize = photoCacheSize / 1024;
            observablePhotoCacheState.set(photoCacheSize + " MB");
        }else{
            observablePhotoCacheState.set(photoCacheSize + " KB");
        }
    }

    private long getFolderSize(File dirFile){
        long length = 0;
        for (File file : dirFile.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += getFolderSize(file);
        }
        return length;
    }

    /**
     * 开启版本检测对话框
     * @param view
     */
    private void updateVersion(View view){
        VersionDetectionDialogFragment dialogFragment = new VersionDetectionDialogFragment();
        dialogFragment.show(getChildFragmentManager(), "VersionDetectionDialogFragment");
    }

    private void closeFragment(View view){
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }

}
