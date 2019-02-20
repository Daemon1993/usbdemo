package utils;

public class LogUtil {
    private static boolean D = true;
    volatile static int n=0;

    public synchronized static void logE(String msg) {
        if(D){
            System.err.println("["+(++n)+"] "+"--"+Thread.currentThread().getStackTrace()[3] +" "+msg);
        }
    }

    public synchronized static void logD(String msg) {
        if(D){
            System.out.println("["+(++n)+"] "+"--"+Thread.currentThread().getStackTrace()[3] +" "+msg);
        }
    }
}
