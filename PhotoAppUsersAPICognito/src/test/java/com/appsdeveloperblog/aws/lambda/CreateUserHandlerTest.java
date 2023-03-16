package com.appsdeveloperblog.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.appsdeveloperblog.aws.lambda.shared.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class CreateUserHandlerTest {

  @Mock
  CognitoUserService cognitoUserService;
  @Mock
  APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;
  @Mock
  Context context;
  @Mock
  LambdaLogger lambdaLogger;
  @InjectMocks
  CreateUserHandler handler;

  @BeforeAll
  public static void runBeforeAllTestMethods() {
    System.out.println("Executing @BeforeAll Method");
  }
  @AfterAll
  public static void runAfterAllTestMethods() {
    System.out.println("Executing @AfterAll Method");
  }

  @BeforeEach
  public void runBeforeEachTestMethod() {
    System.out.println("Executing @BeforeEach Method");
    when(context.getLogger()).thenReturn(lambdaLogger);
  }

  @AfterEach
  public void runAfterEachTestMethod() {
    System.out.println("Executing @AfterEach Method");
  }

  @Test
  public void successfulResponse() {
    //Arrange
    JsonObject userDetails = new JsonObject();
    userDetails.addProperty("firstName", "Ryan");
    userDetails.addProperty("lastName", "Mosser");
    userDetails.addProperty("email", "mosser.ryan.a@gmail.com");
    userDetails.addProperty("password", "123456789");
    String userDetailsJsonString = new Gson().toJson(userDetails, JsonObject.class);

    when(apiGatewayProxyRequestEvent.getBody()).thenReturn(userDetailsJsonString);

    JsonObject createUserResult = new JsonObject();
    createUserResult.addProperty(Constants.IS_SUCCESSFUL, true);
    createUserResult.addProperty(Constants.STATUS_CODE, 200);
    createUserResult.addProperty(Constants.COGNITO_USER_ID, UUID.randomUUID().toString());
    createUserResult.addProperty(Constants.IS_CONFIRMED, false);
    when(cognitoUserService.createUser(any(), any(), any())).thenReturn(createUserResult);

    //Act
    APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(apiGatewayProxyRequestEvent, context);
    String responseBody = responseEvent.getBody();
    JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

    //Assert
    assertTrue(responseBodyJson.get(Constants.IS_SUCCESSFUL).getAsBoolean());
    assertEquals(200, responseBodyJson.get(Constants.STATUS_CODE).getAsInt());
    assertNotNull(responseBodyJson.get(Constants.COGNITO_USER_ID).getAsString());
    assertFalse(createUserResult.get(Constants.IS_CONFIRMED).getAsBoolean());

    verify(lambdaLogger, times(1)).log(anyString());
    verify(cognitoUserService, times(1)).createUser(any(), any(), any());

  }

  @Test
  public void testHandleRequest_whenEmptyRequestBodyProvided_returnsErrorMessage() {
    // Arrange
    when(apiGatewayProxyRequestEvent.getBody()).thenReturn("");

    // Act
    APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(apiGatewayProxyRequestEvent, context);
    String responseBody = responseEvent.getBody();
    JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

    // Assert
    assertEquals(500, responseEvent.getStatusCode());
    assertNotNull(responseBodyJson.get("message"), "Missing the 'message' property in JSON response.");
    assertFalse(responseBodyJson.get("message").getAsString().isEmpty(), "Error message should not be empty");
  }

  @Test
  public void testHandleRequest_whenAwsServiceException_returnErrorMessage() {
    // Arrange
    when(apiGatewayProxyRequestEvent.getBody()).thenReturn("{}");

    AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder()
            .errorCode("")
            .sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build())
            .errorMessage("AwsServiceException took place")
            .build();

    when(cognitoUserService.createUser(any(), any(), any())).thenThrow(
            AwsServiceException.builder()
                    .statusCode(500)
                    .awsErrorDetails(awsErrorDetails)
                    .build());
    // Act
    APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(apiGatewayProxyRequestEvent, context);
    String responseBody = responseEvent.getBody();
    JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

    // Assert
    assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), responseEvent.getStatusCode());
    assertNotNull(responseBodyJson.get("message"));
    assertEquals(awsErrorDetails.errorMessage(), responseBodyJson.get("message").getAsString());

  }

}
