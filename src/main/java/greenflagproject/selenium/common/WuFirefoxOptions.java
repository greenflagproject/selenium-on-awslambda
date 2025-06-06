package greenflagproject.selenium.common;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WuFirefoxOptions extends FirefoxOptions {
    final public static String ENV_KEY_WEBDRIVER_GECKO_DRIVER="WEBDRIVER_GECKO_DRIVER";
    final public static String SYSTEM_PROPERTY_KEY_WEBDRIVER_FIREFOX_DRIVER="webdriver.gecko.driver";

    private final static Logger logger = LoggerFactory.getLogger(WuFirefoxOptions.class);
    public WuFirefoxOptions() {
        this(true);
    }

    public WuFirefoxOptions(boolean headlessMode){
        logger.info("WuFirefoxOptions headless:{}", headlessMode);

        setPageLoadStrategy(PageLoadStrategy.EAGER);    //DOM アクセスの準備は整っていますが、画像などの他のリソースはまだロード中の可能性があります
        setDriver();
        setBinary();
        setHeadlesMode(headlessMode);

        // エラーの許容
        setCapability("acceptInsecureCerts",true);  //CapabilityType.ACCEPT_SSL_CERTS
        addArguments("--ignore-certificate-errors");
        addArguments("--allow-running-insecure-content");
        addArguments("--disable-web-security");
        // headless では不要そうな機能を除外
        addArguments("--disable-desktop-notifications");
        addArguments("--disable-extensions");
//# UA設定 （なくてもいい）
//        addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:62.0) Gecko/20100101 Firefox/62.0")
        // 画像を読み込まないで軽くする
        addArguments("--blink-settings=imagesEnabled=false");
    }

    public FirefoxOptions setBinary(){
        String s =System.getenv("WEBDRIVER_FIREFOX_BINARY");
        if(s!=null && !s.isEmpty()) {
            super.setBinary(s);
            logger.info("Firefox binary path [{}]", s);
        }
        else{
            logger.info("Firefox binary path was not set[{}].", s);
        }
        return this;
    }

    protected void setDriver(){
        String driverPath =System.getenv(ENV_KEY_WEBDRIVER_GECKO_DRIVER);
        if(null!=driverPath && !driverPath.isEmpty()){
            logger.info("Env {} is found[{}].", ENV_KEY_WEBDRIVER_GECKO_DRIVER, driverPath);
        }
        else {
            throw new RuntimeException("Chrome driver path couldn't get.");
        }

        if(null!=driverPath && !driverPath.isEmpty()) {
            System.setProperty(SYSTEM_PROPERTY_KEY_WEBDRIVER_FIREFOX_DRIVER, driverPath);    //AWS Lambdaで動作させた時に、selenium-managerを動かしたくない時は指定必須
        }
        else{
            logger.info("Chrome driver path was not set[{}].", driverPath);
        }
    }

    /**
     * https://stackoverflow.com/questions/75677505/selenium-with-firefox-gecko-driver-on-aws-lambda-container-failed-to-read-mar
     * @param headlessMode
     */
    protected void setHeadlesMode(boolean headlessMode){
        if(headlessMode){
            //AWS Lambdaコンテナ実行で必須のパラメータ
            addArguments("--headless"); // ヘッドレスモードで実行
            addArguments("--no-sandbox");
            addArguments("--disable-dev-shm-usage");    //This is frequently caused by incorrect permissions on /dev/shm.  Try 'sudo chmod 1777 /dev/shm' to fix.
        }
    }
}
