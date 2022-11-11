package com.jizhenkeji.mainnetinspection.missionexecute;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;

import dji.common.product.Model;
import dji.sdk.camera.VideoFeeder;
import android.graphics.Bitmap;
import android.view.TextureView;

import com.jizhenkeji.mainnetinspection.MAppCompatActivity;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.ActivityMissionExecuteBinding;
import com.jizhenkeji.mainnetinspection.dialog.DJIMapDialogFragment;
import com.jizhenkeji.mainnetinspection.main.MainActivity;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.yolo5detc.Yolox;

import java.util.Timer;
import java.util.TimerTask;

import dji.sdk.codec.DJICodecManager;
import dji.ux.widget.FPVOverlayWidget;

public class MissionExecuteActivity extends MAppCompatActivity implements TextureView.SurfaceTextureListener {

    public static final String INSPECTION_PARAMETER = "INSPECTION_PARAMETER";

    private MissionExecuteViewModel mViewModel;

    private ActivityMissionExecuteBinding mBinding;

    private Yolox yolox = new Yolox();

    private DJICodecManager codecManager = null;
    private VideoFeeder.VideoDataListener videoDataListener = null;

    TextureView mVideoSurface;
    int height;
    int width;

    public static Bitmap YOLO5_result_bitmap = null;
//
    private Timer timer = null;
    private TimerTask task = null;

    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                mBinding.YOLO5Image.setImageBitmap(YOLO5_result_bitmap);
            }
        }
    };


    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
        if (codecManager == null) {
            codecManager = new DJICodecManager(getApplicationContext(), surfaceTexture, i, i1);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_mission_execute);

        mViewModel = new ViewModelProvider(this).get(MissionExecuteViewModel.class);
        /* 获取巡检飞行参数 */
        Bundle inspectionParameter = getIntent().getBundleExtra(MissionExecuteActivity.INSPECTION_PARAMETER);
        mViewModel.initInspectionParameter(inspectionParameter);
        mViewModel.initState();
        mViewModel.mMissionWithTowersLiveData.observe(this, (MissionWithTowers missionWithTowers) -> {
            mBinding.towerLabelView.setMissionWithTowers(missionWithTowers);
        });

        // 初始化加载yolov5n
        yolox.loadModel(getAssets(), 0);

        mVideoSurface = findViewById(R.id.ttv_preview);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
            // This callback is for
            videoDataListener = (bytes, size) -> {
                if (null != codecManager) {
                    Log.d("YOLOv5","codecManager get frame size:" + size);
                    codecManager.sendDataToDecoder(bytes, size);
                }
            };
        }
        try {
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoDataListener);
            Log.d("YOLOv5", "VideoFeeder SUCCESS");
        } catch (Exception ignored) {
            Log.d("YOLOv5", "VideoFeeder ERROR:" + ignored.toString());
        }

        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                try {
                    //Log.e("YOLOv5", "timer run!");
                    if (null != codecManager) {
                        //Log.e("YOLOv5", "timer run, codecManager get!");
                        codecManager.getBitmap(pic -> {
                            if (pic != null){
                                Log.e("YOLOv5", "codecManager get bitmap!");
                                long start_t = System.nanoTime();
                                YOLO5_result_bitmap = yolox.detector(pic, Bitmap.Config.ARGB_8888);
                                //YOLO5_result_bitmap = pic;
                                Log.e("YOLOv5", ((System.nanoTime() - start_t) / 1000 / 1000) + "毫秒");
                                if (YOLO5_result_bitmap != null) {
                                    Message msg = new Message();
                                    msg.what = 1;
                                    handler.sendMessage(msg);
                                } else {
                                    Log.e("YOLOv5", "传入图片错误");
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e("YOLOv5", e.getMessage());
                }
            }
        };

        mBinding.map.setOnClickListener(view -> {
            DJIMapDialogFragment dialogFragment = new DJIMapDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "DJIMapDialogFragment");
        });

        /* 设置屏幕网格 */
        mBinding.fpvOverlayWidget.setGridOverlayEnabled(true);
        mBinding.fpvOverlayWidget.setCurrentGridOverlayType(FPVOverlayWidget.GridOverlayType.PARALLEL_DIAGONAL);

        mBinding.setViewModel(mViewModel);
        mBinding.setLifecycleOwner(this);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        height = dm.heightPixels;
        width = dm.widthPixels;

        if(MainActivity.mModel== Model.MATRICE_300_RTK) {
            mBinding.ivLine.setImageBitmap(drawLines());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        timer.schedule(task, 0, Build.VERSION.SDK_INT<30?300:100);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    public Bitmap drawLines(){
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p=new Paint();
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(2);
        int dx = Math.round(width/3.0f);
        int dy = Math.round(height/3.0f);
        canvas.drawLine(0, 0, width, height, p);
        canvas.drawLine(width, 0, 0, height, p);

        canvas.drawLine(0, dy, width, dy, p);
        canvas.drawLine(0, 2*dy, width, 2*dy, p);

        canvas.drawLine(dx, 0, dx, height, p);
        canvas.drawLine(2*dx, 0, 2*dx, height, p);
        return bitmap;
    }
}