package com.jizhenkeji.mainnetinspection.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.fragment.app.DialogFragment;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.DialogReportExportBinding;
import com.jizhenkeji.mainnetinspection.model.AppDatabase;
import com.jizhenkeji.mainnetinspection.model.entity.DataEntity;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;
import com.jizhenkeji.mainnetinspection.model.entity.TreePhotoEntity;
import com.jizhenkeji.mainnetinspection.report.TreeBarrierReportBuilder;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.MediaDataController;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;

public class ReportExportDialogFragment extends DialogFragment {

    public final ObservableField<String> observableProgressMessage = new ObservableField<>(GlobalUtils.getString(R.string.export_inspection_report));

    public final ObservableBoolean observableIsFinish = new ObservableBoolean(false);

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final String CACHE_DIR_NAME = GlobalUtils.getString(R.string.down_cache_name);

    private final String REPORT_FILE_NAME = GlobalUtils.getString(R.string.report_file_name);

    private DataWithMetadata mDataWithMetadata;

    private File mExportPath;

    private File mExportTempPath;

    private int totalTreePhotoCount;

    private int downloadTreePhotoCount;

    private DialogReportExportBinding mBinding;

    @Override
    public void onStart() {
        super.onStart();
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DialogReportExportBinding.inflate(inflater, container, false);

        mBinding.setProgressMsg(observableProgressMessage);
        mBinding.setIsFinish(observableIsFinish);

        return mBinding.getRoot();
    }

    /**
     * 设置巡检报告导出到根目录
     * @param dataWithMetadata 关联的巡检数据
     * @param exportRootPath 导出的路径
     */
    public void exportTo(DataWithMetadata dataWithMetadata, File exportRootPath){
        mDataWithMetadata = dataWithMetadata;
        /* 判断导出的路径是否合法 */
        if(exportRootPath == null || !exportRootPath.exists() || !exportRootPath.isDirectory()){
            observableProgressMessage.set(GlobalUtils.getString(R.string.export_path_error));
            setCancelable(true);
            return;
        }
        /* 判断巡检数据是否合法 */
        if(dataWithMetadata == null
                || (dataWithMetadata.wirePhotoEntities.isEmpty() && dataWithMetadata.treePhotoEntities.isEmpty())){
            observableProgressMessage.set(GlobalUtils.getString(R.string.lose_inspection_data));
            setCancelable(true);
            return;
        }
        mExportPath = new File(exportRootPath, df.format(dataWithMetadata.dataEntity.createDate));
        if(!mExportPath.exists() || !mExportPath.isDirectory()){
            mExportPath.mkdir();
        }
        mExportTempPath = new File(mExportPath, CACHE_DIR_NAME);
        if(!mExportTempPath.exists() || !mExportTempPath.isDirectory()){
            mExportTempPath.mkdir();
        }
        /* 下载树障照片 */
        observableProgressMessage.set(GlobalUtils.getString(R.string.down_tree_barrier_photo));
        totalTreePhotoCount = dataWithMetadata.treePhotoEntities.size();
        for(TreePhotoEntity treePhotoEntity : dataWithMetadata.treePhotoEntities){
            try{
                FileOutputStream outputStream = new FileOutputStream(new File(mExportTempPath, treePhotoEntity.name));
                MediaDataController.getInstance().getPreviewPhoto(treePhotoEntity.mediaFile, outputStream, (JZIError jziError) -> {
                    downloadTreePhotoCount++;
                    if(downloadTreePhotoCount >= totalTreePhotoCount){
                        /* 树障照片下载完毕，开始构建报告 */
                        TaskExecutors.getInstance().runInDiskIoThread(() -> buildReportWithTreePhoto());
                    }
                });
            } catch (FileNotFoundException e) {}
        }
    }

    private void buildReportWithTreePhoto(){
        /* 初始化树障报告构建对象，下载图片缓存 */
        DataEntity dataEntity = mDataWithMetadata.dataEntity;
        TreeBarrierReportBuilder treeBarrierReportBuilder = new TreeBarrierReportBuilder();
        treeBarrierReportBuilder.setAircraftName(dataEntity.aircraftName);
        treeBarrierReportBuilder.setPhaseNumber(dataEntity.phaseNumber);
        treeBarrierReportBuilder.setMissionName(dataEntity.missionName);
        TowerEntity startTowerEntity = AppDatabase.getInstance().getMissionDao().isContains(dataEntity.missionId, dataEntity.startTowerNum);
        treeBarrierReportBuilder.setStartTowerInformation(
                dataEntity.startTowerNum,
                startTowerEntity != null ? startTowerEntity.location : dataEntity.startPointLocation);
        TowerEntity endTowerEntity = AppDatabase.getInstance().getMissionDao().isContains(dataEntity.missionId, dataEntity.endTowerNum);
        treeBarrierReportBuilder.setEndTowerInformation(
                dataEntity.endTowerNum,
                endTowerEntity != null ? endTowerEntity.location : null);
        treeBarrierReportBuilder.setInspectionDate(dataEntity.createDate);
        treeBarrierReportBuilder.setManageClassName(dataEntity.manageClassName);
        treeBarrierReportBuilder.setTreeBarrierDistance(dataEntity.treeBarrierThreshold);
        treeBarrierReportBuilder.setVoltageLevel(dataEntity.voltageLevel);
        treeBarrierReportBuilder.setTerrainInformations(mDataWithMetadata.terrainEntity.terrainInformations);
        /* 遍历树障照片，附加树障照片 */
        for(TreePhotoEntity treePhotoEntity : mDataWithMetadata.treePhotoEntities){
            FileInputStream treePhotoInputStream = getCachePhotoInputStream(treePhotoEntity);
            if(treePhotoInputStream == null){
                continue;
            }
            treeBarrierReportBuilder.addTreeBarrierInformation(
                    treePhotoEntity.name,
                    treePhotoEntity.description,
                    treePhotoEntity.location,
                    treePhotoEntity.treeBarrierXDistance,
                    treePhotoEntity.treeBarrierYDistance,
                    treePhotoInputStream
            );
        }
        File reportFile = new File(mExportPath, REPORT_FILE_NAME);
        try{
            FileOutputStream reportOutputStream = new FileOutputStream(reportFile);
            treeBarrierReportBuilder.build(reportOutputStream, new TreeBarrierReportBuilder.TreeBarrierReportCallback() {
                @Override
                public void onStart() {
                    observableProgressMessage.set(GlobalUtils.getString(R.string.start_build_report));
                }

                @Override
                public void onProgress(int i, String s) {
                    observableProgressMessage.set(s);
                }

                @Override
                public void onFinish(JZIError jziError) {
                    if(jziError != null){
                        observableProgressMessage.set(jziError.getDescription());
                    }else{
                        observableProgressMessage.set(GlobalUtils.getString(R.string.finish_build_report));
                        observableIsFinish.set(true);
                    }
                    setCancelable(true);
                }
            });
        } catch (FileNotFoundException e) {}
    }

    /**
     * 获取本地缓存照片文件的输入流
     * @param treePhotoEntity
     * @return
     */
    private FileInputStream getCachePhotoInputStream(TreePhotoEntity treePhotoEntity){
        File photoFile = new File(mExportTempPath, treePhotoEntity.name);
        if(!photoFile.exists() || !photoFile.isFile()){
            return null;
        }
        try{
            FileInputStream inputStream = new FileInputStream(photoFile);
            return inputStream;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

}
