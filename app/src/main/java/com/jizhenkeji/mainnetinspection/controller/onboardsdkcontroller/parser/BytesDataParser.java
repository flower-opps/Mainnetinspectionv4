package com.jizhenkeji.mainnetinspection.controller.onboardsdkcontroller.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 字节数据解析器，不断从OSDK接口读取数据，直到遇到结束字节，回调其它有效数据
 */
public class BytesDataParser implements OnboardSDKDataParser {

    private final ByteArrayOutputStream mByteArrayOutputStream = new ByteArrayOutputStream();

    private byte mEndFlag = 0;

    public void setEndFlag(byte flag){
        mEndFlag = flag;
    }

    @Override
    public final void onParse(byte[] data) {
        if(data == null || data.length == 0){
            return;
        }
        /* 计算有效数据（非0x00的数据）的长度 */
        int length = 0;
        while(length < data.length && data[length] != mEndFlag){
            length++;
        }
        byte[] validDatas = Arrays.copyOfRange(data, 0, length);
        try{
            mByteArrayOutputStream.write(validDatas);
            if(validDatas.length != data.length){   // 如果本次数据含有无效数据（0x00数据），则直接将缓存中的所有有效数据回调
                onReceive(mByteArrayOutputStream.toByteArray());
                mByteArrayOutputStream.reset();
            }
        } catch (IOException e) {
            mByteArrayOutputStream.reset();
        }
    }

    protected void onReceive(byte[] data){}

}
