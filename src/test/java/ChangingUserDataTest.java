import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ChangingUserDataTest {

    private Response response;
    private Map<String, String> testData = new HashMap<>();

    @Before
    public void setUp() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
    }

    @DisplayName("Changing user data with authorization")
    @Test
    public void changingUserDataWithAuthorization() {
        createUniqueUser();
        testDataGen();
        String userData = "{\n" +
                "\"email\": \""+testData.get("email")+"\",\n" +
                "\"password\": \""+testData.get("password")+"\",\n" +
                "\"name\": \""+testData.get("name")+"\"\n}";
        changingUserData(getToken(),userData);
        login(userData);
        checkingResponseCode(200);
        checkAttribute("success",true);
        checkAttribute("user.email",testData.get("email"));
        checkAttribute("user.name",testData.get("name"));
        checkAttribute("accessToken","notNullValue");
        checkAttribute("refreshToken","notNullValue");
        deleteUser();
        checkingResponseCode(202);

    }

    @DisplayName("Changing user data with out authorization")
    @Test
    public void changingUserDataWitOutAuthorization() {
        createUniqueUser();
        Response responseBuffer = response;
        testDataGen();
        String userData = "{\n" +
                "\"email\": \""+testData.get("email")+"\",\n" +
                "\"password\": \""+testData.get("password")+"\",\n" +
                "\"name\": \""+testData.get("name")+"\"\n}";
        changingUserData("",userData);
        checkingResponseCode(401);
        checkAttribute("success",false);
        checkAttribute("message","You should be authorised");
        response = responseBuffer;
        deleteUser();
        checkingResponseCode(202);

    }


    @Step("Login is performed with the data of an existing user")
    private void login(String userData){
        response = given().header("Content-type", "application/json").and().body(userData).when().post("/api/auth/login");
    }

    @Step("Delete the created user")
    private void deleteUser()
    {
        StringBuilder builder = new StringBuilder(response.getBody().asString());
        String userAccedesToken = builder.substring(builder.indexOf("\"accessToken\":\"",0),builder.indexOf("\"",102))+"\"";
        builder = new StringBuilder(userAccedesToken);
        String token = builder.substring(builder.indexOf("Bearer ",0)+7,builder.indexOf("\"",22));
        response = given().auth().oauth2(token).and().body("{"+userAccedesToken+"}").when().delete("/api/auth/user");
        response.then().statusCode(202);
    }


    @Step("Changing user data")
    private void changingUserData(String token, String userData){
        if (!(token.isEmpty())){
            response = given().header("Content-type", "application/json").auth().oauth2(token).and().body(userData).when().patch("/api/auth/user");
        }
        else {
            response = given().header("Content-type", "application/json").and().body(userData).when().patch("/api/auth/user");
        }
    }

    @Step("Receiving a token")
    private String getToken(){
        String token;

        StringBuilder builder = new StringBuilder(response.getBody().asString());
        String userAccedesToken = builder.substring(builder.indexOf("\"accessToken\":\"",0),builder.indexOf("\"",102))+"\"";
        builder = new StringBuilder(userAccedesToken);
         token = builder.substring(builder.indexOf("Bearer ",0)+7,builder.indexOf("\"",22));

        return token;
    }

    @Step("Creating a unique user")
        private void createUniqueUser(){
            testDataGen();
            String userData = "{\n" +
                    "\"email\": \""+testData.get("email")+"\",\n" +
                    "\"password\": \""+testData.get("password")+"\",\n" +
                    "\"name\": \""+testData.get("name")+"\"\n}";
            createUserWithUserData(userData);
            checkingResponseCode(200);
            checkAttribute("success",true);
            checkAttribute("user.email",testData.get("email"));
            checkAttribute("user.name",testData.get("name"));
            checkAttribute("accessToken","notNullValue");
            checkAttribute("refreshToken","notNullValue");
        }

    @Step("test data generation")
    private void testDataGen(){
        Random rn = new Random();
        int randomNum = rn.nextInt(9999999);
        testData.put("email","test-data"+randomNum+"@yandex.ru");
        testData.put("password","password123");
        testData.put("name","Username"+randomNum);
    }

    @Step("Send POST request to /api/auth/register")
    private void createUserWithUserData(String userData){
        response = given().header("Content-type", "application/json").and().body(userData).when().post("/api/auth/register");
    }

    @Step("The response code is checked as expected")
    private void checkingResponseCode(int code){
        response.then().statusCode(code);
    }

    @Step("Check for the presence of an attribute in the response")
    private void checkAttribute(String attribute, Object expectedValue){
        if (expectedValue.equals("notNullValue")){
            response.then().assertThat().body(attribute,notNullValue());
        }
        else {
            response.then().assertThat().body(attribute,equalTo(expectedValue));
        }
    }
}
