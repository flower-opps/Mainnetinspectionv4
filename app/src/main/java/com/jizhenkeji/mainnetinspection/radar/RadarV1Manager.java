package com.jizhenkeji.mainnetinspection.radar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.jizhenkeji.mainnetinspection.utils.CRC8;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class RadarV1Manager implements RadarManager {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * 雷达蓝牙数据接收缓存区长度
     */
    private final int BLUETOOTH_RECEIVE_LENGTH = 23;

    /**
     * 雷达蓝牙数据长度
     */
    private final int BLUETOOTH_DATA_LENGTH = 19;

    /**
     * 蓝牙设备对象
     */
    private BluetoothDevice mBluetoothDevice;

    /**
     * 蓝牙数据解析处理线程
     */
    private Thread mRadarHandleThread;

    /**
     * 蓝牙套字节对象
     */
    private BluetoothSocket mBluetoothSocket;

    /**
     * 蓝牙连接输入流对象
     */
    private InputStream mBluetoothInputStream;

    /**
     * 雷达接收缓冲区
     */
    private ByteBuffer mReceiveBuffer;

    /**
     * 雷达数据暂存区
     */
    private ByteBuffer mDataBuffer;

    /**
     *雷达数据回调
     */
    public static RadarCall radarCall;

    protected RadarV1Manager(BluetoothDevice device){mBluetoothDevice = device;}

    @Override
    public void conenct() {
        mRadarHandleThread = new Thread(mRadarHandleRunnable);
        mRadarHandleThread.setDaemon(true);
        mRadarHandleThread.start();
    }

    @Override
    public boolean isConnect() {
        return mRadarHandleThread.isAlive();
    }

    @Override
    public void disconenct() {
        try{
            if(mRadarHandleThread != null && mRadarHandleThread.isAlive()){
                mBluetoothSocket.close();
                mRadarHandleThread.interrupt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 雷达数据最新获取时间戳
     */
    private volatile long mLastUpdateTime;

    @Override
    public long getLastDelay() {
        return System.currentTimeMillis() - mLastUpdateTime;
    }

    private volatile float mLineX;

    @Override
    public float getLineX() {
        return mLineX;
    }

    private volatile float mLineY;

    @Override
    public float getLineY() {
        return mLineY;
    }

    private volatile float mLineTreeX1;

    @Override
    public float getLineTreeX1() {
        return mLineTreeX1;
    }

    private volatile float mLineTreeY1;

    @Override
    public float getLineTreeY1() {
        return mLineTreeY1;
    }

    private volatile float mLineTreeX2;

    @Override
    public float getLineTreeX2() {
        return mLineTreeX2;
    }

    private volatile float mLineTreeY2;

    @Override
    public float getLineTreeY2() {
        return mLineTreeY2;
    }

    private volatile float mElectricQuantity;

    @Override
    public float getElectricQuantity() {
        return mElectricQuantity;
    }

    public void radarCall(RadarCall r) {
        radarCall=r;
    }


    private final Runnable mRadarHandleRunnable = () -> {
        try{
            /* 构建传输套字节 */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mBluetoothDevice.getAddress());
            mBluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            mBluetoothSocket.connect();
            mBluetoothInputStream = mBluetoothSocket.getInputStream();               // 获取输入流
            mReceiveBuffer = ByteBuffer.allocate(BLUETOOTH_RECEIVE_LENGTH);          // 初始化接收缓冲区
            mDataBuffer = ByteBuffer.allocate(BLUETOOTH_DATA_LENGTH);                // 初始化数据缓冲区
            /* 循环处理雷达数据 */
            while (!Thread.interrupted()){
                /* 解析数据头 */
                if(!parseHead()){
                    continue;
                }
                /* 解析数据长度 */
                if(!parseLen()){
                    continue;
                }
                /* 解析固定位 */
                if(!parseFix()){
                    continue;
                }
                /* 解析雷达数据 */
                if(!parseData()){
                    continue;
                }
                /* 校验雷达数据 */
                parseVerify();
//                radarCall.radarDataCallBack(mLineX,mLineY,mElectricQuantity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    /**
     * 解析蓝牙数据的起始头
     * @return 解析结果
     */
    private boolean parseHead() throws IOException {
        int data = mBluetoothInputStream.read();
        if(data == 0x11){
            mReceiveBuffer.clear();
            mReceiveBuffer.put((byte)data);
            return true;
        }
        return false;
    }

    /**
     * 解析蓝牙的数据长度位
     * @return
     */
    private boolean parseLen() throws IOException {
        int data = mBluetoothInputStream.read();
        mReceiveBuffer.put((byte)data);
        return data == 0x58;
    }

    /**
     * 解析蓝牙数据的固定位
     * @return
     * @throws IOException
     */
    private boolean parseFix() throws IOException {
        int data = mBluetoothInputStream.read();
        mReceiveBuffer.put((byte)data);
        return data == 0x02;
    }

    /**
     * 解析雷达数据
     * @return
     */
    private boolean parseData() throws IOException {
        mDataBuffer.clear();
        for(int i = 0; i < BLUETOOTH_DATA_LENGTH; i++){
            byte data = (byte)(mBluetoothInputStream.read() & 0xFF);
            mDataBuffer.put(data);
            mReceiveBuffer.put(data);
        }
        return true;
    }

    /**
     * 校验雷达数据
     * @return
     */
    private void parseVerify() throws IOException {
        int data = mBluetoothInputStream.read();
        mReceiveBuffer.put((byte) data);
        /* 使用校验位验证数据 */
        if(CRC8.calcCrc8(mReceiveBuffer.array(), 0, 22) != (byte)data){
            return;
        }
        /* 数据校验通过，开始解析蓝牙数据 */
        byte[] datas = mDataBuffer.array();
        float lineDistance = (datas[0] << 8 | datas[1] & 0xFF) / 1000f;
        float lineAngle = (datas[2] << 8 | datas[3] & 0xFF) / 10f;
        float lineTreeX1 = (datas[4] << 8 | datas[5] & 0xFF) / 1000f;
        float lineTreeY1 = (datas[6] << 8 | datas[7] & 0xFF) / 1000f;
        float lineTreeX2 = (datas[8] << 8 | datas[9] & 0xFF) / 1000f;
        float lineTreeY2 = (datas[10] << 8 | datas[11] & 0xFF) / 1000f;
        float electricQuantity = (datas[12] << 8 | datas[13] & 0xFF) / 100f;

        float lineX, lineY;
        if(lineDistance > 0.3 && lineDistance < 7){
            lineX = (float) (lineDistance * Math.cos(lineAngle / 180.0f * 3.14159f));
            lineY = (float) (lineDistance * Math.sin(lineAngle / 180.0f * 3.14159f));
        }else{
            lineX = 0;
            lineY = 0;
        }
        mLastUpdateTime = System.currentTimeMillis();
        mLineX = lineX;
        mLineY = lineY;
        mLineTreeX1 = lineTreeX1;
        mLineTreeY1 = lineTreeY1;
        mLineTreeX2 = lineTreeX2;
        mLineTreeY2 = lineTreeY2;
        mElectricQuantity = electricQuantity;
    }

}
