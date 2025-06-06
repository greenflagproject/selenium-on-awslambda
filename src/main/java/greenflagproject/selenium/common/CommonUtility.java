package greenflagproject.selenium.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CommonUtility {
    private final static Logger logger = LoggerFactory.getLogger(CommonUtility.class);

    /**
     * 設定がない場合はdefaultValueを返す
     * @param key
     * @param defaultValue
     * @return
     */
    public static boolean getBooleanEnv(String key, boolean defaultValue){
        String s =System.getenv(key);
        if(null!=s){
            return switch (s.toLowerCase()){
                case "true","1" -> true;
                case "false","0" -> false;
                default -> defaultValue;
            };
        }
        return defaultValue;
    }

    /**
     * 設定がない場合はdefaultValueを返す
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getEnv(String key, String defaultValue){
        String s =System.getenv(key);
        if(null!=s && !s.isEmpty()){
            return s;
        }
        return defaultValue;
    }

    public static boolean checkExists(String path, boolean throwRuntimeException){
    return checkExists(new File(path), throwRuntimeException);
}
    public static boolean checkExists(File file, boolean throwRuntimeException){
        if(file.exists()){
            logger.info("Check exists: true path[{}] type[{}] canExecute[{}]", file.toString(), file.isDirectory()?"directory":"file",file.canExecute());
            return true;
        }
        String message = String.format("Check exists: false path[%s]",null==file?file:file.getPath());
        logger.info(message);
        if(false == throwRuntimeException){
            throw new RuntimeException(message);
        }
        return false;
    }
}
