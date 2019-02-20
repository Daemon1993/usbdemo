package sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.LogUtil;

public class LogImpl {
    private Logger logger = null;

    public Logger getLogger() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(this.getClass());
        }
        return logger;
    }

    public void log(Object object) {
        LogUtil.logE(object.toString());
    }

    public void logD(Object object) {
        LogUtil.logD(object.toString());
    }

}
