package sample.task;


import sample.LogImpl;
import sample.USBDataHandler;
import utils.DataUtils;

import java.util.concurrent.BlockingQueue;

/**
 * Created by Daemon1993 on 2019/1/26 9:59 AM.
 */
public class Consumer extends LogImpl implements Runnable {
    private final BlockingQueue blockingQueue;


    public Consumer(BlockingQueue blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    @Override
    public void run() {
        for (; ; ) {
            try {
                TaskUtils.Task task = (TaskUtils.Task) blockingQueue.take();


                byte[] bytes = task.byte_arr_lists.get(0);
                log("Consumer " + DataUtils.ByteArrToHex(bytes));
                USBDataHandler.getIsnatnce().sendDatas(bytes);


            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
