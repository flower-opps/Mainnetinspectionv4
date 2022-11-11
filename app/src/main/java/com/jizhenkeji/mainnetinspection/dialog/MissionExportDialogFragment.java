package com.jizhenkeji.mainnetinspection.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.fragment.app.DialogFragment;

import com.jizhenkeji.mainnetinspection.MApplication;
import com.jizhenkeji.mainnetinspection.R;
import com.jizhenkeji.mainnetinspection.databinding.DialogMissionExportBinding;
import com.jizhenkeji.mainnetinspection.model.entity.MissionWithTowers;
import com.jizhenkeji.mainnetinspection.model.entity.TowerEntity;
import com.jizhenkeji.mainnetinspection.utils.GlobalUtils;
import com.jizhenkeji.mainnetinspection.utils.TaskExecutors;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MissionExportDialogFragment extends DialogFragment {

    private ObservableField<String> observableProgressMessage = new ObservableField<>(GlobalUtils.getString(R.string.export_mission));

    public final ObservableBoolean observableIsFinish = new ObservableBoolean(false);

    private DialogMissionExportBinding mBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DialogMissionExportBinding.inflate(inflater, container, false);

        mBinding.setProgressMsg(observableProgressMessage);
        mBinding.setIsFinish(observableIsFinish);

        return mBinding.getRoot();
    }

    public void exportTo(File rootPath, MissionWithTowers missionWithTowers){
        setCancelable(false);
        TaskExecutors.getInstance().runInDiskIoThread(() -> {
            try {
                int rowIndex = 0;
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                HSSFWorkbook missionWorkbook = new HSSFWorkbook();
                HSSFSheet sheet = missionWorkbook.createSheet();
                HSSFRow headerRow = sheet.createRow(rowIndex++);
                /* 添加表头 */
                headerRow.createCell(0).setCellValue(GlobalUtils.getString(R.string.time));
                headerRow.createCell(1).setCellValue(GlobalUtils.getString(R.string.tower_num));
                headerRow.createCell(2).setCellValue(GlobalUtils.getString(R.string.latitude));
                headerRow.createCell(3).setCellValue(GlobalUtils.getString(R.string.longitude));
                /* 添加杆塔数据内容 */
                List<TowerEntity> towerEntities = missionWithTowers.towerEntities;
                for(TowerEntity towerEntity : towerEntities){
                    HSSFRow contentRow = sheet.createRow(rowIndex++);
                    contentRow.createCell(0).setCellValue(df.format(towerEntity.createDate));  // 时间
                    contentRow.createCell(1).setCellValue(towerEntity.towerNum);               // 塔号
                    contentRow.createCell(2).setCellValue(towerEntity.location.latitude);      // 纬度
                    contentRow.createCell(3).setCellValue(towerEntity.location.longitude);     // 经度
                }
                missionWorkbook.write(new File(rootPath, missionWithTowers.missionEntity.name + ".xls"));
                observableProgressMessage.set(GlobalUtils.getString(R.string.export_mission_success));
            } catch (IOException e) {
                observableProgressMessage.set(GlobalUtils.getString(R.string.export_mission_failure));
            }finally {
                setCancelable(true);
                observableIsFinish.set(true);
            }
        });
    }

}
