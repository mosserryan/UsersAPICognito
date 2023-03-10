package com.appsdeveloperblog.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import java.util.HashMap;
import java.util.Map;

public class ConfirmUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public ConfirmUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        Map<String, String> headers = new HashMap<>();
        LambdaLogger logger = context.getLogger();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        String requestBody = input.getBody();

        JsonObject userDetails = null;
        try {
            userDetails = JsonParser.parseString(requestBody).getAsJsonObject();
            String email = userDetails.get("email").getAsString();
            String confirmationCode = userDetails.get("code").getAsString();
            JsonObject createUserResult = cognitoUserService.confirmUserSignup(appClientId, appClientSecret, email, confirmationCode);
            response.withStatusCode(200);
            response.withBody(new Gson().toJson(createUserResult, JsonObject.class));
        } catch (AwsServiceException exception) {
            logger.log(exception.awsErrorDetails().errorMessage());
            ErrorResponse errorResponse = new ErrorResponse(exception.awsErrorDetails().errorMessage());
            response.withBody(new Gson().toJson(errorResponse, ErrorResponse.class));
            response.withStatusCode(exception.awsErrorDetails().sdkHttpResponse().statusCode());
        } catch (Exception exception) {
            logger.log(exception.getMessage());
            response.withBody(exception.getMessage());
            response.withStatusCode(500);
        }

        return response;
    }


}
