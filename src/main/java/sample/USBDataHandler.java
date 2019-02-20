package sample;

import sample.task.TaskUtils;
import utils.CrcCalculator;
import utils.DataUtils;

import javax.usb.*;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class USBDataHandler extends LogImpl {
    static short vendorId = 0x0483;
    static short productId = 0x5740;
    static USBDataHandler usbHandler = null;

    public static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
    private static boolean connect;

    private UsbInterface iface;
    private UsbPipe pipe_send;
    private UsbPipe pipe_receive;

    private byte last_send_base_type;
    private byte last_send_cmd;
    private int tag_size;
    private ComeData comeData;

    public static USBDataHandler getIsnatnce() {
        if (usbHandler == null) {
            usbHandler = new USBDataHandler();
        }
        return usbHandler;
    }


    public class ComeData {
        //需要接收的大小
        public int tag_size;

        private List<byte[]> byte_arr_lists = new ArrayList<>();

        public int byte_arr_sum_size = 0;

        public ComeData(int size) {
            tag_size = size;
            byte_arr_sum_size = 0;
        }

        public boolean addByteArr(byte[] datas) {

            if (isNeedByte(datas)) {

                byte_arr_lists.add(datas);
                byte_arr_sum_size += datas.length;
//                KLog.d("byte_arr_sum_size "+byte_arr_sum_size +"  -> "+DataUtils.ByteArrToHex(datas));
            }

            if (byte_arr_sum_size >= tag_size) {
//                KLog.e("type " + last_send_base_type + "   receivedata " + byte_arr_lists);
                byte[] real_data = byte_arr_lists.get(0);
                for (int i = 1; i < byte_arr_lists.size(); i++) {
                    real_data = DataUtils.byteMerger(real_data, byte_arr_lists.get(i));
                }

                //验证CRC
                byte[] temp_crc_data = new byte[real_data.length - 2];
                System.arraycopy(real_data, 0, temp_crc_data, 0, temp_crc_data.length);
                byte[] crc16 = CrcCalculator.getCrc16(temp_crc_data);
                byte[] crc = new byte[]{real_data[real_data.length - 2], real_data[real_data.length - 1]};

                getLogger().debug("crc16 " + DataUtils.ByteArrToHex(crc16) + "    crc " + DataUtils.ByteArrToHex(crc));
                if (!Arrays.equals(crc, crc16)) {
                    getLogger().error(DataUtils.ByteArrToHex(new byte[]{last_send_base_type, last_send_cmd}) + " 数据无效 下一条");

                    comeData = null;
                    TaskUtils.getInstance().notifyNetTask();
                    return true;
                }


                //去掉crc
                int srcPos = 0;
                int need_lenght = real_data.length - 2;

                if (last_send_base_type == 0x01 && last_send_cmd == real_data[0]) {
                    srcPos = 1;
                    if (last_send_cmd == 0x7b) {
                        //跳过前1k
                        srcPos += 1024;
                    }
                    need_lenght -= srcPos;
                }
                byte[] real_data1 = new byte[need_lenght];

                System.arraycopy(real_data, srcPos, real_data1, 0, need_lenght);

                if (last_send_cmd == 0x7b) {
                    getLogger().debug("size " + real_data1.length + "  " + DataUtils.Byte2Hex(last_send_base_type) + "  " + DataUtils.Byte2Hex(last_send_cmd) + " 真实数据 \n"
                            + DataUtils.ByteNArrToHex(real_data1, 64));
                } else {
                    getLogger().debug("size " + real_data1.length + "  " + DataUtils.Byte2Hex(last_send_base_type) + "  " + DataUtils.Byte2Hex(last_send_cmd) + " 真实数据 \n"
                            + DataUtils.Byte4ArrToHex(real_data1));
                }
                comeData = null;
                TaskUtils.getInstance().notifyNetTask();
                return true;
            }

            return false;
        }

        //加入的数据是否还需要
        private boolean isNeedByte(byte[] datas) {
            return (datas.length + byte_arr_sum_size) <= tag_size;
        }


        public int needSize() {
            return tag_size - byte_arr_sum_size;
        }
    }


    public void init() {

        destory();
        UsbServices usbServices = null;

        try {
            usbServices = UsbHostManager.getUsbServices();
            UsbHub usbHub = usbServices.getRootUsbHub();
            UsbDevice device = findDevice(usbHub, vendorId, productId);

            if (device == null) {
                getLogger().error("device is null");
                return;
            }

            UsbConfiguration configuration = device.getActiveUsbConfiguration();
//            logger.debug(configuration + "");

            iface = configuration.getUsbInterface((byte) 1);//接口
            iface.claim(new UsbInterfacePolicy() {
                public boolean forceClaim(UsbInterface arg0) {
                    // TODO Auto-generated method stub
                    return true;
                }
            });

            UsbEndpoint endpoint_receive = iface.getUsbEndpoint((byte) 0x81);//接受数据地址
            UsbEndpoint endpoint_send = iface.getUsbEndpoint((byte) 0x01);//发送数据地址

            pipe_send = endpoint_send.getUsbPipe();
            pipe_receive = endpoint_receive.getUsbPipe();


            pipe_receive.addUsbPipeListener(new UsbPipeListener() {

                @Override
                public void errorEventOccurred(UsbPipeErrorEvent event) {
                    getLogger().error("receive " + event.toString());
                }

                @Override
                public void dataEventOccurred(UsbPipeDataEvent event) {

                    byte[] datas = event.getData();

                    if (datas == null || datas.length == 0) {
                        return;
                    }

                    getLogger().debug(comeData + " 接收 " + datas.length + " " + DataUtils.ByteArrToHex(datas));

                    if (datas[0] == 0x12) {
                        comeData = null;
                        TaskUtils.getInstance().notifyNetTask();
                        return;
                    }


                    if (comeData == null && datas[0] == 0x10) {

                        if (last_send_base_type == 0x05 || last_send_base_type == 0x06) {
                            if (last_send_base_type == 0x05) {
                                connect = true;
                                getBase_AllData();

                            } else if (last_send_base_type == 0x06) {
                                connect = false;

                            }
                            return;
                        }

                        //根据长度来 接收后面收到的
                        byte[] starSizeIndexByteArr = DataUtils.getStarSizeIndexByteArr(datas, 2, 4);
                        int tag_size = DataUtils.bytesToIntLittle(starSizeIndexByteArr);
                        if (tag_size > 0) {
                            comeData = new ComeData(tag_size);
                            requestReceiveDatas(false, comeData);

                        } else {
                            TaskUtils.getInstance().notifyNetTask();
                        }
                        return;
                    }

                    if (comeData == null) {
                        return;
                    }

                    getLogger().debug(DataUtils.Byte2Hex(last_send_base_type) + "  " + DataUtils.Byte2Hex(last_send_cmd));
                    if (last_send_base_type == 0x01) {
                        //get指令 根收到的第一个来判断
                        if (last_send_cmd == datas[0]) {
                            boolean over = comeData.addByteArr(datas);

                            //还没有完 继续读取
                            requestReceiveDatas(over, comeData);

                            return;
                        }
                    }

                    //开始接收 10 后面的数据
                    boolean over = comeData.addByteArr(datas);

                    //还没有完 继续读取
                    requestReceiveDatas(over, comeData);


                }
            });

            pipe_send.open();
            pipe_receive.open();


        } catch (
                UsbException e) {
            e.printStackTrace();
        }

    }

    private void requestReceiveDatas(boolean over, ComeData comeData) {
        if(over){
            getLogger().debug("data receive over ");
            return;
        }
        //请求数据
        getLogger().debug("继续请求 size " + comeData.needSize());
        try {
            pipe_receive.syncSubmit(new byte[comeData.needSize()]);
        } catch (UsbException e) {
            e.printStackTrace();
        }
    }

    private boolean checkUsbNull() {
        if (pipe_send == null) {
            getLogger().debug("usbservice is nulll");
            return true;
        }
        return false;
    }

    private void baseAction1(byte[] head, byte[] data) {

        if (checkUsbNull()) return;

        if (head[0] != 0x05 && !USBDataHandler.connect) {
            getLogger().debug("连接设备");
            return;
        }


        byte[] bytes = DataUtils.byteResult(head, data);

        TaskUtils.Task task = new TaskUtils.Task(head[0], bytes);

        List<TaskUtils.Task> tasks = new ArrayList<>();
        tasks.add(task);
        TaskUtils.getInstance().addAllTask(tasks);

    }

    public void sendConnect() {
        byte[] head = new byte[]{0x05, 0x00, 0x00, 0x00, 0x00, 0x00};
        baseAction1(head, null);
    }

    private byte[] getDeviceModel(boolean isWrite) {
        byte[] data = new byte[]{0x08, 0x00, 0x00, 0x00, 0x00, 0x00};
        if (isWrite) {
            baseAction1(data, null);
        }
        byte[] bytes = DataUtils.byteResult(data, null);
        return bytes;
    }

    public byte[] getCMDDatas(byte type, boolean isWrite) {

        byte[] head = new byte[]{0x01, 0x00, 0x03, 0x00, 0x00, 0x00};
        byte[] data = new byte[]{type};

        if (isWrite) {
            baseAction1(head, data);
        }
        byte[] bytes = DataUtils.byteResult(head, data);
        return bytes;
    }

    public void getBase_AllData() {


        if (!USBDataHandler.connect) {
            getLogger().debug("string.no_coonect_msg");
            return;
        }

        //都区设备信息
        byte[] deviceModel = getDeviceModel(false);

        TaskUtils.getInstance().addAllTask(deviceModel);
    }


    public void sendDatas(byte[] bytes) {
        if (pipe_send == null) {
            return;
        }
        try {

            last_send_base_type = bytes[0];
            if (last_send_base_type == 0x01 && bytes.length > 8) {
                last_send_cmd = bytes[8];
            }

            pipe_send.asyncSubmit(bytes);
            pipe_receive.syncSubmit(new byte[8]);


            tag_size = 0;

        } catch (UsbException e) {
            e.printStackTrace();
        }
    }


    public void destory() {

        try {
            if (iface != null) {
                iface.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (pipe_send != null) {
                pipe_send.close();
            }
            if (pipe_receive != null) {
                pipe_receive.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static UsbDevice findDevice(UsbHub hub, short vendorId, short productId) {
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
            if (device.isUsbHub()) {
                device = findDevice((UsbHub) device, vendorId, productId);
                if (device != null) return device;
            }
        }
        return null;
    }

}
