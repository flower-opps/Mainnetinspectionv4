package com.jizhenkeji.mainnetinspection.datamanage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.os.Environment;

import com.jizhenkeji.mainnetinspection.MAppCompatActivity;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.adapter.DataAdapter;
import com.jizhenkeji.mainnetinspection.databinding.ActivityDataManageBinding;
import com.jizhenkeji.mainnetinspection.dialog.CommonConfirmDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.MediaFileExportDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.ReportExportDialogFragment;
import com.jizhenkeji.mainnetinspection.main.MainMenuFragment;
import com.jizhenkeji.mainnetinspection.model.AppDatabase;
import com.jizhenkeji.mainnetinspection.model.dao.DataDao;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;
import com.jizhenkeji.mainnetinspection.model.entity.TreePhotoEntity;
import com.jizhenkeji.mainnetinspection.model.entity.WirePhotoEntity;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import java.io.File;
import java.util.List;

public class DataManageActivity extends MAppCompatActivity {

    private final String DATA_ROOT_PATH_NAME = "JZI";

    private DataManageViewModel mViewModel;

    private ActivityDataManageBinding mBinding;

    private DataAdapter mDataAdapter;

    private File dataRootPath = new File(Environment.getExternalStorageDirectory(), DATA_ROOT_PATH_NAME);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_data_manage);
        mDataAdapter = new DataAdapter(mDataClickCallback);
        mBinding.dataList.setAdapter(mDataAdapter);
        mBinding.dataList.setLayoutManager(new LinearLayoutManager(this));

        mViewModel = new ViewModelProvider(this).get(DataManageViewModel.class);
        mViewModel.getDatas().observe(this, (List<DataWithMetadata> dataWithMetadata) -> {
            mDataAdapter.setDataWithPhotos(dataWithMetadata);
        });

        mBinding.setViewModel(mViewModel);
        mBinding.setLifecycleOwner(this);
    }

    private DataAdapter.DataClickCallback mDataClickCallback = new DataAdapter.DataClickCallback() {
        @Override
        public void onDownInspectionPhoto(DataWithMetadata dataWithMetadata) {
            openCommonConfirmDialog(GlobalUtils.getString(R.string.is_down_photo), () -> startDownInspectionPhoto(dataWithMetadata));
        }

        @Override
        public void onPreviewPhoto(DataWithMetadata dataWithMetadata) {
            openPreviewFragment(dataWithMetadata);
        }

        @Override
        public void onBuilderReport(DataWithMetadata dataWithMetadata) {
            openCommonConfirmDialog(GlobalUtils.getString(R.string.is_build_report), () -> startBuildReport(dataWithMetadata));
        }

        @Override
        public void onDeleteData(DataWithMetadata dataWithMetadata) {
            openCommonConfirmDialog(GlobalUtils.getString(R.string.is_delete_data), () -> deleteData(dataWithMetadata));
        }

    };

    /**
     * 下载相应的巡检照片至固定的文件夹
     * @param dataWithMetadata
     */
    private void startDownInspectionPhoto(DataWithMetadata dataWithMetadata){
        MediaFileExportDialogFragment mediaFileExportDialog = new MediaFileExportDialogFragment();
        mediaFileExportDialog.show(getSupportFragmentManager(), "MediaFileExportDialogFragment");
        mediaFileExportDialog.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            public void onResume() {
                mediaFileExportDialog.exportTo(dataWithMetadata, dataRootPath);
            }
        });
    }

    /**
     * 开始构建巡检报告
     * @param dataWithMetadata
     */
    private void startBuildReport(DataWithMetadata dataWithMetadata){
        ReportExportDialogFragment reportBuildDialog = new ReportExportDialogFragment();
        reportBuildDialog.show(getSupportFragmentManager(), "ReportExportDialogFragment");
        reportBuildDialog.exportTo(dataWithMetadata, dataRootPath);
    }

    /**
     * 开启巡检照片预览界面
     * @param dataWithMetadata
     */
    private void openPreviewFragment(DataWithMetadata dataWithMetadata){
        DataManageFragment dataManageFragment = new DataManageFragment();
        dataManageFragment.setDataWithMetadata(dataWithMetadata);
        getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, dataManageFragment).commit();
    }

    /**
     * 删除巡检数据
     * @param dataWithMetadata
     */
    private void deleteData(DataWithMetadata dataWithMetadata){
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            DataDao dataDao = AppDatabase.getInstance().getDataDao();
            dataDao.deleteData(dataWithMetadata.dataEntity);
            for(WirePhotoEntity wirePhotoEntity : dataWithMetadata.wirePhotoEntities){
                dataDao.deleteWirePhoto(wirePhotoEntity);
            }
            for(TreePhotoEntity treePhotoEntity : dataWithMetadata.treePhotoEntities){
                dataDao.deleteTreePhoto(treePhotoEntity);
            }
            dataDao.deleteTerrain(dataWithMetadata.terrainEntity);
        });
    }

    /**
     * 开启确定配置对话框
     * @param description 内容描述
     * @param positiveRunnable 积极状态执行的任务
     */
    private void openCommonConfirmDialog(String description, Runnable positiveRunnable){
        CommonConfirmDialogFragment confirmDialog = new CommonConfirmDialogFragment();
        confirmDialog.setDescription(description);
        confirmDialog.setOnResultCallback(new CommonConfirmDialogFragment.OnResultCallback() {
            @Override
            public void onConfirm() {
                positiveRunnable.run();
            }

            @Override
            public void onCancel() {}
        });
        confirmDialog.show(getSupportFragmentManager(), "CommonConfirmDialogFragment");
    }

}