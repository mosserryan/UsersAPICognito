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

public class AddUserToGroupHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String userPoolId;

    public AddUserToGroupHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.userPoolId = Utils.decryptKey("MY_COGNITO_POOL_ID");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.withHeaders(headers);

        LambdaLogger logger = context.getLogger();

        try {
            JsonObject requestBody = JsonParser.parseString(input.getBody()).getAsJsonObject();
            String groupName = requestBody.get("group").getAsString();
            String userName = input.getPathParameters().get("username");

            JsonObject addUserToGroupResult = cognitoUserService.addUserToGroup(groupName, userName, userPoolId);

            response.withBody(new Gson().toJson(addUserToGroupResult, JsonObject.class));
            response.withStatusCode(200);
        } catch (AwsServiceException exception) {
            logger.log(exception.awsErrorDetails().errorMessage());
            ErrorResponse errorResponse = new ErrorResponse(exception.awsErrorDetails().errorMessage());
            response.withBody(new Gson().toJson(errorResponse, ErrorResponse.class));
            response.withStatusCode(exception.awsErrorDetails().sdkHttpResponse().statusCode());
        } catch (Exception exception) {
            logger.log(exception.getMessage());
            ErrorResponse errorResponse = new ErrorResponse(exception.getMessage());
            response.withBody(new Gson().toJson(errorResponse, ErrorResponse.class));
            response.withStatusCode(500);
        }


        return response;
    }

}
