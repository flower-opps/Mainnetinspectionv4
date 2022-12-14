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
     * ????????????????????????
     */
    public final ObservableField<String> observableInspectionMessage = new ObservableField<>();

    /**
     * ??????????????????
     */
    public final MutableLiveData<MissionWithTowers> mMissionWithTowersLiveData = new MutableLiveData<>();

    public LiveData<MissionWithTowers> getMissionWithTowers(){
        return mMissionWithTowersLiveData;
    }

    /* ?????????????????? */
    private DataEntity mDataEntity;

    public void initState(){
        /* ?????????????????????????????? */
        RemoteHardwareController.getInstance().addHardwareStateListener(this);
        /* ??????????????????????????? */
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

    /* ???????????????????????????????????? */
    public static volatile MissionExecuteState mMissionExecuteState = MissionExecuteState.READY_TO_START;

    /* ?????????????????? */
    private MissionWithTowers mMissionWithTowers;

    /* ???????????? */
    private InspectionMode mInspectionMode;

    /* ???????????? */
    private WirePhaseNumber mWirePhaseNumber;

    /* ???????????? */
    private int mStartTowerNum;

    /* ?????????????????? */
    private TowerEntity mStartTowerEntity;

    /* ???????????? */
    private int mEndTowerNum;

    /* ?????????????????? */
    private TowerEntity mEndTowerEntity;

    /* ?????????????????? */
    private float mFlightSpeed;

    /* ?????????????????? */
    private float mHorizontalDistance;

    /* ?????????????????? */
    private float mVerticalDistance;

    /* ?????????????????? */
    private int mTreeBarrierDistance;

    /* ????????????????????????????????? */
    private Timer mTimer;

    /**
     * ?????????????????????
     * @param inspectionParameter
     */
    public void initInspectionParameter(Bundle inspectionParameter){
        /* ????????????????????? */
        mMissionWithTowers = (MissionWithTowers) inspectionParameter.getSerializable(ParameterConfigureDialogFragment.KEY_MISSION);
        mInspectionMode = (InspectionMode) inspectionParameter.getSerializable(ParameterConfigureDialogFragment.KEY_INSPECTION_MODE);
        mWirePhaseNumber = (WirePhaseNumber) inspectionParameter.getSerializable(ParameterConfigureDialogFragment.KEY_PHASE_NUMBER);
        mStartTowerNum = inspectionParameter.getInt(ParameterConfigureDialogFragment.KEY_START_TOWER_NUM);
        mEndTowerNum = inspectionParameter.getInt(ParameterConfigureDialogFragment.KEY_END_TOWER_NUM);
        mFlightSpeed = inspectionParameter.getFloat(ParameterConfigureDialogFragment.KEY_FLIGHT_SPEED);
        mHorizontalDistance = inspectionParameter.getFloat(ParameterConfigureDialogFragment.KEY_HORIZONTAL_DISTANCE);
        mVerticalDistance = inspectionParameter.getFloat(ParameterConfigureDialogFragment.KEY_VERITICAL_DISTANCE);
        mTreeBarrierDistance = inspectionParameter.getInt(ParameterConfigureDialogFragment.KEY_TREE_BARRIER_DISTANCE);
        /* ????????????????????????????????????????????????????????????????????? */
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            long missionId = mMissionWithTowers.missionEntity.id;
            mStartTowerEntity = AppDatabase.getInstance().getMissionDao().isContains(missionId, mStartTowerNum);
            mEndTowerEntity = AppDatabase.getInstance().getMissionDao().isContains(missionId, mEndTowerNum);
            if(mStartTowerEntity != null && mEndTowerEntity != null) {
                /* ?????????????????????????????????????????? */
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
                        progressMessageBuilder.append("?????????").append(df.format(totalLength)).append("??? ");
                        progressMessageBuilder.append("??????????????????").append(df.format(distanceToStartPoint)).append("??? ");
                        progressMessageBuilder.append("??????????????????").append(df.format(distanceToEndPoint)).append("??? ");
                        observableInspectionMessage.set(progressMessageBuilder.toString());
                    }
                }, 0, 250);
            }
        });
        /* ????????????????????????????????? */
        mMissionWithTowersLiveData.setValue(mMissionWithTowers);
    }

    /**
     * ????????????????????????
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
                Log.d("Qimi", "onFinished???" + error.getDescription());
            }else{
                Log.d("Qimi", "onFinished");
            }
        }
    };

    private FollowWireMissionHandler mFollowWireMissionHandler;

    /**
     * ??????????????????
     */
    public void startMission(){
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            /* ??????????????????????????? */
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
            /* ??????????????????????????? */
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
            /* ???????????????????????? */
            FollowWireMission.Builder followWireMissionBuilder = new FollowWireMission.Builder()
                    .addPublisher(lineInspectionPublisher)
                    .addHandler(mFollowWireMissionHandler);
            /* ???????????????????????? */
            JZIError loadError = FollowWireMissionExecutor.getInstance().loadMission(followWireMissionBuilder.build());
            if(loadError != null){
                observableInspectionMessage.set(loadError.getDescription());
                return;
            }
            /* ?????????????????????????????? */
            JZIError startError = FollowWireMissionExecutor.getInstance().startMission();
            if(startError != null){
                observableInspectionMessage.set(startError.getDescription());
                return;
            }
            mMissionExecuteState = MissionExecuteState.INSPECTING;
        });
    }

    /**
     * ??????Playback?????????????????????-??????/???????????????-????????????
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
     * ??????C1???????????????????????????
     * @param event
     */
    @HardwareStateListener(type = HardwareType.C1BUTTON)
    public void onClickC1Button(ButtonEvent event){
        if(event == ButtonEvent.UP){
                mFollowWireMissionHandler.setFlightDirection(FlightDirection.LEFT);
            /* ?????????????????? */
            GimbalController.getInstance().rotatePitchTo(0, null);
        }
    }

    /**
     * ??????C2?????????????????????
     * @param event
     */
    @HardwareStateListener(type = HardwareType.C2BUTTON)
    public void onClickC2Button(ButtonEvent event){
        if(event == ButtonEvent.UP){
            mFollowWireMissionHandler.setFlightDirection(FlightDirection.RIGHT);
            /* ?????????????????? */
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
     * ?????????????????????????????????
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
