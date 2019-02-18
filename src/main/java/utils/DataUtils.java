package utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * 串口数据转换工具类
 * Created by Administrator on 2016/6/2.
 */
public class DataUtils {
    //-------------------------------------------------------
    // 判断奇数或偶数，位运算，最后一位是1则为奇数，为0是偶数
    public static int isOdd(int num) {
        return num & 1;
    }

    //-------------------------------------------------------
    //Hex字符串转int
    public static int HexToInt(String inHex) {
        return Integer.parseInt(inHex, 16);
    }

    public static String IntToHex(int intHex){
        return Integer.toHexString(intHex);
    }

    //-------------------------------------------------------
    //Hex字符串转byte
    public static byte HexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

    //-------------------------------------------------------
    //1字节转2个Hex字符
    public static String Byte2Hex(Byte inByte) {
        return String.format("%02x", new Object[]{inByte}).toUpperCase();
    }

    //-------------------------------------------------------
    //字节数组转转hex字符串
    public static String Byte4ArrToHex(byte[] inBytArr) {
        StringBuilder strBuilder = new StringBuilder();
        int i=0;
        for(;i<inBytArr.length;i+=4){

            if(i+4>inBytArr.length){
                break;
            }

            byte[] temp=new byte[4];
            System.arraycopy(inBytArr,i,temp,0,4);

            strBuilder.append(ByteArrToHex(temp));
            strBuilder.append(" \n");
        }
        if(i<inBytArr.length){
            byte[] temp=new byte[inBytArr.length-i];
            System.arraycopy(inBytArr,i,temp,0,inBytArr.length-i);
            strBuilder.append(ByteArrToHex(temp));
            strBuilder.append(" \n");
        }
        return strBuilder.toString();
    }

    //字节数组转转hex字符串
    public static String ByteNArrToHex(byte[] inBytArr,int n) {
        StringBuilder strBuilder = new StringBuilder();
        int i = 0;
        for (; i < inBytArr.length; i += n) {

            if (i + n > inBytArr.length) {
                break;
            }

            byte[] temp = new byte[n];
            System.arraycopy(inBytArr, i, temp, 0, n);

            strBuilder.append(ByteArrToHex(temp));
            strBuilder.append(" \n");
        }
        if (i < inBytArr.length) {
            byte[] temp = new byte[inBytArr.length - i];
            System.arraycopy(inBytArr, i, temp, 0, inBytArr.length - i);
            strBuilder.append(ByteArrToHex(temp));
            strBuilder.append(" \n");
        }
        return strBuilder.toString();
    }

    //字节数组转转hex字符串
    public static String ByteArrToHex(byte[] inBytArr) {
        StringBuilder strBuilder = new StringBuilder();
        for (byte valueOf : inBytArr) {
            strBuilder.append(Byte2Hex(Byte.valueOf(valueOf)));
            strBuilder.append(" ");
        }
        return strBuilder.toString();
    }

    //-------------------------------------------------------
    //字节数组转转hex字符串，可选长度
    public static String ByteArrToHex(byte[] inBytArr, int offset, int byteCount) {
        StringBuilder strBuilder = new StringBuilder();
        int j = byteCount;
        for (int i = offset; i < j; i++) {
            strBuilder.append(Byte2Hex(Byte.valueOf(inBytArr[i])));
        }
        return strBuilder.toString();
    }

    //-------------------------------------------------------
    //转hex字符串转字节数组
    public static byte[] HexToByteArr(String inHex) {
        byte[] result;
        int hexlen = inHex.length();
        if (isOdd(hexlen) == 1) {
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = HexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    /**
     * 按照指定长度切割字符串
     *
     * @param inputString 需要切割的源字符串
     * @param length      指定的长度
     * @return
     */
    public static List<String> getDivLines(String inputString, int length) {
        List<String> divList = new ArrayList<>();
        int remainder = (inputString.length()) % length;
        // 一共要分割成几段
        int number = (int) Math.floor((inputString.length()) / length);
        for (int index = 0; index < number; index++) {
            String childStr = inputString.substring(index * length, (index + 1) * length);
            divList.add(childStr);
        }
        if (remainder > 0) {
            String cStr = inputString.substring(number * length, inputString.length());
            divList.add(cStr);
        }
        return divList;
    }

    /**
     * 计算长度，两个字节长度
     *
     * @param val value
     * @return 结果
     */
    public static String twoByte(String val) {
        if (val.length() > 4) {
            val = val.substring(0, 4);
        } else {
            int l = 4 - val.length();
            for (int i = 0; i < l; i++) {
                val = "0" + val;
            }
        }
        return val;
    }

    /**
     * 校验和
     *
     * @param cmd 指令
     * @return 结果
     */
    public static String sum(String cmd) {
        List<String> cmdList = DataUtils.getDivLines(cmd, 2);
        int sumInt = 0;
        for (String c : cmdList) {
            sumInt += DataUtils.HexToInt(c);
        }
        String sum = DataUtils.IntToHex(sumInt);
        sum = DataUtils.twoByte(sum);
        cmd += sum;
        return cmd.toUpperCase();
    }

    //System.arraycopy()方法
    public static byte[] byteMerger(byte[] bt1, byte[] bt2){
        byte[] bt3 = new byte[bt1.length+bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

    public static byte[] byteResult(byte [] head,byte [] data){
        byte[] head_crc16 = CrcCalculator.getCrc16(head);

        byte[] head_result = byteMerger(head, head_crc16);

        if(data==null){
            return head_result;
        }


        byte[] data_crc16 = CrcCalculator.getCrc16(data);

        byte[] data_result = byteMerger(data, data_crc16);

        return byteMerger(head_result,data_result);

    }

    /**
     * byte装成int
     * @param value
     * @return
     */
    public static int bytesToint(byte[] value) {
        int ret = 0;
        for (int i = 0; i < value.length; i++) {
            ret += (value[i] & 0xFF) << (i * 8);
        }
        return ret;
    }

    /**
     * 以小端模式将int转成byte[]
     *
     * @param value
     * @return
     */
    public static byte[] intToBytesLittle4(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 以小端模式将int转成byte[]
     *
     * @param value
     * @return
     */
    public static byte[] intToBytesLittle2(int value) {
        byte[] src = new byte[2];
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 以小端模式将int转成byte[]
     *
     * @param value
     * @return
     */
    public static byte intToBytesLittle1(int value) {

        return (byte) (value & 0xFF);

    }

    /**
     * 以小端模式将byte[]转成int
     */
    public static int bytesToIntLittle(byte[] src) {
        int value;
        byte [] result=new  byte[4];
        if(src.length<4){
            System.arraycopy(src,0,result,0,src.length);
        }else {
            result=src;
        }

//        KLog.e(DataUtils.ByteArrToHex(result));

        value = (int) ((result[0] & 0xFF)
                | ((result[1] & 0xFF) << 8)
                | ((result[2] & 0xFF) << 16)
                | ((result[3] & 0xFF) << 24));
        return value;
    }

    public static int byteToInt(byte b) {
        //Java 总是把 byte 当做有符处理；我们可以通过将其和 0xFF 进行二进制与得到它的无符值
        return b & 0xFF;
    }

    public static byte[] getStarSizeIndexByteArr(byte[] real_datas, int start, int length) {
        byte [] result=new byte[length];
        System.arraycopy(real_datas,start,result,0,length);
        return result;
    }


//    /**
//     * 字节转换为浮点
//     *
//     * @param b 字节（至少4个字节）
//     * @return
//     */
//    public static float byte2float(byte[] b) {
//        int l;
//        int index=0;
//
//        l = b[index + 0];
//        l &= 0xff;
//        l |= ((long) b[index + 1] << 8);
//        l &= 0xffff;
//        l |= ((long) b[index + 2] << 16);
//        l &= 0xffffff;
//        l |= ((long) b[index + 3] << 24);
//        return Float.intBitsToFloat(l);
//    }
//
//    /**
//     * 浮点转换为字节
//     *
//     * @param f
//     * @return
//     */
//    public static byte[] float2byte(float f) {
//
//        // 把float转换为byte[]
//        int fbit = Float.floatToIntBits(f);
//
//        byte[] b = new byte[4];
//        for (int i = 0; i < 4; i++) {
//            b[i] = (byte) (fbit >> (i * 8));
//        }
//
////        // 翻转数组
////        int len = b.length;
////        // 建立一个与源数组元素类型相同的数组
////        byte[] dest = new byte[len];
////        // 为了防止修改源数组，将源数组拷贝一份副本
////        System.arraycopy(b, 0, dest, 0, len);
////        byte temp;
////        // 将顺位第i个与倒数第i个交换
////        for (int i = 0; i < len / 2; ++i) {
////            temp = dest[i];
////            dest[i] = dest[len - i - 1];
////            dest[len - i - 1] = temp;
////        }
//        return b;
//    }


    /**
     * 字节转换为浮点
     *
     * @param b 字节（至少4个字节）
     * @return
     */
    public static float byte2float(byte[] b) {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * 浮点转换为字节
     *
     * @param f
     * @return
     */
    public static byte[] float2byte(float f) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(f).array();
    }

}