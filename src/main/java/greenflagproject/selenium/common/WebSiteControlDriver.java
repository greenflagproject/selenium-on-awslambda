package greenflagproject.selenium.common;

import org.jspecify.annotations.Nullable;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriverLogLevel;
import org.openqa.selenium.federatedcredentialmanagement.FederatedCredentialManagementDialog;
import org.openqa.selenium.federatedcredentialmanagement.HasFederatedCredentialManagement;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.print.PrintOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.virtualauthenticator.HasVirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WebSiteControlDriver implements WebDriver, JavascriptExecutor, HasCapabilities, HasDownloads, HasFederatedCredentialManagement, HasVirtualAuthenticator, Interactive, PrintsPage, TakesScreenshot {
    private final static Logger logger = LoggerFactory.getLogger(WebSiteControlDriver.class);
    final static String SELENIUM_LOGLEVEL="SELENIUM_LOGLEVEL";

    /**
     * 実行ブラウザの指定
     * defaultはchrome
     */
    final static String ENV_EXECUTE_BROWSER="EXECUTE_BROWSER";

    private RemoteWebDriver driver;
    protected String downloadFolderPath;
    public String getDownloadFolderPath(){return downloadFolderPath;}

    public static WebSiteControlDriver createChromeDriver(){
        return WebSiteControlDriver.createChromeDriver(true);
    }

    public static WebSiteControlDriver createDriver(){
        return createDriver(true);
    }
    public static WebSiteControlDriver createDriver(boolean headlesMode){
        String executeBrowser = getEnv(ENV_EXECUTE_BROWSER, "");

        return switch (executeBrowser.toLowerCase()){
            case "chrome","chromium","" ->WebSiteControlDriver.createChromeDriver(headlesMode);     //Chromeをデフォルトにしておく。環境変数未定義もこれ。
            case "firefox" ->WebSiteControlDriver.createFirefoxDriver(headlesMode);
            default ->{
                logger.info("Env {}[{}]のDriver処理は定義されていません。",ENV_EXECUTE_BROWSER, executeBrowser);
                yield WebSiteControlDriver.createChromeDriver(headlesMode);
            }
        };
    }

    public static WebSiteControlDriver createChromeDriver(boolean headlesMode){
        logger.info("WebSiteControlDriver.createChromeDriver headlesmode:{}", headlesMode);
        ChromeOptions options = new WuChromeOptions(headlesMode);
        ChromeDriverService service = new ChromeDriverService.Builder().withLogLevel(ChromiumDriverLogLevel.fromString(getLogLevel(SELENIUM_LOGLEVEL,"ERROR"))).withLogOutput(System.out).build();  //ログをコンソールに出力
        ChromeDriver chromeDriver = new ChromeDriver(service, options);
        WebSiteControlDriver driver = new WebSiteControlDriver(chromeDriver);
        return driver;
    }

    public static WebSiteControlDriver createFirefoxDriver() {
        return WebSiteControlDriver.createFirefoxDriver(true);
    }
    public static WebSiteControlDriver createFirefoxDriver(boolean headlesMode){
        logger.info("WebSiteControlDriver.createFirefoxDriver headlesmode:{}", headlesMode);
        FirefoxOptions options = new WuFirefoxOptions(headlesMode);
        FirefoxDriverService service = new GeckoDriverService.Builder().withLogLevel(FirefoxDriverLogLevel.fromString(getLogLevel(SELENIUM_LOGLEVEL,"ERROR"))).withLogOutput(System.out).build();  //ログをコンソールに出力
        FirefoxDriver chromeDriver = new FirefoxDriver(service, options);
        WebSiteControlDriver driver = new WebSiteControlDriver(chromeDriver);
        return driver;
    }

    public WebSiteControlDriver(RemoteWebDriver driver){
        this.driver=driver;
        logger.info("OS Name:{}",System.getProperty("os.name"));
        logger.info("OS Arch:{}",System.getProperty("os.arch"));
        logger.info("OS Ver :{}",System.getProperty("os.version"));
        downloadFolderPath=createTmpDownloadFolder();
    }

    @Override
    public Capabilities getCapabilities() {
        return driver.getCapabilities();
    }

    @Override
    public List<String> getDownloadableFiles() {
        return driver.getDownloadableFiles();
    }

    @Override
    public void downloadFile(String fileName, Path targetLocation) throws IOException {
        driver.downloadFile(fileName, targetLocation);
    }

    @Override
    public void deleteDownloadableFiles() {
        driver.deleteDownloadableFiles();
    }

    @Override
    public @Nullable Object executeScript(String script, @Nullable Object... args) {
        return driver.executeScript(script, args);
    }

    @Override
    public @Nullable Object executeAsyncScript(String script, @Nullable Object... args) {
        return driver.executeAsyncScript(script, args);
    }

    @Override
    public Pdf print(PrintOptions printOptions) throws WebDriverException {
        return driver.print(printOptions);
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        return driver.getScreenshotAs(target);
    }

    @Override
    public void get(String url) {
        driver.get(url);
    }

    @Override
    public @Nullable String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public @Nullable String getTitle() {
        return driver.getTitle();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return driver.findElements(by);
    }

    @Override
    public WebElement findElement(By by) {
        return driver.findElement(by);
    }

    @Override
    public @Nullable String getPageSource() {
        return driver.getPageSource();
    }

    @Override
    public void close() {
        driver.close();
    }

    @Override
    public void quit() {
        driver.quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return driver.switchTo();
    }

    @Override
    public Navigation navigate() {
        return driver.navigate();
    }

    @Override
    public Options manage() {
        return driver.manage();
    }

    @Override
    public void setDelayEnabled(boolean enabled) {
        driver.setDelayEnabled(enabled);
    }

    @Override
    public void resetCooldown() {
        driver.resetCooldown();
    }

    @Override
    public FederatedCredentialManagementDialog getFederatedCredentialManagementDialog() {
        return driver.getFederatedCredentialManagementDialog();
    }

    @Override
    public void perform(Collection<Sequence> actions) {
        driver.perform(actions);
    }

    @Override
    public void resetInputState() {
        driver.resetInputState();
    }

    @Override
    public VirtualAuthenticator addVirtualAuthenticator(VirtualAuthenticatorOptions options) {
        return driver.addVirtualAuthenticator(options);
    }

    @Override
    public void removeVirtualAuthenticator(VirtualAuthenticator authenticator) {
        driver.removeVirtualAuthenticator(authenticator);
    }

    public static String createTmpDownloadFolder(){
        try {
            File tmpFolder= createTmpFolder("AutoWebOperator");
            tmpFolder.deleteOnExit();
            return tmpFolder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String getLogLevel(String envKey, String defaultValue){
        return getEnv(envKey,defaultValue);
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

    /**
     * 作ったフォルダはVM終了時に削除する。
     * @param tmpFolderName
     * @return
     * @throws IOException
     */
    public static File createTmpFolder(String tmpFolderName) throws IOException {
        return createTmpFolder(tmpFolderName, true);
    }
    public static File createTmpFolder(String tmpFolderName, boolean deleteOnExit) throws IOException {
        Path tmpFolderPath = Paths.get(System.getProperty("java.io.tmpdir"),tmpFolderName);
        File  tmpFolder = createTmpFolder(tmpFolderPath);
        if(deleteOnExit){
            tmpFolder.deleteOnExit();
        }
        return tmpFolder;
    }
    public static File createTmpFolder(Path tmpFolderPath) throws IOException {
        File tmpFolderDirectory=tmpFolderPath.toFile();
        if(tmpFolderDirectory.exists()){
            tmpFolderDirectory.delete();
        }
        Files.createDirectories(tmpFolderPath);
        tmpFolderDirectory.deleteOnExit();
        return tmpFolderDirectory;
    }
}
