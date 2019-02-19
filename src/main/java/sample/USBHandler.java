package sample;

import utils.DataUtils;

import javax.usb.*;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;
import java.util.List;

public class USBHandler extends LogImpl {
    static short vendorId = 0x0483;
    static short productId = 0x5740;
    static USBHandler usbHandler = null;

    private UsbInterface iface;
    private UsbPipe pipe_send;
    private UsbPipe pipe_receive;

    public static USBHandler getIsnatnce() {
        if (usbHandler == null) {
            usbHandler = new USBHandler();
        }
        return usbHandler;
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
                    getLogger().error("receive " + DataUtils.ByteArrToHex(event.getData()));
                }
            });

            pipe_send.open();
            pipe_receive.open();


        } catch (UsbException e) {
            e.printStackTrace();
        }
    }

    public void sendConnect() {
        byte[] data = new byte[]{0x05, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] bytes = DataUtils.byteResult(data, null);

        sendDatas(bytes);
    }

    private void sendDatas(byte[] bytes) {
        if(pipe_send==null){
            return;
        }
        getLogger().error(DataUtils.ByteArrToHex(bytes));
        try {
            pipe_send.asyncSubmit(bytes);

            pipe_receive.syncSubmit(new byte[100]);
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
