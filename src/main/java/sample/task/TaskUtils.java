package sample.task;



import sample.LogImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Daemon1993 on 2019/1/26 10:00 AM.
 */
public class TaskUtils  extends LogImpl {

    public List<Task> need_write_task = new ArrayList<>();


    public static class Task {
        public List<byte[]> byte_arr_lists = new ArrayList<>();
        public int type;

        public Task(int type, List<byte[]> byte_arr_lists) {
            this.byte_arr_lists = byte_arr_lists;
            this.type = type;
        }

        public Task(int type, byte[] datas) {
            this.byte_arr_lists.add(datas);
            this.type = type;
        }
    }

    final BlockingQueue<Task> blockingQueue = new ArrayBlockingQueue<>(1);
    Thread threadConsumer;

    private static TaskUtils Instance;

    private void init() {
        if (threadConsumer == null || threadConsumer.isInterrupted()) {
            threadConsumer = new Thread(new Consumer(blockingQueue));
            threadConsumer.start();
        }
    }

    private TaskUtils() {

    }

    public Task getTaskByBytes(byte [] bytes){
        return   new Task(bytes[0], bytes);
    }
    public void addAllTask(byte [] ...args) {

        List<Task> tasks=new ArrayList<>();
        for(int i=0 ; i<args.length;i++){
            byte[] arg = args[i];
            Task task = new Task(arg[0], arg);
            tasks.add(task);
        }

        need_write_task.addAll(tasks);
        if(need_write_task.size()==tasks.size()) {
            notifyNetTask();
        }
    }

    public void addAllTask(List<Task> tasks) {


        need_write_task.addAll(tasks);

        if(need_write_task.size()==tasks.size()) {
            notifyNetTask();
        }
    }

    public static TaskUtils getInstance() {
        if (Instance == null) {
            Instance = new TaskUtils();
            Instance.init();
        }
        return Instance;
    }

    public boolean writeListisOVER() {
        return need_write_task.size()==0;
    }

    /**
     * 通知下一条
     */
    public void notifyNetTask() {
        getLogger().debug("notifyNetTask " + need_write_task.size());
        if (need_write_task.size() > 0) {
            try {
                blockingQueue.put(need_write_task.get(0));
                need_write_task.remove(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
