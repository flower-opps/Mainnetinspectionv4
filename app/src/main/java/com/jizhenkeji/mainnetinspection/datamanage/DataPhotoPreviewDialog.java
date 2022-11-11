package com.jizhenkeji.mainnetinspection.datamanage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.DialogPhotoPreviewPreviewBinding;
import com.jizhenkeji.mainnetinspection.model.entity.PhotoEntity;
import com.jizhenkeji.mainnetinspection.controller.mediadatacontroller.MediaDataController;

import dji.sdk.media.MediaFile;

public class DataPhotoPreviewDialog extends DialogFragment {

    private PhotoEntity mPhotoEntity;

    private DialogPhotoPreviewPreviewBinding mBinding;

    public DataPhotoPreviewDialog(PhotoEntity photoEntity){
        mPhotoEntity = photoEntity;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.7f);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.7f);
        window.setLayout(width, height);
        window.setBackgroundDrawable(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DialogPhotoPreviewPreviewBinding.inflate(inflater, container, false);

        Glide.with(this)
                .load(mPhotoEntity)
                .thumbnail(Glide.with(this).load(R.drawable.jzi))
                .into(mBinding.previewPicture);

        return mBinding.getRoot();
    }

}
