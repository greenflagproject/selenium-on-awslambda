package greenflagproject.selenium.common;

import org.openqa.selenium.*;
import org.openqa.selenium.federatedcredentialmanagement.FederatedCredentialManagementDialog;
import org.openqa.selenium.federatedcredentialmanagement.HasFederatedCredentialManagement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.print.PrintOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.virtualauthenticator.HasVirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

abstract public class AutoWebOperator implements WebDriver, JavascriptExecutor, HasCapabilities, HasDownloads, HasFederatedCredentialManagement, HasVirtualAuthenticator, Interactive, PrintsPage, TakesScreenshot {
    private final static Logger logger = LoggerFactory.getLogger(AutoWebOperator.class);

    /**
     * 実行時のheadless mode指定。
     * デフォルトはtrue(headless mode)。
     * true/falseを指定してください。
     */
    public final static String ENV_HEADLESS_MODE="HEADLESS_MODE";
    public final static boolean DEFAULT_HEADLESS_MODE=true;

    /**
     * タイムアウトやページ表示問題時に何回までリトライするかの設定。
     * デフォルトは1回。
     */
    public final static String ENV_EXECUTE_RETRY_TIMES="EXECUTE_RETRY_TIMES";
    final protected int DEFAULT_EXECUTE_RETRY_TIMES=1;
    protected int executeMaxRetryTimes;

    protected WebSiteControlDriver driver;
    protected List<String> pageStack;

    public AutoWebOperator(){
        this(WebSiteControlDriver.createDriver(CommonUtility.getBooleanEnv(ENV_HEADLESS_MODE,DEFAULT_HEADLESS_MODE)));
    }
    public AutoWebOperator(WebSiteControlDriver driver){
        this.driver=driver;
        pageStack= new ArrayList<>();
        executeMaxRetryTimes = getIntEnv(ENV_EXECUTE_RETRY_TIMES, DEFAULT_EXECUTE_RETRY_TIMES);
        this.downloadFolderPath=driver.getDownloadFolderPath();
        setProperty();
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
    public Object executeScript(String script, Object... args) {
        return driver.executeScript(script, args);
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
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
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public String getTitle() {
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
    public String getPageSource() {
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


    public void exec(){
        logger.info("Browser version: {}", driver.getCapabilities().getBrowserVersion());
        try{
            RuntimeException exception=null;
            int loopCounter=0;
            while(0==loopCounter || (null!=exception && loopCounter< executeMaxRetryTimes)){
                logger.info("AutoWebOperator.exec Ver:2025-05-19");
                exception=null;
                try {
                    execute();
                }
                catch(RuntimeException e){
                    exception=e;
                    exception.printStackTrace(System.out);
                }
                finally {
                    loopCounter++;
                }
            }
            if(null!=exception){
                printPageText();
                throw exception;
            }
        }
        finally {
            close();
        }
    }
    abstract protected void execute();




    protected boolean executedDownload=false;

    protected int timeoutSecond=10;
    protected void setTimeoutSecond(int sec){
        timeoutSecond=sec;
    }
    final protected WebDriverWait wait = new WebDriverWait(this, Duration.ofSeconds(timeoutSecond)); // 最大10秒待つ
    // 非表示ファイルを除くフィルタ
    protected FileFilter downloadFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return isDownloadedFile(file) && isDownloadFile(file);   //!file.isHidden() &&
        }

        private boolean isDownloadedFile(File file){
            String fileName=file.getName();
            return !(fileName.startsWith(".")) && !file.isHidden() && !fileName.endsWith(".crdownload");
        }
    };

    /**
     * ダウンロードファイルのチェックメソッド。
     * 継承先で必要に応じてOverrideする。
     *
     * @param file
     * @return
     */
    protected boolean isDownloadFile(File file){
        return true;
    }

    protected String downloadFolderPath;
    protected int beforeDownloadFolderFileNum =0;    //ダウンロード処理前のダウンロードフォルダファイル数
    protected LocalDateTime beforeDownloadDateTime=null;
    protected Instant beforeDownloadInstant=Instant.now();

    protected String mainWindowHandle;
    protected String getNewWindowHandle(){
        var handles = this.getWindowHandles();
        for(var handle : handles){
            if(!handle.equals(mainWindowHandle)){
                return handle;
            }
        }
        return null;
    }

    protected void get(String[] urlArray, By waitElement){
        for(String url : urlArray) {
            try {
                this.get(url);
                pageLoadWait(waitElement);
                return;
            } catch (RuntimeException e) {
                logger.warn("ページ遷移でエラーが発生しました URL[{}] TITLE[{}]。{}", url,getTitle(), e.getMessage());
                printPageText();
            }
        }
    }

    protected void pageLoadWait(By waitElement){
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(waitElement));
        }
        catch (RuntimeException e){
            printPageText();
            throw e;
        }
    }

    protected void setBeforeDownloadInfo(){
        setBeforeDownloadInfo(downloadFolderPath);
    }
    protected void setBeforeDownloadInfo(final String folderPath){
        beforeDownloadDateTime=LocalDateTime.now();
        beforeDownloadInstant=Instant.now();
        File directory = new File(folderPath);
        // ディレクトリが存在するか確認
        if (directory.exists() && directory.isDirectory()) {
            // ファイルとディレクトリの一覧を取得
            File[] files = directory.listFiles(downloadFilter);
            if (files != null) {
                beforeDownloadFolderFileNum =files.length;
            } else {
                beforeDownloadFolderFileNum =-1;
                throw new RuntimeException("ファイルが見つかりませんでした。");
            }
        } else {
            beforeDownloadFolderFileNum =-1;
            throw new RuntimeException("指定したパスは存在しないか、ディレクトリではありません。");
        }
    }

    protected File downloadedFile;
    protected List<File> downloadFileList =null;

    public File getDownloadedFile() {
        return downloadedFile;
    }

    public List<File> getDownloadedFileList() {
        if(downloadFileList==null){
            downloadFileList = new ArrayList<>();
            downloadFileList.add(downloadedFile);
            return downloadFileList;
        }

        return downloadFileList;
    }

    protected void setDownloadedFile(File downloadedFile) {
        if(checkInvoiceFileName(downloadedFile)) {
            this.downloadedFile = downloadedFile;
            if(downloadFileList==null){
                downloadFileList = new ArrayList<>();
            }
            downloadFileList.add(downloadedFile);
        }
        else{
            logger.info("データのファイル名が想定しているものではありませんでした[{}]",downloadedFile.getName());
        }
    }
    public void setDownloadedFile(){
        executedDownload=true;
        setDownloadedFile(getDownloadFile(downloadFolderPath));
        executedDownload=false;
    }

    /**
     * コンストラクタで呼び出される
     * 継承したクラスでメンバ変数の設定など、インスタンス作成時に行う処理を記載する。
     */
    protected void setProperty(){}


    protected File getDownloadFile(String dir){
        if(false==executedDownload){
            logger.info("ダウンロード処理が実行されていないため、ダウンロードファイル確認処理をスキップします。");
            return null;
        }
        //    List<String> files = ((HasDownloads) driver).getDownloadableFiles();

        File directory = new File(dir);
        // ディレクトリが存在するか確認
        if (directory.exists() && directory.isDirectory()) {
            // ファイルとディレクトリの一覧を取得
            File[] files = directory.listFiles(downloadFilter);
            if (files != null) {
                int loopMax=60;
                int loopCounter=0;
//                logger.debug(directory.listFiles(filter).toString());
                boolean found=false;
                while (loopCounter<loopMax) {   //ファイルが見つからなければ繰り返す。ファイルリストの取得からやり直すので、ループを外に一つ作った
                    logger.debug("beforeDownloadFolderFileNum: {}, directory.listFiles(filter): {}", beforeDownloadFolderFileNum, directory.listFiles(downloadFilter));
                    while (beforeDownloadFolderFileNum == Objects.requireNonNull(directory.listFiles(downloadFilter)).length && loopCounter < loopMax) {
                        loopCounter++;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    logger.info("loopCounter:{}{}", loopCounter, loopCounter == loopMax ? " タイムアウト" : "");
                    if (loopCounter == loopMax) {
                        logger.debug("{} beforeDownloadFolderFileNum:{} directory.listFiles(filter):{}", directory.getPath(), beforeDownloadFolderFileNum, directory.listFiles(downloadFilter).length);
                        throw new RuntimeException("ダウンロードタイムアウト。");
                    }
                    for (File file : directory.listFiles(downloadFilter)) {
                        try {
                            var udt = Files.getLastModifiedTime(file.toPath()).toInstant();
                            var diff = udt.compareTo(beforeDownloadInstant);
                            logger.debug("beforeDownloadInstant:{}", beforeDownloadInstant);
                            logger.debug("filepath:{}", file.toPath());
                            logger.debug("udt:{}", udt);
                            logger.debug("diff:{}", diff);
                            if (0 < diff) {
                                file.deleteOnExit();
                                logger.info("ダウンロードファイル: [{}]", file.toString());
                                return Paths.get(dir, file.getName()).toFile();
                            } else {
                                logger.debug("ファイルチェックエラー: [{}]", file.toString());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("ダウンロードフォルダ処理エラー。");
                        }
                    }
                }
            }
            else {
                throw new RuntimeException("ファイルが見つかりませんでした。");
            }
        } else {
            throw new RuntimeException("指定したパスは存在しないか、ディレクトリではありません。");
        }

        throw new RuntimeException("指定したパスのファイルを探す処理に失敗しました。");
    }

    public String getBaseLocation(){
        return null;
    }

    public void loadWait(String xPath){
        loadWait(By.xpath(xPath));
    }
    public void loadWait(By location){
        wait.until(ExpectedConditions.visibilityOfElementLocated(location));
    }
    public WebElement findElement(String xPath){
        return this.findElement(By.xpath(xPath));
    }
    public String getText(String xPath){
        return getText(findElement(xPath));
    }
    public String getText(WebElement element){
        return element.getText();
    }
    public String getAttribute(String xPath, String name){
        return getAttribute(findElement(xPath), name);
    }
    public String getAttribute(WebElement element, String name){
        return element.getAttribute(name);
    }



    public boolean isChecked(String xPath){
        return isChecked(By.xpath(xPath));
    }
    public boolean isChecked(By by){
        var e = this.findElement(by);
        boolean b = e.isSelected();
        return b;
    }
    public void checkOnCheckBox(String xPath){
        checkCheckBox(true, By.xpath(xPath));
    }
    public void checkOffCheckBox(String xPath){
        checkCheckBox(false, By.xpath(xPath));
    }
    public void checkCheckBox(boolean on, String xPath){
        checkCheckBox(on, By.xpath(xPath));
    }
    public void checkCheckBox(boolean on, By location){
        //指定された状態と異なっていた場合は状態変更する
        if(!(on==isChecked(location))){
            clickElement(location);
        }

    }

    public void inputFormByXpath(String xPath, Number num, int maxRetryTimes){
        inputFormByXpath(xPath, num.toString(), maxRetryTimes);
    }
    public void inputFormByXpath(String xPath, Number num){
        inputFormByXpath(xPath, num.toString());
    }
    public void inputFormByXpath(String xPath, String text, int maxRetryTimes) {
        inputFormByXpath(By.xpath(xPath), text, maxRetryTimes);
    }
    public void inputFormByXpath(By b, String text, int maxRetryTimes){
        for(int retryTimes=0; retryTimes<maxRetryTimes; retryTimes++){
            try {
                WebElement e = this.findElement(b);
                wait.until(ExpectedConditions.visibilityOfElementLocated(b));
                inputForm(e, text);
                String s = e.getAttribute("value");
                if(s!=null && s.equals(text)) {
                    retryTimes = maxRetryTimes;
                }
                else{
                    logger.warn("inputFormByXpath retryTimes:{}", retryTimes);
                    e.clear();
                    try {
                        Thread.sleep(1500);
                    }
                    catch (InterruptedException ignored) {}
                }
            }
            catch (Exception e){
                logger.error("inputFormByXpath retryTimes:{} [{}]", retryTimes, e.toString());
                try {
                    Thread.sleep(1500);
                }
                catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
                }
            }
        }

    }

    public void inputFormByXpath(String xPath, String text){
        By by=By.xpath(xPath);
        inputForm(by, text, true);
    }
    public void inputFormByXpath(String xPath, String text, boolean doWait){
        By by=By.xpath(xPath);
        inputForm(by, text, doWait);
    }

    public void inputForm(By by, String text, boolean doWait){
        if(doWait) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        }
        inputForm(this.findElement(by), text);
        logger.debug("celenium inputForm:{} value:{}",by.toString(), text);
    }
    public void inputForm(WebElement element, String text){
        element.clear();
        element.sendKeys(text);
    }

    public void inputForm(WebElement formElement, String xpath, String text){
        var element = formElement.findElement(By.xpath(xpath));
        element.clear();
        element.sendKeys(text); //element!=null is always true.
    }
    public void inputForm(WebElement formElement, By by, String text){
        var element = formElement.findElement(by);
        element.clear();
        element.sendKeys(text); //element!=null is always true.
    }
    public void clickByXpath(String xPath){
        clickByXpath(this, xPath);
    }
    public void clickByXpath(SearchContext element, String xPath){
        By location=By.xpath(xPath);
        WebElement clickElement=element.findElement(location);
        clickElement.click();
    }

    public void clickAByXpath(String xPath){
        By by=By.xpath(xPath);
        clickA(by);
    }
    public void clickA(By by){
        clickA(by, true);
    }
    public void clickA(By by, boolean doWait){
        if(doWait) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        }
        var element = this.findElement(by);
        element.click();
    }

    /**
     * マウスをオブジェクトの上に乗せてメニューを開く
     * @param xPath
     */
    public void performElementByXpath(String xPath){
        performElementByXpath(xPath, true);
    }
    /**
     *
     * @param xPath
     * @param doWait
     */
    public void performElementByXpath(String xPath, boolean doWait){
        By by=By.xpath(xPath);
        performElement(by, doWait);
    }

    public void performElement(By by){
        performElement(by, true);
    }
    public void performElement(By by, boolean doWait){
        if(doWait) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        }
        Actions actions = new Actions(this);
        var element = this.findElement(by);
        actions.moveToElement(element).perform();
    }

    public void clickElementByXpath(String xPath){
        clickElementByXpath(xPath, true);
    }

    /**
     *
     * @param xPath
     * @param doWait
     */
    public void clickElementByXpath(String xPath, boolean doWait){
        By by=By.xpath(xPath);
        clickElement(by, doWait);
    }


    public void clickElement(String xPath){
        clickElement(By.xpath(xPath), true);
    }
    public void clickElement(By by){
        clickElement(by, true);
    }
    public void clickElement(By by, boolean doWait){
        if(doWait) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        }
        var element = this.findElement(by);
        element.click();
    }
    public void selectByVisibleText(String xPath, String text){
        selectByVisibleText(getWebElement(xPath), text);
    }
    public void selectByVisibleText(By location, String text){
        WebElement selectElement = this.findElement(location);
        selectByVisibleText(selectElement, text);
    }
    public void selectByVisibleText(WebElement element, String text){
        Select select = new Select(element);
        select.selectByVisibleText(text);
    }

    public void selectByValue(String xPath, String value){
        selectByValue(getWebElement(xPath), value);
    }

    public void selectByValue(By location, String value){
        WebElement selectElement = this.findElement(location);
        selectByValue(selectElement, value);
    }
    public void selectByValue(WebElement element, String value){
        Select select = new Select(element);
        select.selectByValue(value);
    }

    public WebElement getWebElement(String xPath){
        var element = this.findElement(By.xpath(xPath));
        return element;
    }

    /**
     * 処理中画面表示が消えるのを待つ
     * @param element
     */
    protected void loadingWait(WebElement element){
        final int interval=500;
        final int maxWaitMilliSec=20*1000;
        int waitingMilliSec=0;
        try {
            if (!element.isDisplayed()) {
                if (maxWaitMilliSec < waitingMilliSec) {
                    throw new TimeoutException();
                }
                Thread.sleep(interval);
                waitingMilliSec += interval;
            }
        } catch (Exception ignore) {    //待ち対象の要素がない時は例外が飛んでくる
        }

    }

    /**
     * 請求書ファイル名のチェックをする場合はこのメソッドをOverrideすること。
     * @param extractedFile
     * @return
     */
    public boolean checkInvoiceFileName(File extractedFile){
        return true;
    }

    protected void printPageText(){
        logger.debug("ページテキスト出力");
        try {
            By element = By.xpath("/html");
            logger.debug(this.findElement(element).getText());
        }
        catch (RuntimeException e){
            logger.warn("ページテキスト出力でエラーが発生しました。{}", e.getMessage());
        }
    }

    /**
     *
     * @return 移ったウィンドウハンドルを返す
     */
    protected String switchNewTab(){
        return switchNewTab(null);
    }
    protected String switchNewTab(By waitLocation){
        if(pageStack == null) {
            pageStack= new ArrayList<>();
        }

        var windowHandles=this.getWindowHandles();
        for(String h : windowHandles){
            if(pageStack.contains(h)){
                continue;
            }
            pageStack.add(h);
            switchTo().window(h);

            // ロード待ちのロケーションが設定されていたら、対象のロケーションが読み込まれるまで待つ
            if(null!=waitLocation) {
                loadWait(waitLocation);
            }
            return h;
        }
        throw new RuntimeException("開いたTabをActiveにできませんでした。");
    }

    public int getIntEnv(String key, int defaultValue) {
        try{
            String s =System.getenv(key);
            if(null!=s && !s.isEmpty()){
                return Integer.parseInt(s);
            }
            return defaultValue;
        }
        catch (RuntimeException ignore){}
        return defaultValue;
    }
}
