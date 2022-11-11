package com.jizhenkeji.mainnetinspection.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.ObservableField;
import androidx.fragment.app.DialogFragment;

import com.jizhenkeji.mainnetinspection.databinding.DialogCommonConfirmBinding;


public class CommonConfirmDialogFragment extends DialogFragment {

    public final ObservableField<String> observableDescription = new ObservableField<>();

    private DialogCommonConfirmBinding mBinding;

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setBackgroundDrawable(null);
    }

    public void setDescription(String description){
        observableDescription.set(description);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DialogCommonConfirmBinding.inflate(inflater, container, false);
        mBinding.setDescription(observableDescription);
        mBinding.cancel.setOnClickListener((View v) -> {
            if(mCallback != null) mCallback.onCancel();
            dismiss();
        });
        mBinding.confirm.setOnClickListener((View v) -> {
            if(mCallback != null) mCallback.onConfirm();
            dismiss();
        });
        return mBinding.getRoot();
    }

    private OnResultCallback mCallback;

    public void setOnResultCallback(OnResultCallback callback){
        mCallback = callback;
    }

    public interface OnResultCallback{

        void onConfirm();

        void onCancel();

    }

}
