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

    /* 附带杆塔信息的任务对象 */
    private MissionWithTowers mMissionWithTowers;

    /* 执行完任务生成出来的数据对象 */
    private static DataEntity mDataEntity;

    /* 存储拍摄的导线照片数据 */
    private static List<WirePhotoEntity> mWirePhotoEntitys;

    /* 存储拍摄的树障照片数据 */
    private List<TreePhotoEntity> mTreePhotoEntitys;

    /* 存储地形数据 */
    private TerrainEntity mTerrainEntity;

    /* 起始杆塔坐标 */
    private static Location mStartTowerLocation;

    /* 终止杆塔坐标 */
    private Location mEndTowerLocation;

    /* 巡检模式 */
    public static  InspectionMode mInspectionMode;

    /* 巡检速度 */
    private float mInspectionSpeed;

    private HandlerStateCallback mCallback;

    private static Executor mSingleExecutor;

    /* 上一个数据采集点 */
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
     * 设置起始杆塔坐标
     * @param location
     */
    public void setStartTowerLocation(Location location) {
        mStartTowerLocation = location;
    }

    /**
     * 设置终止杆塔坐标
     * @param location
     */
    public void setEndTowerLocation(Location location) {
        mEndTowerLocation = location;
    }

    /**
     * 构建仿线巡检任务处理者
     * @param missionWithTowers 关联的任务对象
     * @param inspectionMode
     */
    public FollowWireMissionHandler(MissionWithTowers missionWithTowers, InspectionMode inspectionMode){
        mMissionWithTowers = missionWithTowers;
        mInspectionMode = inspectionMode;
        /* 初始化巡检数据通用字段 */
        mDataEntity = new DataEntity();
        mDataEntity.missionId = mMissionWithTowers.missionEntity.id;
        mDataEntity.missionName = mMissionWithTowers.missionEntity.name;
        mDataEntity.voltageLevel =  mMissionWithTowers.missionEntity.voltageLevel;
        mDataEntity.manageClassName = mMissionWithTowers.missionEntity.manageClassName;
        mDataEntity.aircraftName = DJIFlightControlUtil.getAircraftModel();
        /* 初始化导线照片缓冲集合 */
        mWirePhotoEntitys = new ArrayList<>();
        /* 初始化树障照片缓冲集合 */
        mTreePhotoEntitys = new ArrayList<>();
        /* 初始化地形数据实体 */
        mTerrainEntity = new TerrainEntity();
    }

    @SubscribeEvent(event = LineInspectionPublisher.Event.ON_INSPECTING_START)
    public void onInspectionStart(){
        if(mCallback != null) {
            mCallback.onStart();
        }
        /* 设置摄像设备模式 */
        CameraController.getInstance().setCameraMode(CameraMode.SHOOT_PHOTO, null);
        /* 存储起始点位置 */
        mDataEntity.startPointLocation = getLocation();
        mDataEntity.location = mDataEntity.startPointLocation;
        /* 初始化照片采集点 */
        mLastPhotoRecordLocation = getLocation();
        /* 开启事务处理单线程池 */
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
            /* 如果存在起始终止塔坐标，则计算档距 */
            if(mStartTowerLocation != null && mEndTowerLocation != null) {
                float totalLength = GPSUtil.calculateLineDistance(
                        mStartTowerLocation.latitude,
                        mStartTowerLocation.longitude,
                        mEndTowerLocation.latitude,
                        mEndTowerLocation.longitude
                );
                progressMessageBuilder.append("档距：").append(df.format(totalLength)).append("米 ");
            } else {
                progressMessageBuilder.append("档距：暂无");
            }
            /* 计算距离小号塔/起始点位置 */
            float distanceToStartPoint;
            if(mStartTowerLocation != null) {
                distanceToStartPoint = GPSUtil.calculateLineDistance(
                        mStartTowerLocation.latitude,
                        mStartTowerLocation.longitude,
                        currentLocation.latitude,
                        currentLocation.longitude
                );
                progressMessageBuilder.append("距离小号塔：").append(df.format(distanceToStartPoint)).append("米 ");
            }else{
                distanceToStartPoint = GPSUtil.calculateLineDistance(
                        mDataEntity.startPointLocation.latitude,
                        mDataEntity.startPointLocation.longitude,
                        currentLocation.latitude,
                        currentLocation.longitude
                );
                progressMessageBuilder.append("距离起始点：").append(df.format(distanceToStartPoint)).append("米 ");
            }
            /* 如果为台账模式，则计算距离终止塔距离 */
            if(mStartTowerLocation != null && mEndTowerLocation != null) {
                float distanceToEndPoint = GPSUtil.calculateLineDistance(
                        mEndTowerLocation.latitude,
                        mEndTowerLocation.longitude,
                        currentLocation.latitude,
                        currentLocation.longitude
                );
                progressMessageBuilder.append("距离大号塔：").append(df.format(distanceToEndPoint)).append("米 ");
            }
            mCallback.onProgressInfo(progressMessageBuilder.toString());
        }
        /* 记录地形测量数据 */
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
        /* 记录导线照片数据 */
        float distanceToLastPhoto = GPSUtil.calculateLineDistance(
                mLastPhotoRecordLocation.latitude,
                mLastPhotoRecordLocation.longitude,
                currentLocation.latitude,
                currentLocation.longitude
        );
        float shootPhotoDistance = Math.max(4f, Math.abs(2 * mInspectionSpeed));
        if(distanceToLastPhoto >= shootPhotoDistance) {
            if(MissionExecuteViewModel.mMissionExecuteState!=MissionExecuteViewModel.MissionExecuteState.PAUSED) {
                Log.e("LINE_PHOTO", "触发拍照，距离：" + distanceToLastPhoto + "，小号塔：" + distanceToStartTower);
                onShootWirePhoto();
                mLastPhotoRecordLocation = currentLocation;
            }
        } else {
            Log.e("LINE_PHOTO", "距离："+distanceToLastPhoto+"，小号塔："+distanceToStartTower);
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

    /* 巡检事务锁，将异步的相机云台操作转换为同步 */
    private static ReentrantLock mTransactionLock = new ReentrantLock();

    private static Condition mShootPhotoCondition = mTransactionLock.newCondition();

    private Condition mGimbalRotateCondition = mTransactionLock.newCondition();

    /**
     * 释放拍照事务锁
     */
    private static void releaseShootPhotoLock(){
        mTransactionLock.lock();
        mShootPhotoCondition.signalAll();
        mTransactionLock.unlock();
    }

    /**
     * 释放云台转动事务锁
     */
    private void releaseGimbalRotateLock(){
        mTransactionLock.lock();
        mGimbalRotateCondition.signalAll();
        mTransactionLock.unlock();
    }

    /**
     * 执行导线拍照事务
     */
    private void executeWireShootPhotoTransaction() {
        if(mInspectionMode != InspectionMode.WIRE_MODE){
            /* 如果不是导线模式，则暂停巡检进行拍照，避免拍照冲突 */
            publishEvent(LineInspectionPublisher.Event.PAUSE_INSPECTION_CONTROL);
        }
        try{
            mTransactionLock.lock();
            shootPhoto((Map<CameraMediaFileType, MediaFile> cameraMediaFileTypeMediaFileMap) -> {
                /* 遍历所有类型的照片，保存至缓冲列表 */
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
            mShootPhotoCondition.await();       // 阻塞至拍照结果回调
        } catch (InterruptedException e) {

        }finally {
            mTransactionLock.unlock();
            if(mInspectionMode != InspectionMode.WIRE_MODE) publishEvent(LineInspectionPublisher.Event.RESUME_INSPECTION_CONTROL);
        }
    }

    /**
     * 执行间隔棒拍照事务
     */
    public static void executeSpacerBarShootPhotoTransaction() {
        if(mInspectionMode != InspectionMode.WIRE_MODE){
            /* 如果不是导线模式，则暂停巡检进行拍照，避免拍照冲突 */
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
                /* 遍历所有类型的照片，保存至缓冲列表 */
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
//            mShootPhotoCondition.await();       // 阻塞至拍照结果回调
        } catch (Exception e) {
        }finally {
            mTransactionLock.unlock();
            if(mInspectionMode != InspectionMode.WIRE_MODE) publishEvent(LineInspectionPublisher.Event.RESUME_INSPECTION_CONTROL);
        }
    }

    /**
     * 执行树障拍照事务
     */
    private void executeTreeBarrierShootPhotoTransaction(){
        publishEvent(LineInspectionPublisher.Event.PAUSE_INSPECTION_CONTROL);
        try{
            mTransactionLock.lock();
            GimbalController.getInstance().rotatePitchTo(-90, (JZIError error) -> {
                releaseGimbalRotateLock();
            });
            mGimbalRotateCondition.await();             // 等待云台完成向下转动
            Thread.sleep(500);
            shootPhoto((Map<CameraMediaFileType, MediaFile> cameraMediaFileTypeMediaFileMap) -> {
                /* 遍历所有类型的照片，保存至缓冲列表 */
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
            mShootPhotoCondition.await();               // 阻塞至拍照成功
            GimbalController.getInstance().rotatePitchTo(0, (JZIError error) -> {
                releaseGimbalRotateLock();
            });
            mGimbalRotateCondition.await();             // 等待云台回中
            Thread.sleep(500);
        } catch (InterruptedException e) {

        }finally {
            mTransactionLock.unlock();
            publishEvent(LineInspectionPublisher.Event.RESUME_INSPECTION_CONTROL);
        }
    }


    /**
     * 拍摄照片
     * @param callback
     */
    private static void shootPhoto(CommonCallback<Map<CameraMediaFileType, MediaFile>> callback){
        CameraController.getInstance().startShootPhoto(new CommonCallbackWith<Map<CameraMediaFileType, MediaFile>>() {
            @Override
            public void onSuccess(Map<CameraMediaFileType, MediaFile> cameraMediaFileTypeMediaFileMap) {
                callback.onResult(cameraMediaFileTypeMediaFileMap);
                Log.d("yolox","成功");
            }

            @Override
            public void onFailure(JZIError jziError) {
                Log.d("yolox",jziError.getDescription());
            }
        });
    }

    /**
     * 获取描述距离的照片名称
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
            return "距离小号塔" + df.format(distanceToStartPoint) + "米.jpg";
        } else {
            float distanceToStartPoint = GPSUtil.calculateLineDistance(
                    mDataEntity.startPointLocation.latitude,
                    mDataEntity.startPointLocation.longitude,
                    currentLocation.latitude,
                    currentLocation.longitude
            );
            return "距离起始点" + df.format(distanceToStartPoint) + "米.jpg";
        }
    }

    @SubscribeEvent(event = LineInspectionPublisher.Event.ON_INSPECTING_STOP)
    public void onInspectionStop(){
        if(mCallback != null) mCallback.onProgressInfo(GlobalUtils.getString(R.string.finish_execute_mission));
        /* 存储终止点位置 */
        mDataEntity.endPointLocation = getLocation();
        /* 保存巡检数据到数据库 */
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
                if(mCallback != null) mCallback.onFinished(new JZIError("存储巡检数据出错"));
            }
        });
    }

    /**
     * 获取当前无人机位置信息
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
