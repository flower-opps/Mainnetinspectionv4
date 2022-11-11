package com.jizhenkeji.mainnetinspection.main;

import androidx.databinding.ObservableField;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import dji.common.product.Model;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainViewModel extends ViewModel {

    public final ObservableField<String> observableStateTag = new ObservableField<>(GlobalUtils.getString(R.string.init_state_tag));

    private MutableLiveData<Model> mObservableModel = new MutableLiveData<>();

    public LiveData<Model> getProductModel(){
        return mObservableModel;
    }

    public void initState(){
        initStateTag();
        initAircraftModel();
    }

    /**
     * 初始化顶部状态标签
     */
    private void initStateTag(){
        StringBuilder stateTagBuilder = new StringBuilder();
        /* 附加项目名称 */
        stateTagBuilder.append("主网巡检机  ");
        /* 附加飞机名称至消息标签 */
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if(aircraft != null && aircraft.getModel() != null){
            stateTagBuilder.append(aircraft.getModel().getDisplayName() + "  ");
        }else{
            stateTagBuilder.append("未知机型  ");
        }
        /* 显示顶部标签消息 */
        observableStateTag.set(stateTagBuilder.toString());
    }

    /**
     * 初始化无人机型号s
     */
    private void initAircraftModel(){
        Aircraft aircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        if(aircraft == null || aircraft.getModel() == null){
            return;
        }
        mObservableModel.setValue(aircraft.getModel());
    }

}
