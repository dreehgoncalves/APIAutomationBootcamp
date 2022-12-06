import Entities.*;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.filter.log.ErrorLoggingFilter;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookingTests {

    private static User user;
    private static Auth auth;
    private static BookingDates bookingdates;
    private static Booking booking;
    public static Faker faker;
    public static RequestSpecification request;
    public static BookingUserDetails bookingid;
    public static LocalDate date;


    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";

        faker = new Faker();
        auth = new Auth("admin", "password123");
        user = new User(
                faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                faker.internet().safeEmailAddress(),
                faker.internet().password(8, 10),
                faker.phoneNumber().toString(),
                faker.superhero().name(),
                date.now().plusDays(faker.number().randomNumber())
        );

        bookingdates = new BookingDates(
                user.getDate().toString(),
                user.getDate().plusDays(faker.number().numberBetween(1, 10)).toString());

        bookingid = new BookingUserDetails(
                (int) faker.number().randomNumber());

        booking = new Booking(
                user.getFirstname(), user.getLastname(),
                (float) faker.number().randomDouble(2, 50, 100000),
                faker.bool().bool(),
                bookingdates,
                user.getAdditionalneeds());

        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter(), new ErrorLoggingFilter());
    }

    @BeforeEach
    void setRequest(){

        Response response = given()
                .contentType(ContentType.JSON)
                .body(auth)
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .extract().response();

        String token = response.jsonPath().getString("token");

        request = given()
                        .config(RestAssured.config()
                                .logConfig(logConfig()
                                        .enableLoggingOfRequestAndResponseIfValidationFails()))
                        .contentType(ContentType.JSON).cookie("token", token);
    }

    //Cria uma nova reserva
    @Test
    @Order(12)
    public void createBooking_WithValidData_ReturnOk(){
        bookingdates.setCheckin("");
        bookingdates.setCheckout("");
        request
                .when()
                .body(booking)
                .post("/booking")
                .then()
                .body(matchesJsonSchemaInClasspath("creatingBookingRequestSchema.json"))
                .and()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .time(lessThan(2000L));
    }

    @Test
    @Order(1)
    public void createBooking_WithInvalidData_Return400(){
        request
                .when()
                .body(booking)
                .post("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .time(lessThan(2000L));
    }

    //Filtra as reservas por nome
    @Test
    @Order(2)
    public void getAllBookingsByFirstName_WithValidData_ReturnOk(){
        Integer id = request
                .when()
                .queryParam("firstname", user.getFirstname())
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0))).extract().response().body().path("[0].bookingid");

        bookingid.setBookingid(id);
    }

    //Filtra as reservas por sobrenome
    @Test
    @Order(3)
    public void getAllBookingsByLastName_WithValidData_ReturnOk(){
        request
                .when()
                .queryParam("lastname", user.getLastname())
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0)));
    }

    //Filtra as reservas por nome
    @Test
    @Order(4)
    public void getAllBookingsByFirsAndLastName_WithValidData_ReturnOk(){
        request
                .when()
                .queryParam("firstname", user.getFirstname())
                .queryParam("lastname", user.getLastname())
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0)));
    }

    //Filtra as reservas por checkin e checkout
    @Test
    @Order(5)
    public void getAllBookingsByCheckinAndCheckout_WithValidData_ReturnOk(){

        request
                .when()
                .queryParam("checkin", user.getDate().minusDays(faker.number().numberBetween(1, 20)).toString())
                .queryParam("checkout", user.getDate().plusDays(faker.number().numberBetween(1, 10)).toString())
                .get("/booking")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body("results", hasSize(greaterThan(0)));
    }

    //Retorna uma reserva específica
    @Test
    @Order(6)
    public void getBookingById_WithValidData_ReturnOk(){
        request
                .when()
                .pathParam("bookingid", bookingid.getBookingid())
                .get("/booking/{bookingid}")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body(matchesJsonSchemaInClasspath("updateBookingRequestSchema.json"));

    }

    //Busca todas as reservas
    @Test
    @Order(7)
    public void getAllBookings_WithValidData_ReturnOk(){

        Response response = request
                                .when()
                                    .get("/booking")
                                .then()
                                    .extract()
                                    .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatusCode());
    }

    //Atualiza uma reserva
    @Test
    @Order(8)
    public void updateBooking_returnOK(){

        request.when()
                .body(booking)
                .and()
                .pathParam("bookingid", bookingid.getBookingid())
                .put("/booking/{bookingid}")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body(matchesJsonSchemaInClasspath("updateBookingRequestSchema.json"));
    }

    //Atualiza uma reserva
    @Test
    @Order(9)
    public void updateBookingParcially_returnOK(){

        request.when()
                .body(booking)
                .and()
                .pathParam("bookingid", bookingid.getBookingid())
                .patch("/booking/{bookingid}")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .and()
                .body(matchesJsonSchemaInClasspath("updateBookingRequestSchema.json"));
    }

    //Deleta uma reserva
    @Test
    @Order(10)
    public void deleteBookingById_WithValidData_ReturnOk(){
        request
                .when()
                .pathParam("bookingid", bookingid.getBookingid())
                .delete("/booking/{bookingid}")
                .then()
                .assertThat()
                .statusCode(201);

    }

    //Busca todas as reservas
    @Test
    @Order(11)
    public void healthCheck_ReturnOk(){

        Response response = request
                .when()
                .get("/ping")
                .then()
                .extract()
                .response();

        Assertions.assertNotNull(response);
        Assertions.assertEquals(201, response.getStatusCode(), response.getBody().asString());
    }
}
