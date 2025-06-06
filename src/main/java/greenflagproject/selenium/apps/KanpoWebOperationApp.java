package greenflagproject.selenium.apps;

import greenflagproject.selenium.common.AutoWebOperator;
import greenflagproject.selenium.common.CommonUtility;
import greenflagproject.selenium.common.WebSiteControlDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class KanpoWebOperationApp extends AutoWebOperator {
    public static void main(String[] args){
        AutoWebOperator webOperator = null;
        try {
            webOperator = new KanpoWebOperationApp(WebSiteControlDriver.createDriver(CommonUtility.getBooleanEnv("HEADLESS_MODE",true)));
            webOperator.exec();
        }
        finally {
            if(null!=webOperator) {
                webOperator.quit();
            }
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(KanpoWebOperationApp.class);

    public KanpoWebOperationApp(){
        super();
    }

    public KanpoWebOperationApp(WebSiteControlDriver driver){
        super(driver);
    }

    @Override
    protected void execute() {
        open();
        search();
    }

    private void open(){
        String[] urlArray= {"https://www.kanpo.go.jp/"};
        By waitElement = By.tagName("body");
        get(urlArray, waitElement);
        logger.info("ページ遷移しました[{}]。", getTitle());

    }

    protected void search(){
        logger.info("#search");
//        List<WebElement> todayBoxWebElements = findElements(By.className("todayBox"));
        List<WebElement> todayBoxWebElements = findElements(By.id("todayProducts"));
        WebElement dateElement = todayBoxWebElements.getFirst().findElement(By.tagName("dt"));
        String dateString = dateElement.getText().replaceAll("\\n.*$","");  //日付以降の文言が改行に続いて記載されているので、それを除去

        for (WebElement todayBoxWebElement : todayBoxWebElements) {
            List<WebElement> liTagArray = todayBoxWebElement.findElements(By.tagName("li"));
            for (WebElement li : liTagArray) {
                logger.info("#search[{}]", li.getText());
                download(dateString, li);
            }
        }

    }

    protected void download(String dateString, WebElement liWebelement){
        String titleNumber="";
        List<WebElement> aTagList = liWebelement.findElements(By.tagName("a"));
        for(var pdfLink : aTagList) {
            String pdfLinkUrl = pdfLink.getAttribute("href");
            if(false == pdfLinkUrl.contains("full")) {
                titleNumber=getDocumentTitle(pdfLink);
            }
            else {
                String titlePage = getDocumentTitle(pdfLink);
                downloadPdf(aTagList.getLast(), dateString+"_"+titleNumber+"_"+titlePage.replaceAll("\\[.*\\]","")+".pdf"); //ファイル拡張子はpdfと決めつけて処理をしている。
            }
        }
    }
    protected String getDocumentTitle(WebElement aWebelement){
        return aWebelement.getText().replaceAll("\n","");
    }
    protected void downloadPdf(WebElement aWebelement, String title){
        loadWait(By.tagName("main"));
        String currentUrl = aWebelement.getAttribute("href");
        Path path = Paths.get(currentUrl);
        Path base = path.getParent();
        Path con = path.getFileName();
        Path pdfUrl=Paths.get(base.toString(),"pdf",con.toString().replaceAll("f.html",".pdf"));//ファイルの種類はpdfと決めつけて処理をしている。
        try {
            downloadFile(pdfUrl.toString().replaceAll("https:/", "https://"), title);   //Paths.getで処理した際に「//」が「/」になってしまうので、対応しておく。本質的には、Paths.getの利用を見直す。
        }
        catch (IOException ignore){}
    }

    /**
     * HttpURLConnectionでデータをダウンロード
     * @param linkURL
     * @param fileName
     * @throws IOException
     */
    public void downloadFile(String linkURL, String fileName) throws IOException {
        URL url = java.net.URI.create(linkURL).toURL();
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        InputStream inputStream = httpConn.getInputStream();
        FileOutputStream outputStream = new FileOutputStream(Paths.get(downloadFolderPath,fileName).toFile());
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.close();
        inputStream.close();
    }
}
