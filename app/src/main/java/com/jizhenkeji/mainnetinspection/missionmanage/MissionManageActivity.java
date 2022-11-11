package com.jizhenkeji.mainnetinspection.missionmanage;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jizhenkeji.mainnetinspection.MAppCompatActivity;
import com.jizhenkeji.mainnetinspection.MApplication;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.adapter.MissionAdapter;
import com.jizhenkeji.mainnetinspection.common.JZIError;
import com.jizhenkeji.mainnetinspection.common.Location;
import com.jizhenkeji.mainnetinspection.databinding.ActivityMissionManageBinding;
import com.jizhenkeji.mainnetinspection.datamanage.DataManageViewModel;
import com.jizhenkeji.mainnetinspection.dialog.CommonConfirmDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.DJIMapDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.MissionCreateDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.MissionExportDialogFragment;
import com.jizhenkeji.mainnetinspection.dialog.PhaseSpacingDialogFragment;
import com.jizhenkeji.mainnetinspection.missionrecord.MissionRecordActivity;
import com.jizhenkeji.mainnetinspection.model.AppDatabase;
import com.jizhenkeji.mainnetinspection.model.dao.MissionDao;
import com.jizhenkeji.mainnetinspection.model.entity.DataWithMetadata;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;
import com.jizhenkeji.mainnetinspection.report.TerrainChartBuilder;
import com.jizhenkeji.mainnetinspection.utils.FileChooseUtil;
import com.jizhenkeji.mainnetinspection.utils.GPSUtil;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MissionManageActivity extends MAppCompatActivity {

    private MissionManageViewModel mViewModel;
    private String path;
    private ActivityMissionManageBinding mBinding;
    private MissionWithTowers missionWithTowers;
    private MissionAdapter mMissionAdapter;
    private DataManageViewModel dataManageViewModel;
    private List<DataWithMetadata> dataWithMetadata;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_mission_manage);
        mBinding.createMission.setOnClickListener(this::createMission);

        dataManageViewModel = new ViewModelProvider(this).get(DataManageViewModel.class);
        dataManageViewModel.getDatas().observe(this, (List<DataWithMetadata> dataWithMetadataList) -> {
            dataWithMetadata=dataWithMetadataList;
        });

        mMissionAdapter = new MissionAdapter(mMissionClickCallback);
        mBinding.missionList.setAdapter(mMissionAdapter);
        mBinding.missionList.setLayoutManager(new LinearLayoutManager(this));

        mViewModel = new ViewModelProvider(this).get(MissionManageViewModel.class);
        mViewModel.getToastMessage().observe(this, (String msg) -> {
            Toast.makeText(MissionManageActivity.this, msg, Toast.LENGTH_SHORT).show();
        });
        mViewModel.getMissions().observe(this, (List<MissionWithTowers> missionWithTowers) -> {
            mMissionAdapter.setMissionList(missionWithTowers);
        });

        mBinding.setViewModel(mViewModel);
        mBinding.setLifecycleOwner(this);


    }

    private void createMission(View view){
        MissionCreateDialogFragment missionCreateDialogFragment = new MissionCreateDialogFragment();
        missionCreateDialogFragment.setOnMissionCreatedCallback((String lineName, String voltageLevel, String manageClassName) -> {
            mViewModel.onCreateMission(lineName, voltageLevel, manageClassName);
        });
        missionCreateDialogFragment.show(getSupportFragmentManager(), "MissionCreateDialogFragment");
    }

    private final MissionAdapter.MissionClickCallback mMissionClickCallback = new MissionAdapter.MissionClickCallback() {
        @Override
        public void onEditMission(MissionWithTowers mission) {
            Intent editMissionIntent = new Intent(MissionManageActivity.this, MissionRecordActivity.class);
            editMissionIntent.putExtra(MissionRecordActivity.KEY_MISSION_ENTITY, mission);
            startActivity(editMissionIntent);
        }

        @Override
        public void onDeleteMission(MissionWithTowers mission) {
            CommonConfirmDialogFragment confirmDialog = new CommonConfirmDialogFragment();
            confirmDialog.setDescription(GlobalUtils.getString(R.string.is_delete_mission));
            confirmDialog.setOnResultCallback(new CommonConfirmDialogFragment.OnResultCallback() {
                @Override
                public void onConfirm() {
                    deleteMission(mission);
                }

                @Override
                public void onCancel() {}
            });
            confirmDialog.show(getSupportFragmentManager(), "CommonConfirmDialogFragment");
        }

        @Override
        public void onSearchTowerPoint(MissionWithTowers mission) {
            DJIMapDialogFragment mapDialogFragment = new DJIMapDialogFragment();
            mapDialogFragment.show(getSupportFragmentManager(), "DJIMapDialogFragment");
            mapDialogFragment.getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                public void onResume() {
                    mapDialogFragment.showInspectionMission(mission);
                }
            });
        }

        @Override
        public void onExportMission(MissionWithTowers mission) {
            MissionExportDialogFragment dialogFragment = new MissionExportDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "MissionExportDialogFragment");
            if(!MApplication.MISSION_ROOT_PATH.exists() || !MApplication.MISSION_ROOT_PATH.isDirectory()){
                MApplication.MISSION_ROOT_PATH.mkdir();
            }
            dialogFragment.exportTo(MApplication.MISSION_ROOT_PATH, mission);
        }

        @Override
        public void onKmlOut(MissionWithTowers mission) {
            if(mission.towerEntities.size()>0){
                new File(Environment.getExternalStorageDirectory()+"/mainnetinsepectionKML/").mkdirs();
                createKml(Environment.getExternalStorageDirectory()+"/mainnetinsepectionKML/"+System.currentTimeMillis()+".kml",mission);
            }else{
                Toast.makeText(MissionManageActivity.this, "没有航点导出失败", Toast.LENGTH_SHORT).show();
            }

        }

        @Override
        public void onKmlIn(MissionWithTowers mission) {
            missionWithTowers=mission;
            intoFileManager();
        }

        @Override
        public void onPhaseSpacing(MissionWithTowers mission) {
            PhaseSpacingDialogFragment phaseSpacingDialogFragment = new PhaseSpacingDialogFragment();
            List<DataWithMetadata> list=new ArrayList<>();
            for(int i=0; i<dataWithMetadata.size();i++){
                if(dataWithMetadata.get(i).dataEntity.missionId==mission.missionEntity.id){
                    list.add(dataWithMetadata.get(i));
                }
            }
            phaseSpacingDialogFragment.setDatawithmetadatalist(list);
            phaseSpacingDialogFragment.show(getSupportFragmentManager(), "PhaseSpacingDialogFragment");



        }

    };


    private void intoFileManager() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//无类型限制
//        有类型限制是这样的:
//        intent.setType(“image/*”);//选择图片
//        intent.setType(“audio/*”); //选择音频
//        intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
//        intent.setType(“video/*;image/*”);//同时选择视频和图片

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 1);
    }

    private void deleteMission(MissionWithTowers missionWithTowers){
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            MissionDao missionDao = AppDatabase.getInstance().getMissionDao();
            missionDao.deleteMission(missionWithTowers.missionEntity);
            for(TowerEntity towerEntity:missionWithTowers.towerEntities){
                missionDao.deleteTower(towerEntity);
            }
        });
    }


    private void updateTower(List missionWithTowers,MissionWithTowers mt){
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            MissionDao missionDao = AppDatabase.getInstance().getMissionDao();
            for(TowerEntity towerEntity:mt.towerEntities){
                missionDao.deleteTower(towerEntity);
            }
            for(int i=0;i<missionWithTowers.size();i++){
                Element element = (Element) missionWithTowers.get(i);
                Element name=element.element("name");
                Element Point=element.element("Point");
                Element coordinates=Point.element("coordinates");
                String[] location=coordinates.getText().split(",");
                TowerEntity newTowerEntity = new TowerEntity();
                newTowerEntity.missionId = mt.missionEntity.id;
                newTowerEntity.towerNum = Long.parseLong(name.getText());
                Location aircraftLocation = new Location(Double.parseDouble(location[0]), Double.parseDouble(location[1]),Float.parseFloat(location[2]));
                newTowerEntity.location = aircraftLocation;
                missionDao.insertTower(newTowerEntity);
            }
        });

    }

    public void createKml(String filePath,MissionWithTowers missionWithTowers) {
        Element root = DocumentHelper.createElement("kml"); //根节点是kml
        Document document = DocumentHelper.createDocument(root);
        document.setXMLEncoding("UTF-8"); //给根节点kml添加属性
        root.addNamespace("", "http://www.opengis.net/kml/2.2");
        root.addNamespace("atom", "http://www.w3.org/2005/Atom");
        root.addNamespace("gx", "http://www.google.com/kml/ext/2.2"); //给根节点kml添加子节点
        root.addElement("name").addText("JZIMainnetinspection");
        Element documentElement = root.addElement("Document");
        Element folderDe = documentElement.addElement("Folder");

        folderDe.addElement("Name").addText("JZI主网航点");

        for(int i=0;i<missionWithTowers.towerEntities.size();i++){
            Element PlacemarkDe = folderDe.addElement("Placemark");
            PlacemarkDe.addElement("name").addText(missionWithTowers.towerEntities.get(i).towerNum+"");
            Element Point = PlacemarkDe.addElement("Point");
            Point.addElement("coordinates").addText(missionWithTowers.towerEntities.get(i).location.longitude+","+missionWithTowers.towerEntities.get(i).location.latitude+","+missionWithTowers.towerEntities.get(i).location.altitude);
        }
        try {
            Writer fileWriter = new FileWriter(filePath); //换行
            OutputFormat format = new OutputFormat();
            format.setEncoding("UTF-8");
            format.setNewlines(true); // 生成缩进
            format.setIndent(true); //dom4j提供了专门写入文件的对象
            XMLWriter xmlWriter = new XMLWriter(fileWriter, format);
            xmlWriter.write(document);
            xmlWriter.flush();
            xmlWriter.close();
            Toast.makeText(this, "生成成功", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MissionManageActivity.this, "航点导出失败"+e, Toast.LENGTH_SHORT).show();
        }
    }

    public void readKml(String filePath) {
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            File file = new File(filePath);
            document = reader.read(file);
            Element rootElement = document.getRootElement();
            List responseDocument = rootElement.element("Document").element("Folder").elements("Placemark");
            for (int i = 0; i < responseDocument.size(); i++) {
                updateTower(responseDocument,missionWithTowers);
            }
            Toast.makeText(MissionManageActivity.this, "读取kml导入成功", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(MissionManageActivity.this, "读取kml有误导入失败:"+e, Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {//4.4以后
                path = FileChooseUtil.getPath(this, uri);
            } else {//4.4以下下系统调用方法
                path = FileChooseUtil.getRealPathFromURI(uri);

            }
            if(path!=null){
            readKml(path);
            }
        }

    }

}