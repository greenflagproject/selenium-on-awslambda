package greenflagproject.selenium.common;

import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://note.com/ai_tarou/n/n1b6469adec17
 */
public class WuChromeOptions extends ChromeOptions {
    private final static Logger logger = LoggerFactory.getLogger(WuChromeOptions.class);
    final public static String ENV_KEY_WEBDRIVER_CHROME_DRIVER="WEBDRIVER_CHROME_DRIVER";
    final public static String SYSTEM_PROPERTY_KEY_WEBDRIVER_CHROME_DRIVER="webdriver.chrome.driver";
    final public static String DEFAULT_WEBDRIVER_CHROME_DRIVER="chrome";

    protected String downloadFolderPath;
    public String getDownloadFolderPath(){
        return downloadFolderPath;
    }

    public WuChromeOptions() {
        this(false);
    }

    public WuChromeOptions(boolean headlessMode){
        this(WebSiteControlDriver.createTmpDownloadFolder(), headlessMode);
    }
    public WuChromeOptions(String downloadFolderPath, boolean headlessMode){
        setDriver();
        setBinary();

        setHeadlesMode(headlessMode);
        setStandardOptions();
        setSslOptions();
        setIgnoreRobotOptions();
        setOsOptions(headlessMode);
        setExperimentalOptions(downloadFolderPath);
    }

    public ChromeOptions setBinary(){
        String s =System.getenv("WEBDRIVER_CHROME_BINARY");
        if(s!=null && !s.isEmpty()) {
            super.setBinary(s);
            logger.info("Chrome binary path [{}]", s);
            CommonUtility.checkExists(s,true);
        }
        else{
            logger.info("Chrome binary path was not set[{}].", s);
        }
        return this;
    }

    protected void setDriver(){
        String chromeDriverPath =System.getenv(ENV_KEY_WEBDRIVER_CHROME_DRIVER);
        if(null!=chromeDriverPath && !chromeDriverPath.isEmpty()){
            logger.info("Env {} is found[{}].", ENV_KEY_WEBDRIVER_CHROME_DRIVER, chromeDriverPath);
        }
        else if(null==System.getProperty(SYSTEM_PROPERTY_KEY_WEBDRIVER_CHROME_DRIVER)){     //開発時のための処理。本番環境では、環境変数WEBDRIVER_CHROME_DRIVERを設定する。
            logger.info("{}} is null. Set [{}].", SYSTEM_PROPERTY_KEY_WEBDRIVER_CHROME_DRIVER, DEFAULT_WEBDRIVER_CHROME_DRIVER);
            chromeDriverPath=DEFAULT_WEBDRIVER_CHROME_DRIVER;
        }
        else {
            throw new RuntimeException("Chrome driver path couldn't get.");
        }

        if(null!=chromeDriverPath && !chromeDriverPath.isEmpty()) {
            System.setProperty(SYSTEM_PROPERTY_KEY_WEBDRIVER_CHROME_DRIVER, chromeDriverPath);    //AWS Lambdaで動作させた時に、selenium-managerを動かしたくない時は指定必須
            CommonUtility.checkExists(chromeDriverPath, true);
        }
        else{
            logger.info("Chrome driver path was not set[{}].", chromeDriverPath);
        }
    }

    protected void setStandardOptions(){
        addArguments("--lang=ja-JP");   //Webサイトに日本語判定してもらうための設定
        addArguments("--disable-popup-blocking");
    }

    protected void setSslOptions(){
        addArguments("--allow-running-insecure-content");
        addArguments("--ignore-certificate-errors");    //SSL認証(この接続ではプライバシーが保護されません)を無効
        addArguments("--disable-features=InsecureDownloadWarnings");
    }

    protected void setIgnoreRobotOptions(){
        addArguments("--disable-extensions");
        addArguments("--disable-blink-features=AutomationControlled");
        setExperimentalOption("excludeSwitches", List.of("enable-automation"));
    }

    protected void setExperimentalOptions(String downloadFolderPath){
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_settings.popups", 0);
        if(downloadFolderPath!=null && !downloadFolderPath.isEmpty()) {
            this.downloadFolderPath=downloadFolderPath;
            prefs.put("download.default_directory", downloadFolderPath);
        }
        prefs.put("safebrowsing.enabled", true);                                    // 安全ブラウジングを無効化
        prefs.put("profile.default_content_setting_values.automatic_downloads", 1); // 自動ダウンロードを許可
        prefs.put("download.prompt_for_download", false);
//        prefs.put("profile.managed_default_content_settings.images", 2);          // 画像読み込みしない←動作しなかったのでコメントアウト
//        prefs.put("profile.default_content_setting_values.images", 2);            // 画像読み込みしない←動作しなかったのでコメントアウト
        setExperimentalOption("prefs", prefs);
    }

    protected void setHeadlesMode(boolean headlessMode){
        logger.info("setHeadlesMode:{}", headlessMode);
        if(headlessMode){
            //AWS Lambdaコンテナ実行で必須のパラメータ
            addArguments("--headless=new"); // ヘッドレスモードで実行
            addArguments("--no-sandbox");
            addArguments("--disable-dev-shm-usage");    //This is frequently caused by incorrect permissions on /dev/shm.  Try 'sudo chmod 1777 /dev/shm' to fix.

            addArguments("--single-process");               //AWS動作に必須

//            addArguments("--remote-debugging-pipe");      //なくてもAWS動作可能   //In this mode DevToolsActivePort is not used.
//            addArguments("--disable-application-cache");  //なくてもAWS動作可能
//            addArguments("--window-size=1280,1024");      //なくてもAWS動作可能
        }
    }
    protected void setOsOptions(boolean headlessMode){

        //OS 固有設定
        String osName =System.getProperty("os.name");
        if (osName.equals("Linux")) {
            logger.info("setOsOptions(Linux)");
            addArguments("--user-data-dir=/tmp");       //Macで開発時に実行する場合につけると動かないが、AWS Lambda稼働させるために付けたオプション
        }
        else if(osName.equals("Mac OS X")){
            logger.info("setOsOptions(Mac OS X)");
            addArguments("--password-store=basic");     // 効果を確認できていないが、悪さもしていないので、残しておく
            addArguments("--use-mock-keychain");        // 効果を確認できていないが、悪さもしていないので、残しておく
        }

    }
}
