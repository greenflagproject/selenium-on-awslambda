package greenflagproject.selenium.apps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import greenflagproject.selenium.common.AutoWebOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AwsLambdaRequestHandler implements RequestHandler<Map<String,String>, String>{

    private final static Logger logger = LoggerFactory.getLogger(AwsLambdaRequestHandler.class);

    @Override
    public String handleRequest(Map<String, String> stringStringMap, Context context) {
        logger.info("ENVIRONMENT VARIABLES: {}", System.getenv());
        logger.info("EVENT: {}", stringStringMap);

        AutoWebOperator webOperator = null;
        try {
            webOperator = new KanpoWebOperationApp();
            webOperator.exec();
            return "SUCCESS.";
        }
        catch (RuntimeException e) {
            return "FAILED.";
        }
        finally {
            if(null!=webOperator) {
                webOperator.quit();
            }
        }
    }
}
