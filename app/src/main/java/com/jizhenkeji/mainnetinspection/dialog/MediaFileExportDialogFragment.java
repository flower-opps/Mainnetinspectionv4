package com.jizhenkeji.mainnetinspection.dialog;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.fragment.app.DialogFragment;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.DialogMediaFileExportBinding;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;
import com.jizhenkeji.mainnetinspection.model.entity.TreePhotoEntity;
import com.jizhenkeji.mainnetinspection.model.entity.WirePhotoEntity;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.MediaDataController;
import com.jizhenkeji.mainnetinspection.utils.DrawRadianTerrainUtils;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 巡检数据的媒体文件导出对话框
 */
public class MediaFileExportDialogFragment extends DialogFragment {

    private final String WIRE_DIR_NAME = GlobalUtils.getString(R.string.wire_photo);

    private final String TREE_BARRIER_DIR_NAME = GlobalUtils.getString(R.string.tree_barrier_photo);

    private final String TERRAIN_ARCS_NAME = GlobalUtils.getString(R.string.terrain_arcs_photo);

    public final ObservableField<String> observableProgressMessage = new ObservableField<>(GlobalUtils.getString(R.string.export_inspection_data));

    public final ObservableField<String> observableProgressPercent = new ObservableField<>("");

    public final ObservableInt observableProgressValue = new ObservableInt();

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private DataWithMetadata mDataWithMetadata;

    private File mExportRootPath;

    private File mExportPath;

    private File mExportWirePath;

    private File mExportARCSPath;

    private File mExportTreeBarrierPath;

    private int totalExportNum;

    private int currentExportNum;

    private DialogMediaFileExportBinding mBinding;

    private List<WirePhotoEntity> wirePhotoEntityList=new ArrayList<>();

    private List<TreePhotoEntity> treePhotoEntities=new ArrayList<>();

    @Override
    public void onStart() {
        super.onStart();
        setCancelable(false);
        getDialog().getWindow().setBackgroundDrawable(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DialogMediaFileExportBinding.inflate(inflater, container, false);
        mBinding.setProgressMessage(observableProgressMessage);
        mBinding.setProgressValue(observableProgressValue);
        mBinding.setProgressPercent(observableProgressPercent);
        return mBinding.getRoot();
    }

    /**
     * 设置导出到的根目录
     * @param dataWithMetadata 关联的数据对象
     * @param exportRootPath 导出的根目录
     */
    public void exportTo(DataWithMetadata dataWithMetadata, File exportRootPath){
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            mDataWithMetadata = dataWithMetadata;
            mExportRootPath = exportRootPath;
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
            mExportPath = new File(mExportRootPath, df.format(dataWithMetadata.dataEntity.createDate));
            if(!mExportPath.exists() || !mExportPath.isDirectory()){
                mExportPath.mkdir();
            }
            /* 计算总进度 */

            totalExportNum = 0;
            currentExportNum = 0;
            /* 下载地形弧线照片 */
            mExportARCSPath = new File(mExportPath, TERRAIN_ARCS_NAME);
            if(!mExportARCSPath.exists() || !mExportARCSPath.isDirectory()){
                mExportARCSPath.mkdir();
            }

            /* 遍历下载导线照片 */
            mExportWirePath = new File(mExportPath, WIRE_DIR_NAME);
            if(!mExportWirePath.exists() || !mExportWirePath.isDirectory()){
                mExportWirePath.mkdir();
            }
            for(WirePhotoEntity wirePhotoEntity : dataWithMetadata.wirePhotoEntities){
                try{
                    if(fileIsExists(mExportWirePath+"/"+ wirePhotoEntity.name)){

                    }else{
                        wirePhotoEntityList.add(wirePhotoEntity);
                        totalExportNum ++;
                    }
                } catch (Exception e) {}
            }


            /* 遍历下载树障照片 */
            mExportTreeBarrierPath = new File(mExportPath, TREE_BARRIER_DIR_NAME);
            if(!mExportTreeBarrierPath.exists() || !mExportTreeBarrierPath.isDirectory()){
                mExportTreeBarrierPath.mkdir();
            }

            for(TreePhotoEntity treePhotoEntity : dataWithMetadata.treePhotoEntities){
                try{
                    if(fileIsExists(mExportTreeBarrierPath+"/"+ treePhotoEntity.name)){

                    }else{
                        treePhotoEntities.add(treePhotoEntity);
                        totalExportNum ++;
                    }
                } catch (Exception e) {}
            }
            /* 遍历下载导线照片 */
            for(WirePhotoEntity wirePhotoEntity : wirePhotoEntityList){
                try{
                    FileOutputStream outputStream = new FileOutputStream(new File(mExportWirePath, wirePhotoEntity.name));
                    MediaDataController.getInstance().getRawPhoto(wirePhotoEntity.mediaFile, outputStream, (JZIError jziError) -> {
                        currentExportNum++;
                        refreshProgressValue();
                    });
                } catch (FileNotFoundException e) {}
            }
            for(TreePhotoEntity treePhotoEntity : treePhotoEntities){
                try{
                    FileOutputStream outputStream = new FileOutputStream(new File(mExportTreeBarrierPath, treePhotoEntity.name));
                    MediaDataController.getInstance().getRawPhoto(treePhotoEntity.mediaFile, outputStream, (JZIError jziError) -> {
                        currentExportNum++;
                        refreshProgressValue();
                    });
                } catch (FileNotFoundException e) {}
            }
            refreshProgressValue();
        });
        try {
            FileOutputStream outputStream = new FileOutputStream(new File(mExportARCSPath, String.valueOf(dataWithMetadata.terrainEntity.dataId)+".jpg"));
            DrawRadianTerrainUtils.createSectionView(800,400,dataWithMetadata.terrainEntity).compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 刷新下载进度
     */
    private void refreshProgressValue(){
        observableProgressValue.set(totalExportNum != 0 ? currentExportNum * 100 / totalExportNum : 100);
        observableProgressPercent.set(currentExportNum + "/" + totalExportNum);
        if(currentExportNum >= totalExportNum){
            /* 下载数据完毕 */
            observableProgressMessage.set(GlobalUtils.getString(R.string.export_finish));
            setCancelable(true);
        }
    }
	
    public boolean fileIsExists(String strFile)
    {
        try
        {
            File f=new File(strFile);
            if(!f.exists())
            {
                return false;
            }

        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }
}
