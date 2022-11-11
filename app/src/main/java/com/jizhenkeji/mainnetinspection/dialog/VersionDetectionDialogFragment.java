package com.jizhenkeji.mainnetinspection.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableFloat;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.jizhenkeji.mainnetinspection.MApplication;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.DialogVersionDetectionBinding;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import org.apache.poi.util.NullLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 软件版本检测下载对话框
 */
public class VersionDetectionDialogFragment extends DialogFragment {

    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    private final String PACKAGE_CONFIG_ADDRESS = MApplication.SERVICE_ROOT_URL + "/config";

    private final String PACKAGE_DOWN_ADDRESS = MApplication.SERVICE_ROOT_URL + "/package";

    private final String PACKAGE_ARRAY_NAME = "package";

    private final String PACKAGE_NAME_KEY = "name";

    private final String PACKAGE_VERSION_KEY = "version";

    private final String PACKAGE_SIZE_KEY = "size";

    private final String PACKAGE_PACKAGE_KEY = "package";

    private final String USER_AGENT_KEY = "User-Agent";

    private final String USER_AGENT_VALUE = "Mozilla/5.0 (Linux; Android 4.2.1; M040 Build/JOP40D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.59 Mobile Safari/537.36";

    private final ObservableBoolean observableIsLastVersion = new ObservableBoolean(false);

    private final ObservableBoolean observableIsPulled = new ObservableBoolean(false);

    private final ObservableField<String> observableDetectionMessage = new ObservableField<>(GlobalUtils.getString(R.string.get_version_information));

    private final ObservableInt observablePackageDownProgress = new ObservableInt(0);

    private DialogVersionDetectionBinding mBinding;

    private String mLastVersionCode;

    private String mPackageName;

    private long mAllPackageSize;

    private String mPackages;

    private File mPackageOutDir = new File(Environment.getExternalStorageDirectory(), "JZI");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DialogVersionDetectionBinding.inflate(inflater, container, false);

        Glide.with(this).load(R.drawable.jzi).into(mBinding.logo);

        mBinding.closeButton.setOnClickListener((View view) -> dismiss());
        mBinding.setIsPulled(observableIsPulled);
        mBinding.setIsLastVersion(observableIsLastVersion);
        mBinding.setDetectionMessage(observableDetectionMessage);
        mBinding.setPackageDownProgress(observablePackageDownProgress);
        mBinding.packageDownloadButton.setOnClickListener(this::startDownLastPackage);
        
        mBinding.productWebView.setWebViewClient(new WebViewClient());
        mBinding.productWebView.clearCache(true);
        mBinding.productWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        return mBinding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        setCancelable(false);
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        getDialog().getWindow().setBackgroundDrawable(null);
        /* 拉取软件的版本信息 */
        TaskExecutors.getInstance().runInNetworkIoThread(() -> {
            try{
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .header(USER_AGENT_KEY, USER_AGENT_VALUE)
                        .url(PACKAGE_CONFIG_ADDRESS)
                        .build();
                Response response = client.newCall(request).execute();
                if(!response.isSuccessful()){
                    observableDetectionMessage.set(GlobalUtils.getString(R.string.network_request_error));
                    return;
                }
                parseConfigInformation(response.body().string());   // 解析版本信息
            } catch (IOException e) {
                e.printStackTrace();
                observableDetectionMessage.set(GlobalUtils.getString(R.string.network_request_error));
            }
        });
    }

    /**
     * 解析软件版本配置信息
     * @param config
     */
    private void parseConfigInformation(String config){
        try{
            JSONObject configJsonObject = new JSONObject(config);
            JSONArray packageArray = configJsonObject.getJSONArray(PACKAGE_ARRAY_NAME);
            JSONObject packageJsonObject = packageArray.getJSONObject(0);
            mPackageName = packageJsonObject.getString(PACKAGE_NAME_KEY);           // 名称
            mLastVersionCode = packageJsonObject.getString(PACKAGE_VERSION_KEY);    // 版本
            mAllPackageSize = packageJsonObject.getLong(PACKAGE_SIZE_KEY);          // 大小
            mPackages = packageJsonObject.getString(PACKAGE_PACKAGE_KEY);           // 分包名称
            boolean isUpdateVersion = compareVersionCode(MApplication.VERSION, mLastVersionCode);
            observableIsPulled.set(true);
            if(isUpdateVersion){
                observableIsLastVersion.set(false);
                loadIntroducePage(mLastVersionCode);
            }else{
                observableIsLastVersion.set(true);
                observableDetectionMessage.set(GlobalUtils.getString(R.string.already_the_latest_version));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            observableDetectionMessage.set(GlobalUtils.getString(R.string.get_version_error));
        }
    }

    /**
     * 比较两个版本号
     * @param currentVersion 当前版本号
     * @param targetVersion 目标版本号
     * @return 布尔值，代表是否可以更新至目标版本号
     */
    private boolean compareVersionCode(String currentVersion, String targetVersion){
        if(!currentVersion.startsWith("v") || !targetVersion.startsWith("v")){
            return false;
        }
        currentVersion = currentVersion.substring(1);
        targetVersion = targetVersion.substring(1);
        String[] currentVersionArray = currentVersion.split("\\.");
        String[] targetVersionArray = targetVersion.split("\\.");
        if(currentVersionArray.length != targetVersionArray.length || currentVersionArray.length != 3){
            return false;
        }
        for(int i = 0; i < currentVersionArray.length; i++){
            int targetVersionNum = Integer.parseInt(targetVersionArray[i]);
            int currentVersionNum = Integer.parseInt(currentVersionArray[i]);
            if(targetVersionNum > currentVersionNum){
                return true;
            }else if(targetVersionNum < currentVersionNum){
                return false;
            }
        }
        return false;
    }

    /**
     * 加载软件版本预览界面
     * @param version
     */
    private void loadIntroducePage(String version){
        loadUrlPage(PACKAGE_DOWN_ADDRESS + "/" + version + "/index.html");
    }

    /**
     * 加载网络页面
     * @param url
     */
    private void loadUrlPage(String url){
        TaskExecutors.getInstance().runInMainThread(() -> {
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put(USER_AGENT_KEY, USER_AGENT_VALUE);
            mBinding.productWebView.loadUrl(url, headerMap);
        });
    }

    /**
     * 开始下载最新安装包
     */
    private void startDownLastPackage(View view) {
        view.setEnabled(false);
        mBinding.packageDownloadProgress.setAlpha(1f);
        mBinding.packageDownloadText.setAlpha(1f);
        mBinding.closeButton.setVisibility(View.GONE);

        TaskExecutors.getInstance().runInNetworkIoThread(() -> {
            try{
                if(!mPackageOutDir.exists() || !mPackageOutDir.isDirectory()){
                    mPackageOutDir.mkdir();
                    return;
                }
                File packageOutFile = new File(mPackageOutDir, mPackageName + ".apk");
                FileOutputStream packageOutputStream = new FileOutputStream(packageOutFile);
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .header(USER_AGENT_KEY, USER_AGENT_VALUE)
                        .url(PACKAGE_DOWN_ADDRESS + "/" + mLastVersionCode + "/" + mPackages)
                        .build();
                Response response = client.newCall(request).execute();
                InputStream inputStream = response.body().byteStream();
                byte[] tempDatas = new byte[1024 * 200];
                long hasDownLength = 0;
                int length;
                long startDownTime = System.currentTimeMillis();
                while((length = inputStream.read(tempDatas)) > 0){
                    hasDownLength += length;
                    packageOutputStream.write(tempDatas, 0, length);
                    /* 统计下载进度 */
                    observablePackageDownProgress.set((int) (hasDownLength * 100 / mAllPackageSize));
                }
                packageOutputStream.close();
                observablePackageDownProgress.set(100);
                /* 调用系统活动安装新版本软件 */
                installNewVersionSoftware(packageOutFile);
            } catch (IOException e) {
                observablePackageDownProgress.set(0);
            }
        });
    }

    private void installNewVersionSoftware(File newVersionFile){
        TaskExecutors.getInstance().runInMainThread(() -> {
            Uri uri = FileProvider.getUriForFile(getContext(), MApplication.FILE_PROVIDER_AUTHORITY, newVersionFile);
            Intent installNewVersionIntent = new Intent(Intent.ACTION_VIEW);
            installNewVersionIntent.setDataAndType(uri, "application/vnd.android.package-archive");
            installNewVersionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(installNewVersionIntent);
            dismiss();
        });
    }

}
