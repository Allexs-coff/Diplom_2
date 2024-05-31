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

public class CreateNewUserTest {
    private Response response;
    private Map<String, String> testData = new HashMap<>();


    @Before
    public void setUp() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
    }
    @Test
    @DisplayName("Creating a unique user")
    public void createUniqueUser(){
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
        deleteUser();
        checkingResponseCode(202);
        checkAttribute("success",true);
        checkAttribute("message", "User successfully removed");
    }

    @Test
    @DisplayName("Creating a duplicate user")
    public void createDuplicateUser(){
        testDataGen();
        String userData = "{\n" +
                "\"email\": \""+testData.get("email")+"\",\n" +
                "\"password\": \""+testData.get("password")+"\",\n" +
                "\"name\": \""+testData.get("name")+"\"\n}";
        createUserWithUserData(userData);
        checkingResponseCode(200);
        createUserWithUserData(userData);
        checkingResponseCode(403);
        checkAttribute("success",false);
        checkAttribute("message", "User already exists");
    }

    @Test
    @DisplayName("Creating a user without the required email field")
    public void createUserWithOutEmail(){
        testDataGen();
        String userData = "{\n" +
                "\"password\": \""+testData.get("password")+"\",\n" +
                "\"name\": \""+testData.get("name")+"\"\n}";
        createUserWithUserData(userData);
        checkingResponseCode(403);
        checkAttribute("success",false);
        checkAttribute("message","Email, password and name are required fields");
    }

    @Test
    @DisplayName("Creating a user without the required Password field")
    public void createUserWithOutPassword(){
        testDataGen();
        String userData = "{\n" +
                "\"email\": \""+testData.get("email")+"\",\n" +
                "\"name\": \""+testData.get("name")+"\"\n}";
        createUserWithUserData(userData);
        checkingResponseCode(403);
        checkAttribute("success",false);
        checkAttribute("message","Email, password and name are required fields");
    }

    @Test
    @DisplayName("Creating a user without the required Name field")
    public void createUserWithOutName(){
        testDataGen();
        String userData = "{\n" +
                "\"email\": \""+testData.get("email")+"\",\n" +
                "\"password\": \""+testData.get("password")+"\"}";
        createUserWithUserData(userData);
        checkingResponseCode(403);
        checkAttribute("success",false);
        checkAttribute("message","Email, password and name are required fields");
    }

    @Step("Delete the created user")
    public void deleteUser()
    {
        StringBuilder builder = new StringBuilder(response.getBody().asString());
        String userAccedesToken = builder.substring(builder.indexOf("\"accessToken\":\"",0),builder.indexOf("\"",102))+"\"";
        builder = new StringBuilder(userAccedesToken);
        String token = builder.substring(builder.indexOf("Bearer ",0)+7,builder.indexOf("\"",22));
        response = given().auth().oauth2(token).and().body("{"+userAccedesToken+"}").when().delete("/api/auth/user");
    }


    @Step("Send POST request to /api/auth/register")
    private void createUserWithUserData(String userData){
        response = given().header("Content-type", "application/json").and().body(userData).when().post("/api/auth/register");
    }

    @Step("test data generation")
    private void testDataGen(){
        Random rn = new Random();
        int randomNum = rn.nextInt(9999999);
        testData.put("email","test-data"+randomNum+"@yandex.ru");
        testData.put("password","password123");
        testData.put("name","Username"+randomNum);
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

    @Step("The response code is checked as expected")
    private void checkingResponseCode(int code){
        response.then().statusCode(code);
    }




}
