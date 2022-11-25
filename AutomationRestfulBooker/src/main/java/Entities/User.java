package Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class User {

    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private String password;
    private String phone;
    private String additionalneeds;
    private LocalDate date;
}
