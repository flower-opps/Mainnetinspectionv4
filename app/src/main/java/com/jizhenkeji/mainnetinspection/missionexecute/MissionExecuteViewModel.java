package com.jizhenkeji.mainnetinspection.missionexecute;

import android.os.Bundle;
import android.util.Log;

import androidx.databinding.ObservableField;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.common.InspectionMode;
import com.jizhenkeji.mainnetinspection.common.WirePhaseNumber;
import com.jizhenkeji.mainnetinspection.dialog.ParameterConfigureDialogFragment;
import com.jizhenkeji.mainnetinspection.model.AppDatabase;
import com.jizhenkeji.mainnetinspection.model.entity.DataEntity;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;
import com.jizhenkeji.mainnetinspection.common.FlightDirection;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.controller.gimbalcontroller.GimbalController;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.ButtonEvent;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.HardwareStateListener;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.HardwareType;
import com.jizhenkeji.mainnetinspection.controller.remotehardwarecontroller.RemoteHardwareController;
import com.jizhenkeji.mainnetinspection.djiutil.DJIFlightControlUtil;
import com.jizhenkeji.mainnetinspection.mission.followwire.FollowWireMission;
import com.jizhenkeji.mainnetinspection.mission.followwire.FollowWireMissionExecutor;
import com.jizhenkeji.mainnetinspection.utils.GPSUtil;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.flightcontroller.LocationCoordinate3D;


public class MissionExecuteViewModel extends ViewModel {

    private final DecimalFormat df = new DecimalFormat("#.##");

    /**
     * 巡检状态消息提示
     */
    public final ObservableField<String> observableInspectionMessage = new ObservableField<>();

    /**
     * 巡检任务对象
     */
    public final MutableLiveData<MissionWithTowers> mMissionWithTowersLiveData = new MutableLiveData<>();

    public LiveData<MissionWithTowers> getMissionWithTowers(){
        return mMissionWithTowersLiveData;
    }

    /* 巡检数据对象 */
    private DataEntity mDataEntity;

    public void initState(){
        /* 初始化遥控器组件监听 */
        RemoteHardwareController.getInstance().addHardwareStateListener(this);
        /* 初始化巡检数据对象 */
        mDataEntity = new DataEntity();
        mDataEntity.phaseNumber = mWirePhaseNumber.getName();
        mDataEntity.aircraftName = DJIFlightControlUtil.getAircraftModel();
        LocationCoordinate3D locationCoordinate3D = DJIFlightControlUtil.getAircraftLocation();
        if(locationCoordinate3D != null){
            mDataEntity.location = new Location(locationCoordinate3D.getLatitude(), locationCoordinate3D.getLongitude(), locationCoordinate3D.getAltitude());
        }
        mDataEntity.missionId = mMissionWithTowers.missionEntity.id;
        mDataEntity.treeBarrierThreshold = mTreeBarrierDistance;
    }

    /* 当前巡检任务执行事务状态 */
    public static volatile MissionExecuteState mMissionExecuteState = MissionExecuteState.READY_TO_START;

    /* 巡检任务对象 */
    private MissionWithTowers mMissionWithTowers;

    /* 巡检模式 */
    private InspectionMode mInspectionMode;

    /* 导线相号 */
    private WirePhaseNumber mWirePhaseNumber;

    /* 起始塔号 */
    private int mStartTowerNum;

    /* 起始杆塔对象 */
    private TowerEntity mStartTowerEntity;

    /* 终止塔号 */
    private int mEndTowerNum;

    /* 终止杆塔对象 */
    private TowerEntity mEndTowerEntity;

    /* 巡检飞行速度 */
    private float mFlightSpeed;

    /* 水平离线距离 */
    private float mHorizontalDistance;

    /* 垂直离线距离 */
    private float mVerticalDistance;

    /* 树障警报阈值 */
    private int mTreeBarrierDistance;

    /* 巡检任务初始化配置线程 */
    private Timer mTimer;

    /**
     * 初始化巡检参数
     * @param inspectionParameter
     */
    public void initInspectionParameter(Bundle inspectionParameter){
        /* 初始化巡检参数 */
        mMissionWithTowers = (MissionWithTowers) inspectionParameter.getSerializable(ParameterConfigureDialogFragment.KEY_MISSION);
        mInspectionMode = (InspectionMode) inspectionParameter.getSerializable(ParameterConfigureDialogFragment.KEY_INSPECTION_MODE);
        mWirePhaseNumber = (WirePhaseNumber) inspectionParameter.getSerializable(ParameterConfigureDialogFragment.KEY_PHASE_NUMBER);
        mStartTowerNum = inspectionParameter.getInt(ParameterConfigureDialogFragment.KEY_START_TOWER_NUM);
        mEndTowerNum = inspectionParameter.getInt(ParameterConfigureDialogFragment.KEY_END_TOWER_NUM);
        mFlightSpeed = inspectionParameter.getFloat(ParameterConfigureDialogFragment.KEY_FLIGHT_SPEED);
        mHorizontalDistance = inspectionParameter.getFloat(ParameterConfigureDialogFragment.KEY_HORIZONTAL_DISTANCE);
        mVerticalDistance = inspectionParameter.getFloat(ParameterConfigureDialogFragment.KEY_VERITICAL_DISTANCE);
        mTreeBarrierDistance = inspectionParameter.getInt(ParameterConfigureDialogFragment.KEY_TREE_BARRIER_DISTANCE);
        /* 初始化巡检状态，显示杆塔之间的档距以及其它距离 */
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            long missionId = mMissionWithTowers.missionEntity.id;
            mStartTowerEntity = AppDatabase.getInstance().getMissionDao().isContains(missionId, mStartTowerNum);
            mEndTowerEntity = AppDatabase.getInstance().getMissionDao().isContains(missionId, mEndTowerNum);
            if(mStartTowerEntity != null && mEndTowerEntity != null) {
                /* 开启定时器刷新档距和飞机距离 */
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Location aircraftLocation = getLocation();
                        float totalLength = GPSUtil.calculateLineDistance(
                                mStartTowerEntity.location.latitude,
                                mStartTowerEntity.location.longitude,
                                mEndTowerEntity.location.latitude,
                                mEndTowerEntity.location.longitude
                        );
                        float distanceToStartPoint = GPSUtil.calculateLineDistance(
                                mStartTowerEntity.location.latitude,
                                mStartTowerEntity.location.longitude,
                                aircraftLocation.latitude,
                                aircraftLocation.longitude
                        );
                        float distanceToEndPoint = GPSUtil.calculateLineDistance(
                                mEndTowerEntity.location.latitude,
                                mEndTowerEntity.location.longitude,
                                aircraftLocation.latitude,
                                aircraftLocation.longitude
                        );
                        StringBuilder progressMessageBuilder = new StringBuilder();
                        progressMessageBuilder.append("档距：").append(df.format(totalLength)).append("米 ");
                        progressMessageBuilder.append("距离小号塔：").append(df.format(distanceToStartPoint)).append("米 ");
                        progressMessageBuilder.append("距离大号塔：").append(df.format(distanceToEndPoint)).append("米 ");
                        observableInspectionMessage.set(progressMessageBuilder.toString());
                    }
                }, 0, 250);
            }
        });
        /* 发布当前选定的巡检任务 */
        mMissionWithTowersLiveData.setValue(mMissionWithTowers);
    }

    /**
     * 巡检状态处理回调
     */
    private FollowWireMissionHandler.HandlerStateCallback mHandlerStateCallback = new FollowWireMissionHandler.HandlerStateCallback() {

        @Override
        public void onStart() {
            mTimer.cancel();
            observableInspectionMessage.set(GlobalUtils.getString(R.string.start_execute_mission));
        }

        @Override
        public void onProgressInfo(String msg) {
            observableInspectionMessage.set(msg);
        }

        @Override
        public void onFinished(JZIError error) {
            if(error != null){
                Log.d("Qimi", "onFinished：" + error.getDescription());
            }else{
                Log.d("Qimi", "onFinished");
            }
        }
    };

    private FollowWireMissionHandler mFollowWireMissionHandler;

    /**
     * 开始执行任务
     */
    public void startMission(){
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            /* 构建仿线巡检任务类 */
            LineInspectionPublisher lineInspectionPublisher = new LineInspectionPublisher();
            lineInspectionPublisher.setInspectionSpeed(mFlightSpeed);
            lineInspectionPublisher.setVerticalDistanceToLine(mVerticalDistance);
            lineInspectionPublisher.setHorizontalDistanceToLine(mHorizontalDistance);
            if(mStartTowerEntity != null){
                lineInspectionPublisher.setStartTowerLocation(mStartTowerEntity.location);
            }
            if(mEndTowerEntity != null){
                lineInspectionPublisher.setEndTowerLocation(mEndTowerEntity.location);
            }
            /* 构建巡检数据处理者 */
            mFollowWireMissionHandler = new FollowWireMissionHandler(mMissionWithTowers, mInspectionMode);
            mFollowWireMissionHandler.setWirePhaseNumber(mWirePhaseNumber);
            mFollowWireMissionHandler.setTreeBarrierThreshold(mTreeBarrierDistance);
            mFollowWireMissionHandler.setHandlerStateCallback(mHandlerStateCallback);
            mFollowWireMissionHandler.setStartTowerNum(mStartTowerNum);
            mFollowWireMissionHandler.setEndTowerNum(mEndTowerNum);
            mFollowWireMissionHandler.setInspectionSpeed(mFlightSpeed);
            if(mStartTowerEntity != null){
                mFollowWireMissionHandler.setStartTowerLocation(mStartTowerEntity.location);
            }
            if(mEndTowerEntity != null){
                mFollowWireMissionHandler.setEndTowerLocation(mEndTowerEntity.location);
            }
            /* 构建仿线巡检任务 */
            FollowWireMission.Builder followWireMissionBuilder = new FollowWireMission.Builder()
                    .addPublisher(lineInspectionPublisher)
                    .addHandler(mFollowWireMissionHandler);
            /* 加载仿线巡检任务 */
            JZIError loadError = FollowWireMissionExecutor.getInstance().loadMission(followWireMissionBuilder.build());
            if(loadError != null){
                observableInspectionMessage.set(loadError.getDescription());
                return;
            }
            /* 开始执行仿线巡检任务 */
            JZIError startError = FollowWireMissionExecutor.getInstance().startMission();
            if(startError != null){
                observableInspectionMessage.set(startError.getDescription());
                return;
            }
            mMissionExecuteState = MissionExecuteState.INSPECTING;
        });
    }

    /**
     * 点击Playback按钮监听，短按-开始/暂停，长按-结束任务
     * @param event
     */
    @HardwareStateListener(type = HardwareType.PLAYBACK)
    public void onClickPlayback(ButtonEvent event){
        if(event == ButtonEvent.UP){
            switch (mMissionExecuteState){
                case READY_TO_START:
                    startMission();
                    break;
                case INSPECTING:
                    FollowWireMissionExecutor.getInstance().pauseMission();
                    mMissionExecuteState = MissionExecuteState.PAUSED;
                    break;
                case PAUSED:
                    FollowWireMissionExecutor.getInstance().resumeMission();
                    mMissionExecuteState = MissionExecuteState.INSPECTING;
                    break;
            }
        }
        if(event == ButtonEvent.LONG_DOWN &&
                (mMissionExecuteState == MissionExecuteState.INSPECTING || mMissionExecuteState == MissionExecuteState.PAUSED)){
            FollowWireMissionExecutor.getInstance().stopMission();
            mMissionExecuteState = MissionExecuteState.FINISH;
        }
    }

    @HardwareStateListener(type = HardwareType.FIVED_STICK)
    public void onFivedButton(ButtonEvent event){
        if(event == ButtonEvent.DOWN){
            GimbalController.getInstance().rotatePitchTo(GimbalController.getInstance().getPitchAngle()!=0?0:-90, null);
        }
    }

    /**
     * 点击C1按钮，选择向左巡检
     * @param event
     */
    @HardwareStateListener(type = HardwareType.C1BUTTON)
    public void onClickC1Button(ButtonEvent event){
        if(event == ButtonEvent.UP){
                mFollowWireMissionHandler.setFlightDirection(FlightDirection.LEFT);
            /* 重置云台角度 */
            GimbalController.getInstance().rotatePitchTo(0, null);
        }
    }

    /**
     * 点击C2按钮，向右巡检
     * @param event
     */
    @HardwareStateListener(type = HardwareType.C2BUTTON)
    public void onClickC2Button(ButtonEvent event){
        if(event == ButtonEvent.UP){
            mFollowWireMissionHandler.setFlightDirection(FlightDirection.RIGHT);
            /* 重置云台角度 */
            GimbalController.getInstance().rotatePitchTo(0, null);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        RemoteHardwareController.getInstance().removeHardwareStateListener(this);
        FollowWireMissionExecutor.getInstance().stopMission();
    }

    /**
     * 获取当前无人机位置信息
     * @return
     */
    private Location getLocation(){
        LocationCoordinate3D endPointLocation = DJIFlightControlUtil.getAircraftLocation();
        Location location = new Location(
                endPointLocation.getLatitude(),
                endPointLocation.getLongitude(),
                endPointLocation.getAltitude()
        );
        return location;
    }

    public enum MissionExecuteState {

        READY_TO_START,

        INSPECTING,

        WAIT_CONFIGURE_DIRECTION,

        PAUSED,

        FINISH

    }

}
