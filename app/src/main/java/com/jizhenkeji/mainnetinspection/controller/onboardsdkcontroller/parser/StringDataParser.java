package com.jizhenkeji.mainnetinspection.controller.onboardsdkcontroller.parser;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 字符串数据解析器，不断从OSDK接口读取数据，直到遇到结束字节，回调其它有效数据
 */
public class StringDataParser extends BytesDataParser {

    private Charset mDecodeCharsets = StandardCharsets.UTF_8;

    public void setDecodeCharsets(Charset decodeCharsets){
        mDecodeCharsets = decodeCharsets;
    }

    public void setDecodeCharsets(String charsetsName){
        mDecodeCharsets = Charset.forName(charsetsName);
    }

    @Override
    protected final void onReceive(byte[] data) {
        onReceive(new String(data, mDecodeCharsets));
    }

    protected void onReceive(String data){

    }

}
