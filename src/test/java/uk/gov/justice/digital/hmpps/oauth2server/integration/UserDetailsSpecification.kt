package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class UserDetailsSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var userDetailsPage: UserDetailsPage

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Test
  fun `A user can change their user details`() {
    val homePage = goTo(loginPage).loginAs("AUTH_ADM")
    val currentName = homePage.getCurrentName()

    goTo(userDetailsPage)
        .checkCurrentName(currentName)
        .submitUserDetails("   Joe  ", "  New Name  ")

    accountDetailsPage.isAt()
    assertThat(homePage.getCurrentName()).isEqualTo("Joe New Name")

    goTo(userDetailsPage)
        .checkCurrentName("Joe New Name")
  }

  @Test
  fun `A user can cancel changing their user details`() {
    val homePage = goTo(loginPage).loginAs("AUTH_ADM")

    val currentName = homePage.getCurrentName()

    goTo(userDetailsPage)
        .setUserDetails("Joe", "Cancel")
        .cancel()

    accountDetailsPage.isAt()
    assertThat(homePage.getCurrentName()).isEqualTo(currentName)
  }

  @Test
  fun `Errors are displayed to the user`() {
    goTo(loginPage).loginAs("AUTH_ADM")

    goTo(userDetailsPage).submitUserDetails("Jo", "     ")
        .checkError("Enter your last name")
        .checkUserDetails("Jo", "     ")
        .submitUserDetails("Jo", "NewUserNameThatisExactly FiftyOneCharactersInLength")
        .checkError("Your last name must be 50 characters or less")
        .submitUserDetails("Jo", "<script>alert('hello')</script>")
        .checkError("Your last name cannot contain < or > characters")
  }
}

@PageUrl("/user-details")
open class UserDetailsPage : AuthPage<UserDetailsPage>("HMPPS Digital Services - Update Personal Details", "Update personal details") {
  @FindBy(css = "input[type='submit']")
  private lateinit var saveButton: FluentWebElement

  @FindBy(css = "input[name='firstName']")
  private lateinit var firstName: FluentWebElement

  @FindBy(css = "input[name='lastName']")
  private lateinit var lastName: FluentWebElement

  fun checkUserDetails(firstName: String, lastName: String): UserDetailsPage {
    assertThat(this.firstName.value()).isEqualTo(firstName)
    assertThat(this.lastName.value()).isEqualTo(lastName)
    return this
  }

  fun setUserDetails(firstName: String, lastName: String): UserDetailsPage {
    this.firstName.fill().withText(firstName)
    this.lastName.fill().withText(lastName)
    assertThat(saveButton.value()).isEqualTo("Save")
    return this
  }

  fun submitUserDetails(firstName: String, lastName: String): UserDetailsPage {
    setUserDetails(firstName, lastName)
    saveButton.click()
    return this
  }

  fun checkCurrentName(name: String): UserDetailsPage {
    assertThat("${firstName.value()} ${lastName.value()}").isEqualTo(name)
    return this
  }

  fun cancel() {
    val cancelButton = el("a[id='cancel']")
    assertThat(cancelButton.text()).isEqualTo("Cancel")
    cancelButton.click()
  }
}
