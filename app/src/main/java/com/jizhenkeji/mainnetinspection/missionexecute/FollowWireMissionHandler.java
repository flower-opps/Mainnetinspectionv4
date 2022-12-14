package com.jizhenkeji.mainnetinspection.missionexecute;


import android.graphics.Camera;
import android.graphics.PointF;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraControl;

import com.jizhenkeji.mainnetinspection.MApplication;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.common.InspectionMode;
import com.jizhenkeji.mainnetinspection.common.TerrainInformation;
import com.jizhenkeji.mainnetinspection.common.WirePhaseNumber;
import com.jizhenkeji.mainnetinspection.initsdk.InitViewModel;
import com.jizhenkeji.mainnetinspection.mission.followwire.FollowWireMissionExecutor;
import com.jizhenkeji.mainnetinspection.model.AppDatabase;
import com.jizhenkeji.mainnetinspection.model.entity.DataEntity;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.model.entity.TerrainEntity;
import com.jizhenkeji.mainnetinspection.model.entity.TreePhotoEntity;
import com.jizhenkeji.mainnetinspection.model.entity.WirePhotoEntity;
import com.jizhenkeji.mainnetinspection.common.CommonCallback;
import com.jizhenkeji.mainnetinspection.common.CommonCallbackWith;
import com.jizhenkeji.mainnetinspection.common.FlightDirection;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraController;
import com.jizhenkeji.mainnetinspection.controller.cameracontroller.CameraMediaFileType;
import com.jizhenkeji.mainnetinspection.controller.gimbalcontroller.GimbalController;
import com.jizhenkeji.mainnetinspection.djiutil.DJIFlightControlUtil;
import com.jizhenkeji.mainnetinspection.mission.followwire.BaseHandler;
import com.jizhenkeji.mainnetinspection.mission.followwire.SubscribeEvent;
import com.jizhenkeji.mainnetinspection.radar.RadarAdapter;
import com.jizhenkeji.mainnetinspection.utils.GPSUtil;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import dji.common.camera.SettingsDefinitions.CameraMode;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.util.CommonCallbacks;
import dji.sdk.media.MediaFile;


public class FollowWireMissionHandler extends BaseHandler {

    private static final DecimalFormat df = new DecimalFormat("#.##");

    /* ????????????????????????????????? */
    private MissionWithTowers mMissionWithTowers;

    /* ?????????????????????????????????????????? */
    private static DataEntity mDataEntity;

    /* ????????????????????????????????? */
    private static List<WirePhotoEntity> mWirePhotoEntitys;

    /* ????????????????????????????????? */
    private List<TreePhotoEntity> mTreePhotoEntitys;

    /* ?????????????????? */
    private TerrainEntity mTerrainEntity;

    /* ?????????????????? */
    private static Location mStartTowerLocation;

    /* ?????????????????? */
    private Location mEndTowerLocation;

    /* ???????????? */
    public static  InspectionMode mInspectionMode;

    /* ???????????? */
    private float mInspectionSpeed;

    private HandlerStateCallback mCallback;

    private static Executor mSingleExecutor;

    /* ???????????????????????? */
    private Location mLastPhotoRecordLocation;

    private boolean mIsAllowChooseFlightDirection = false;

    private static long last=0;

    public void setHandlerStateCallback(HandlerStateCallback callback){
        mCallback = callback;
    }

    public void setWirePhaseNumber(@NonNull WirePhaseNumber wirePhaseNumber){
        mDataEntity.phaseNumber = wirePhaseNumber.getName();
    }

    public void setStartTowerNum(int startTowerNum){
        mDataEntity.startTowerNum = startTowerNum;
    }

    public void setEndTowerNum(int endTowerNum){
        mDataEntity.endTowerNum = endTowerNum;
    }

    public void setTreeBarrierThreshold(int treeBarrierThreshold){
        mDataEntity.treeBarrierThreshold = treeBarrierThreshold;
    }

    public void setInspectionSpeed(float inspectionSpeed) {
        mInspectionSpeed = inspectionSpeed;
    }

    /**
     * ????????????????????????
     * @param location
     */
    public void setStartTowerLocation(Location location) {
        mStartTowerLocation = location;
    }

    /**
     * ????????????????????????
     * @param location
     */
    public void setEndTowerLocation(Location location) {
        mEndTowerLocation = location;
    }

    /**
     * ?????????????????????????????????
     * @param missionWithTowers ?????????????????????
     * @param inspectionMode
     */
    public FollowWireMissionHandler(MissionWithTowers missionWithTowers, InspectionMode inspectionMode){
        mMissionWithTowers = missionWithTowers;
        mInspectionMode = inspectionMode;
        /* ????????????????????????????????? */
        mDataEntity = new DataEntity();
        mDataEntity.missionId = mMissionWithTowers.missionEntity.id;
        mDataEntity.missionName = mMissionWithTowers.missionEntity.name;
        mDataEntity.voltageLevel =  mMissionWithTowers.missionEntity.voltageLevel;
        mDataEntity.manageClassName = mMissionWithTowers.missionEntity.manageClassName;
        mDataEntity.aircraftName = DJIFlightControlUtil.getAircraftModel();
        /* ????????????????????????????????? */
        mWirePhotoEntitys = new ArrayList<>();
        /* ????????????????????????????????? */
        mTreePhotoEntitys = new ArrayList<>();
        /* ??????????????????????????? */
        mTerrainEntity = new TerrainEntity();
    }

    @SubscribeEvent(event = LineInspectionPublisher.Event.ON_INSPECTING_START)
    public void onInspectionStart(){
        if(mCallback != null) {
            mCallback.onStart();
        }
        /* ???????????????????????? */
        CameraController.getInstance().setCameraMode(CameraMode.SHOOT_PHOTO, null);
        /* ????????????????????? */
        mDataEntity.startPointLocation = getLocation();
        mDataEntity.location = mDataEntity.startPointLocation;
        /* ???????????????????????? */
        mLastPhotoRecordLocation = getLocation();
        /* ?????????????????????????????? */
        mSingleExecutor = Executors.newSingleThreadExecutor();
    }

    public void setFlightDirection(FlightDirection flightDirection){
        if(mIsAllowChooseFlightDirection){
            switch (flightDirection){
                case LEFT:
                    publishEvent(LineInspectionPublisher.Event.CONFIGURE_LEFT_DIRECTION);
                    break;
                case RIGHT:
                    publishEvent(LineInspectionPublisher.Event.CONFIGURE_RIGHT_DIRECTION);
                    break;
                default:
                    return;
            }
            mIsAllowChooseFlightDirection = true;
        }
    }

    @SubscribeEvent(event = LineInspectionPublisher.Event.ON_WAIT_INSPECTION_DIRECTION)
    public void onChooseDirection(){
        if(mCallback != null) mCallback.onProgressInfo(GlobalUtils.getString(R.string.choose_inspection_direction));
        mIsAllowChooseFlightDirection = true;
    }

    @SubscribeEvent(event = LineInspectionPublisher.Event.ON_INSPECTING)
    public void onInspecting(){
        Location currentLocation = getLocation();
        if(mDataEntity.startPointLocation != null && mCallback != null){
            StringBuilder progressMessageBuilder = new StringBuilder();
            /* ??????????????????????????????????????????????????? */
            if(mStartTowerLocation != null && mEndTowerLocation != null) {
                float totalLength = GPSUtil.calculateLineDistance(
                        mStartTowerLocation.latitude,
                        mStartTowerLocation.longitude,
                        mEndTowerLocation.latitude,
                        mEndTowerLocation.longitude
                );
                progressMessageBuilder.append("?????????").append(df.format(totalLength)).append("??? ");
            } else {
                progressMessageBuilder.append("???????????????");
            }
            /* ?????????????????????/??????????????? */
            float distanceToStartPoint;
            if(mStartTowerLocation != null) {
                distanceToStartPoint = GPSUtil.calculateLineDistance(
                        mStartTowerLocation.latitude,
                        mStartTowerLocation.longitude,
                        currentLocation.latitude,
                        currentLocation.longitude
                );
                progressMessageBuilder.append("??????????????????").append(df.format(distanceToStartPoint)).append("??? ");
            }else{
                distanceToStartPoint = GPSUtil.calculateLineDistance(
                        mDataEntity.startPointLocation.latitude,
                        mDataEntity.startPointLocation.longitude,
                        currentLocation.latitude,
                        currentLocation.longitude
                );
                progressMessageBuilder.append("??????????????????").append(df.format(distanceToStartPoint)).append("??? ");
            }
            /* ?????????????????????????????????????????????????????? */
            if(mStartTowerLocation != null && mEndTowerLocation != null) {
                float distanceToEndPoint = GPSUtil.calculateLineDistance(
                        mEndTowerLocation.latitude,
                        mEndTowerLocation.longitude,
                        currentLocation.latitude,
                        currentLocation.longitude
                );
                progressMessageBuilder.append("??????????????????").append(df.format(distanceToEndPoint)).append("??? ");
            }
            mCallback.onProgressInfo(progressMessageBuilder.toString());
        }
        /* ???????????????????????? */
        TerrainInformation terrainInformation = new TerrainInformation();
        terrainInformation.aircraftLocation = getLocation();
        terrainInformation.wireHeight = getLocation().altitude + RadarAdapter.getRadarManager().getLineY();
        float distanceToStartTower = GPSUtil.calculateLineDistance(
                mStartTowerLocation.latitude,
                mStartTowerLocation.longitude,
                currentLocation.latitude,
                currentLocation.longitude
        );
        terrainInformation.trumpetTowerDistance = distanceToStartTower;
        terrainInformation.barrierDistance = RadarAdapter.getRadarManager().getLineTreeY1() * RadarAdapter.getRadarManager().getLineTreeY1()
                                           + RadarAdapter.getRadarManager().getLineTreeX1() * RadarAdapter.getRadarManager().getLineTreeX1();
        mTerrainEntity.terrainInformations.add(terrainInformation);
        /* ???????????????????????? */
        float distanceToLastPhoto = GPSUtil.calculateLineDistance(
                mLastPhotoRecordLocation.latitude,
                mLastPhotoRecordLocation.longitude,
                currentLocation.latitude,
                currentLocation.longitude
        );
        float shootPhotoDistance = Math.max(4f, Math.abs(2 * mInspectionSpeed));
        if(distanceToLastPhoto >= shootPhotoDistance) {
            if(MissionExecuteViewModel.mMissionExecuteState!=MissionExecuteViewModel.MissionExecuteState.PAUSED) {
                Log.e("LINE_PHOTO", "????????????????????????" + distanceToLastPhoto + "???????????????" + distanceToStartTower);
                onShootWirePhoto();
                mLastPhotoRecordLocation = currentLocation;
            }
        } else {
            Log.e("LINE_PHOTO", "?????????"+distanceToLastPhoto+"???????????????"+distanceToStartTower);
        }
    }

    @SubscribeEvent(event = LineInspectionPublisher.Event.ON_INSPECTING_PAUSE)
    public void onInspectionPause(){
        if(mCallback != null) mCallback.onProgressInfo(GlobalUtils.getString(R.string.pause_execute_mission));
    }



    public static void onShootWirePhoto(){
        if(!mTransactionLock.isHeldByCurrentThread()){
            if(mSingleExecutor!=null) {
                mSingleExecutor.execute(FollowWireMissionHandler::executeSpacerBarShootPhotoTransaction);
            }
        }
//        long now=System.currentTimeMillis();
    }

    public void onShootTreeBarrierPhoto(){
        mSingleExecutor.execute(this::executeTreeBarrierShootPhotoTransaction);
    }

    /* ??????????????????????????????????????????????????????????????? */
    private static ReentrantLock mTransactionLock = new ReentrantLock();

    private static Condition mShootPhotoCondition = mTransactionLock.newCondition();

    private Condition mGimbalRotateCondition = mTransactionLock.newCondition();

    /**
     * ?????????????????????
     */
    private static void releaseShootPhotoLock(){
        mTransactionLock.lock();
        mShootPhotoCondition.signalAll();
        mTransactionLock.unlock();
    }

    /**
     * ???????????????????????????
     */
    private void releaseGimbalRotateLock(){
        mTransactionLock.lock();
        mGimbalRotateCondition.signalAll();
        mTransactionLock.unlock();
    }

    /**
     * ????????????????????????
     */
    private void executeWireShootPhotoTransaction() {
        if(mInspectionMode != InspectionMode.WIRE_MODE){
            /* ??????????????????????????????????????????????????????????????????????????? */
            publishEvent(LineInspectionPublisher.Event.PAUSE_INSPECTION_CONTROL);
        }
        try{
            mTransactionLock.lock();
            shootPhoto((Map<CameraMediaFileType, MediaFile> cameraMediaFileTypeMediaFileMap) -> {
                /* ??????????????????????????????????????????????????? */
                for(CameraMediaFileType cameraMediaFileType : cameraMediaFileTypeMediaFileMap.keySet()){
                    WirePhotoEntity wirePhotoEntity = new WirePhotoEntity();
                    wirePhotoEntity.mediaFileType = cameraMediaFileType;
                    wirePhotoEntity.mediaFile = cameraMediaFileTypeMediaFileMap.get(cameraMediaFileType);;
                    wirePhotoEntity.location = getLocation();
                    wirePhotoEntity.name = getDistanceDescription();
                    mWirePhotoEntitys.add(wirePhotoEntity);
                }
                releaseShootPhotoLock();
            });
            mShootPhotoCondition.await();       // ???????????????????????????
        } catch (InterruptedException e) {

        }finally {
            mTransactionLock.unlock();
            if(mInspectionMode != InspectionMode.WIRE_MODE) publishEvent(LineInspectionPublisher.Event.RESUME_INSPECTION_CONTROL);
        }
    }

    /**
     * ???????????????????????????
     */
    public static void executeSpacerBarShootPhotoTransaction() {
        if(mInspectionMode != InspectionMode.WIRE_MODE){
            /* ??????????????????????????????????????????????????????????????????????????? */
            publishEvent(LineInspectionPublisher.Event.PAUSE_INSPECTION_CONTROL);
        }
        if(FollowWireMissionExecutor.mState!= FollowWireMissionExecutor.FollowWireMissionExecutorState.EXECUTING){
            return;
        }

        try{
            mTransactionLock.lock();
            if(MissionExecuteActivity.YOLO5_result_bitmap!=null) {
                CameraController.getInstance().getCamera().setFocusTarget(new PointF(MissionExecuteActivity.YOLO5_result_bitmap.getWidth(), MissionExecuteActivity.YOLO5_result_bitmap.getHeight()), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {

                    }
                });
            }
            shootPhoto((Map<CameraMediaFileType, MediaFile> cameraMediaFileTypeMediaFileMap) -> {
                /* ??????????????????????????????????????????????????? */
                for(CameraMediaFileType cameraMediaFileType : cameraMediaFileTypeMediaFileMap.keySet()){
                    WirePhotoEntity wirePhotoEntity = new WirePhotoEntity();
                    wirePhotoEntity.mediaFileType = cameraMediaFileType;
                    wirePhotoEntity.mediaFile = cameraMediaFileTypeMediaFileMap.get(cameraMediaFileType);;
                    wirePhotoEntity.location = getLocation();
                    wirePhotoEntity.name = getDistanceDescription();
                    mWirePhotoEntitys.add(wirePhotoEntity);
                }
                releaseShootPhotoLock();
            });
//            mShootPhotoCondition.await();       // ???????????????????????????
        } catch (Exception e) {
        }finally {
            mTransactionLock.unlock();
            if(mInspectionMode != InspectionMode.WIRE_MODE) publishEvent(LineInspectionPublisher.Event.RESUME_INSPECTION_CONTROL);
        }
    }

    /**
     * ????????????????????????
     */
    private void executeTreeBarrierShootPhotoTransaction(){
        publishEvent(LineInspectionPublisher.Event.PAUSE_INSPECTION_CONTROL);
        try{
            mTransactionLock.lock();
            GimbalController.getInstance().rotatePitchTo(-90, (JZIError error) -> {
                releaseGimbalRotateLock();
            });
            mGimbalRotateCondition.await();             // ??????????????????????????????
            Thread.sleep(500);
            shootPhoto((Map<CameraMediaFileType, MediaFile> cameraMediaFileTypeMediaFileMap) -> {
                /* ??????????????????????????????????????????????????? */
                for(CameraMediaFileType cameraMediaFileType : cameraMediaFileTypeMediaFileMap.keySet()){
                    TreePhotoEntity treePhotoEntity = new TreePhotoEntity();
                    treePhotoEntity.mediaFileType = cameraMediaFileType;
                    treePhotoEntity.mediaFile = cameraMediaFileTypeMediaFileMap.get(cameraMediaFileType);;
                    treePhotoEntity.location = getLocation();
                    treePhotoEntity.name = getDistanceDescription();
                    treePhotoEntity.treeBarrierXDistance = RadarAdapter.getRadarManager().getLineTreeX1();
                    treePhotoEntity.treeBarrierYDistance = RadarAdapter.getRadarManager().getLineTreeY1();
                    mTreePhotoEntitys.add(treePhotoEntity);
                }
                releaseShootPhotoLock();
            });
            mShootPhotoCondition.await();               // ?????????????????????
            GimbalController.getInstance().rotatePitchTo(0, (JZIError error) -> {
                releaseGimbalRotateLock();
            });
            mGimbalRotateCondition.await();             // ??????????????????
            Thread.sleep(500);
        } catch (InterruptedException e) {

        }finally {
            mTransactionLock.unlock();
            publishEvent(LineInspectionPublisher.Event.RESUME_INSPECTION_CONTROL);
        }
    }


    /**
     * ????????????
     * @param callback
     */
    private static void shootPhoto(CommonCallback<Map<CameraMediaFileType, MediaFile>> callback){
        CameraController.getInstance().startShootPhoto(new CommonCallbackWith<Map<CameraMediaFileType, MediaFile>>() {
            @Override
            public void onSuccess(Map<CameraMediaFileType, MediaFile> cameraMediaFileTypeMediaFileMap) {
                callback.onResult(cameraMediaFileTypeMediaFileMap);
                Log.d("yolox","??????");
            }

            @Override
            public void onFailure(JZIError jziError) {
                Log.d("yolox",jziError.getDescription());
            }
        });
    }

    /**
     * ?????????????????????????????????
     * @return
     */
    private static String getDistanceDescription(){
        Location currentLocation = getLocation();
        if(mStartTowerLocation != null) {
            float distanceToStartPoint = GPSUtil.calculateLineDistance(
                    mDataEntity.startPointLocation.latitude,
                    mDataEntity.startPointLocation.longitude,
                    currentLocation.latitude,
                    currentLocation.longitude
            );
            return "???????????????" + df.format(distanceToStartPoint) + "???.jpg";
        } else {
            float distanceToStartPoint = GPSUtil.calculateLineDistance(
                    mDataEntity.startPointLocation.latitude,
                    mDataEntity.startPointLocation.longitude,
                    currentLocation.latitude,
                    currentLocation.longitude
            );
            return "???????????????" + df.format(distanceToStartPoint) + "???.jpg";
        }
    }

    @SubscribeEvent(event = LineInspectionPublisher.Event.ON_INSPECTING_STOP)
    public void onInspectionStop(){
        if(mCallback != null) mCallback.onProgressInfo(GlobalUtils.getString(R.string.finish_execute_mission));
        /* ????????????????????? */
        mDataEntity.endPointLocation = getLocation();
        /* ?????????????????????????????? */
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            try{
                long dataId = AppDatabase.getInstance().getDataDao().insertData(mDataEntity);
                for(WirePhotoEntity wirePhotoEntity : mWirePhotoEntitys){
                    wirePhotoEntity.dataId = dataId;
                    AppDatabase.getInstance().getDataDao().insertWirePhoto(wirePhotoEntity);
                }
                for(TreePhotoEntity treePhotoEntity : mTreePhotoEntitys){
                    treePhotoEntity.dataId = dataId;
                    AppDatabase.getInstance().getDataDao().insertTreePhoto(treePhotoEntity);
                }
                mTerrainEntity.dataId = dataId;
                AppDatabase.getInstance().getDataDao().insertTerrain(mTerrainEntity);
                if(mCallback != null) mCallback.onFinished(null);
            }catch (Exception e){
                if(mCallback != null) mCallback.onFinished(new JZIError("????????????????????????"));
            }
        });
    }

    /**
     * ?????????????????????????????????
     * @return
     */
    private static Location getLocation(){
        LocationCoordinate3D currentLocation = DJIFlightControlUtil.getAircraftLocation();
        Location location = new Location(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                currentLocation.getAltitude()
        );
        return location;
    }

    public interface HandlerStateCallback {

        void onStart();

        void onProgressInfo(String msg);

        void onFinished(JZIError error);

    }

}
