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

public class ReceivingUserOrders {
    private Map<String, String> testData = new HashMap<>();
    private Map<Integer, String> listIngredients = new HashMap<>();
    private Response response;

    @Before
    public void setUp() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
    }

    @Test
    @DisplayName("Obtaining information about orders of an authorized user")
    public void obtainingInfoOrdersAuthUsers(){
        getListOfIngredients();
        createUniqueUser();
        Response responseBuffer = response;
        createOrder(getToken(),generateIngredientsPool());
//        System.out.println(response.getBody().asString());
        checkAttribute("name","notNullValue");
        checkAttribute("success",true);
        checkAttribute("order.number","notNullValue");
        checkingResponseCode(200);
        response = responseBuffer;
        obtainingInfoOrdersUsers(getToken());
        checkingResponseCode(200);
        checkAttribute("success",true);
        response = responseBuffer;
        deleteUser();
        checkingResponseCode(202);
        checkAttribute("success",true);
        checkAttribute("message", "User successfully removed");
    }



    @Test
    @DisplayName("Receiving information about orders unauthorized user")
    public void receivingInfoOrdersUnAuthUser(){
        obtainingInfoOrdersUsers("");
        checkingResponseCode(401);
        checkAttribute("success",false);
        checkAttribute("message", "You should be authorised");
    }



    @Step("selection of ingredient pool")
    private String generateIngredientsPool(){
        String poolIngredients;
        poolIngredients = "{"+"\"ingredients\":[\""+listIngredients.get(1 + (int)(Math.random() * ((listIngredients.size() - 1) + 1)))+"\""+","+"\""+listIngredients.get(1 + (int)(Math.random() * ((listIngredients.size() - 1) + 1)))+"\""+","+"\""+listIngredients.get(1 + (int)(Math.random() * ((listIngredients.size() - 1) + 1)))+"\""+"]}";
        return poolIngredients;
    }

    @Step("Obtaining information about user orders")
    private void obtainingInfoOrdersUsers(String token){
        if (!(token.isEmpty())){
            response = given().auth().oauth2(token).and().when().get("/api/orders");
        }
        else {
            response = given().and().when().get("/api/orders");
        }
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

    @Step("Receiving a token")
    private String getToken(){
        String token;

        StringBuilder builder = new StringBuilder(response.getBody().asString());
        String userAccedesToken = builder.substring(builder.indexOf("\"accessToken\":\"",0),builder.indexOf("\"",102))+"\"";
        builder = new StringBuilder(userAccedesToken);
        token = builder.substring(builder.indexOf("Bearer ",0)+7,builder.indexOf("\"",22));

        return token;
    }
    @Step("Create an order")
    private void createOrder(String authToken, String userData){
        if (!(authToken.isEmpty())) {
            response = given().header("Content-type", "application/json").auth().oauth2(authToken).and().body(userData).when().post("/api/orders");
        }
        else {response = given().header("Content-type", "application/json").and().body(userData).when().post("/api/orders");}
    }

    @Step("Get list of ingredients")
    private void getListOfIngredients(){
        response = given().get("/api/ingredients");
        String flag = "\"_id\":\"";
        StringBuilder builder = new StringBuilder(response.getBody().asString());
        int i = 0;
        while (builder.toString().contains(flag)){
            builder = builder.delete(0,builder.indexOf(flag,0)+7);
            i++;
            listIngredients.put(i,builder.substring(0,builder.indexOf("\"",0)));
        }
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
}
