package com.jizhenkeji.mainnetinspection.datamanage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jizhenkeji.mainnetinspection.adapter.DataPhotoAdapter;
import com.jizhenkeji.mainnetinspection.databinding.FragmnetDataManageBinding;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;
import com.jizhenkeji.mainnetinspection.model.entity.PhotoEntity;

import java.util.ArrayList;
import java.util.List;

public class DataManageFragment extends Fragment {

    private final int LIST_SPAN_COUNT = 4;

    private FragmnetDataManageBinding mBinding;

    private DataWithMetadata mDataWithMetadata;

    private DataPhotoAdapter mAdapter;

    private boolean isDisplayWirePhoto = true;

    private boolean isDisplayTreeBarrierPhoto = true;

    public void setDataWithMetadata(DataWithMetadata dataWithMetadata){
        mDataWithMetadata = dataWithMetadata;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmnetDataManageBinding.inflate(inflater, container, false);

        mBinding.wirePhotoCheckBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            isDisplayWirePhoto = isChecked;
            initDisplayState();
        });
        mBinding.treeBarrierPhotoCheckBox.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            isDisplayTreeBarrierPhoto = isChecked;
            initDisplayState();
        });

        mAdapter = new DataPhotoAdapter(mPhotoClickCallback);

        mBinding.dataPhotoList.setAdapter(mAdapter);
        mBinding.dataPhotoList.setLayoutManager(new GridLayoutManager(getContext(), LIST_SPAN_COUNT));

        mBinding.closeButton.setOnClickListener((View v) -> {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        initDisplayState();

        return mBinding.getRoot();
    }

    public void initDisplayState(){
        if(mAdapter == null){
            return;
        }
        List<PhotoEntity> photoEntities = new ArrayList<>();
        if(isDisplayWirePhoto){
            photoEntities.addAll(mDataWithMetadata.wirePhotoEntities);
        }
        if(isDisplayTreeBarrierPhoto){
            photoEntities.addAll(mDataWithMetadata.treePhotoEntities);
        }
        mAdapter.setPhotoEntitys(photoEntities);
    }

    public DataPhotoAdapter.OnClickPhotoCallback mPhotoClickCallback = new DataPhotoAdapter.OnClickPhotoCallback() {
        @Override
        public void onClickPhoto(PhotoEntity photoEntity) {
            DataPhotoPreviewDialog dialogFragment = new DataPhotoPreviewDialog(photoEntity);
            dialogFragment.show(getChildFragmentManager(), "DataPhotoPreviewDialog");
        }
    };

}
