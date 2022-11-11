package com.jizhenkeji.mainnetinspection.common;

public interface CommonCallbackWith<T> {

    void onSuccess(T obj);

    void onFailure(JZIError error);

}
