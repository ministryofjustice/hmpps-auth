package uk.gov.justice.digital.hmpps.oauth2server.auth.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PERSON")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"username"})
public class Person {
    @Id
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    public Person(final String username, final String firstName, final String lastName) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getName() {
        return String.format("%s %s", firstName, lastName);
    }
}
