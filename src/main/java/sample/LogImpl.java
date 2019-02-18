package sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogImpl {
    private Logger logger=null;

    public Logger getLogger(){
        if(logger==null){
            logger=LoggerFactory.getLogger(this.getClass());
        }
        return logger;
    }

}
