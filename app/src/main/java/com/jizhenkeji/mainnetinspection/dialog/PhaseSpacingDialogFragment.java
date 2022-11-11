package com.jizhenkeji.mainnetinspection.dialog;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jizhenkeji.mainnetinspection.adapter.PhaseDataAdapter;
import com.jizhenkeji.mainnetinspection.databinding.FragmentPhaseSpacingDialogBinding;
import com.jizhenkeji.mainnetinspection.missionmanage.MissionManageActivity;
import com.jizhenkeji.mainnetinspection.missionmanage.MissionManageViewModel;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.utils.DrawPhaseDataUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhaseSpacingDialogFragment extends DialogFragment {

    private FragmentPhaseSpacingDialogBinding mBinding;
    private PhaseDataAdapter phaseDataAdapter;
    private List<DataWithMetadata> dataWithMetadataList;
    private File dataRootPath = new File(Environment.getExternalStorageDirectory(), "JZI");
    private MissionManageViewModel mViewModel;
    private List<MissionWithTowers> missionWithTowers;

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setBackgroundDrawable(null);
    }

    public void setDatawithmetadatalist(List<DataWithMetadata> list){
        dataWithMetadataList=list;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentPhaseSpacingDialogBinding.inflate(inflater, container, false);
        phaseDataAdapter = new PhaseDataAdapter(mDataClickCallback);
        phaseDataAdapter.setDataWithPhotos(dataWithMetadataList);
        mBinding.dtList.setAdapter(phaseDataAdapter);
        mViewModel = new ViewModelProvider(this).get(MissionManageViewModel.class);
        mViewModel.getMissions().observe(getViewLifecycleOwner(), (List<MissionWithTowers> m) -> {
            missionWithTowers=m;
        });
        mBinding.dtList.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.cancel.setOnClickListener((View v) -> {
            dismiss();
        });
        mBinding.confirm.setOnClickListener((View v) -> {
            dismiss();
            if (phaseDataAdapter.mCheckDataWithMetadata.size() > 1) {
                try {
                    float dis=0;
                    for(int oo=0;oo<missionWithTowers.size();oo++){
                        if(missionWithTowers.get(oo).missionEntity.id==phaseDataAdapter.mCheckDataWithMetadata.get(0).dataEntity.missionId){
                            dis=getDistance(missionWithTowers.get(oo).towerEntities.get(0).location.longitude,missionWithTowers.get(oo).towerEntities.get(0).location.latitude,missionWithTowers.get(oo).towerEntities.get(missionWithTowers.get(oo).towerEntities.size()-1).location.longitude,missionWithTowers.get(oo).towerEntities.get(missionWithTowers.get(oo).towerEntities.size()-1).location.latitude);
                        }
                    }
                    new File(Environment.getExternalStorageDirectory() + "/JZI/相间测距/").mkdirs();
                    FileOutputStream outputStream = new FileOutputStream(new File(dataRootPath,"相间测距/"+phaseDataAdapter.mCheckDataWithMetadata.get(0).dataEntity.missionName+"相间距离.jpg" ));
                    DrawPhaseDataUtils.createSectionView(800,400,phaseDataAdapter.mCheckDataWithMetadata,dis).compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    Toast.makeText(getContext(),"相间数据图片导出成功",Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "文件夹不存在或没有权限", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "相间数据图片导出失败", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(getContext(), "选取的相位数据不正确", Toast.LENGTH_SHORT).show();
            }
            ArrayList<DataWithMetadata> mCheckDataWithMetadata=new ArrayList<>();
            phaseDataAdapter.mCheckDataWithMetadata=mCheckDataWithMetadata;

        });

        return mBinding.getRoot();
    }
    private PhaseDataAdapter.DataClickCallback mDataClickCallback = new PhaseDataAdapter.DataClickCallback() {


    };

    public static float getDistance(double longitude1, double latitude1, double longitude2, double latitude2) {
        // 纬度
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        // 经度
        double lng1 = Math.toRadians(longitude1);
        double lng2 = Math.toRadians(longitude2);
        // 纬度之差
        double a = lat1 - lat2;
        // 经度之差
        double b = lng1 - lng2;
        // 计算两点距离的公式
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(b / 2), 2)));
        // 弧长乘地球半径, 返回单位: 米
        s =  s * 6378137;
        return (float) s;
    }


}