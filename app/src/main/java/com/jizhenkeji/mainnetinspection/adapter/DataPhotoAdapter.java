package com.jizhenkeji.mainnetinspection.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.DataPhotoItemBinding;
import com.jizhenkeji.mainnetinspection.model.entity.PhotoEntity;
import java.util.List;

import dji.sdk.media.MediaFile;

public class DataPhotoAdapter extends RecyclerView.Adapter<DataPhotoAdapter.DataPhotoHolder> {

    private List<PhotoEntity> mPhotoEntitys;

    private OnClickPhotoCallback mCallback;

    public DataPhotoAdapter(@NonNull OnClickPhotoCallback callback){
        mCallback = callback;
    }

    public void setPhotoEntitys(List<PhotoEntity> photoEntitys){
        mPhotoEntitys = photoEntitys;
        notifyDataSetChanged();
    }

    @Override
    public DataPhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DataPhotoItemBinding dataPhotoItemBinding = DataPhotoItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new DataPhotoHolder(dataPhotoItemBinding);
    }

    @Override
    public void onBindViewHolder(DataPhotoHolder holder, int position) {
        Glide.with(holder.binding.getRoot())
                .load(mPhotoEntitys.get(position))
                .thumbnail(Glide.with(holder.binding.getRoot()).load(R.drawable.jzi))
                .into(holder.binding.dataPhoto);
        holder.binding.photoCard.setOnClickListener(view -> mCallback.onClickPhoto(mPhotoEntitys.get(position)));
    }

    @Override
    public int getItemCount() {
        return mPhotoEntitys != null ? mPhotoEntitys.size() : 0;
    }

    public static class DataPhotoHolder extends RecyclerView.ViewHolder {

        private final DataPhotoItemBinding binding;

        public DataPhotoHolder(DataPhotoItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnClickPhotoCallback {

        void onClickPhoto(PhotoEntity photoEntity);

    }

}
