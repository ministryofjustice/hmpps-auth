package uk.gov.justice.digital.hmpps.oauth2server.delius.model;

import lombok.Data;

import java.util.List;

@Data
public class UserDetails {
    private String surname;
    private String firstName;
    private String email;
    private boolean locked;
    private List<UserRole> roles;
}
