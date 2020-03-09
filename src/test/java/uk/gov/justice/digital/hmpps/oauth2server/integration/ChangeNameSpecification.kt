package uk.gov.justice.digital.hmpps.oauth2server.integration

import org.assertj.core.api.Assertions.assertThat
import org.fluentlenium.core.annotation.Page
import org.fluentlenium.core.annotation.PageUrl
import org.fluentlenium.core.domain.FluentWebElement
import org.junit.jupiter.api.Test
import org.openqa.selenium.support.FindBy

class ChangeNameSpecification : AbstractAuthSpecification() {
  @Page
  private lateinit var changeNamePage: ChangeNamePage

  @Page
  private lateinit var accountDetailsPage: AccountDetailsPage

  @Test
  fun `A user can change their user details`() {
    val homePage = goTo(loginPage).loginAs("AUTH_ADM")
    val currentName = homePage.getCurrentName()

    goTo(changeNamePage)
        .checkCurrentName(currentName)
        .submitUserDetails("   Joe  ", "  New Name  ")

    accountDetailsPage.isAt()
    assertThat(homePage.getCurrentName()).isEqualTo("Joe New Name")

    goTo(changeNamePage)
        .checkCurrentName("Joe New Name")
  }

  @Test
  fun `Errors are displayed to the user`() {
    goTo(loginPage).loginAs("AUTH_ADM")

    goTo(changeNamePage).submitUserDetails("Jo", "     ")
        .checkError("Enter your last name")
        .checkUserDetails("Jo", "     ")
        .submitUserDetails("Jo", "NewUserNameThatisExactly FiftyOneCharactersInLength")
        .checkError("Your last name must be 50 characters or less")
        .submitUserDetails("Jo", "<script>alert('hello')</script>")
        .checkError("Your last name cannot contain < or > characters")
  }
}

@PageUrl("/change-name")
open class ChangeNamePage : AuthPage<ChangeNamePage>("HMPPS Digital Services - Update Personal Details", "Update personal details") {
  @FindBy(css = "input[type='submit']")
  private lateinit var saveButton: FluentWebElement

  @FindBy(css = "input[name='firstName']")
  private lateinit var firstName: FluentWebElement

  @FindBy(css = "input[name='lastName']")
  private lateinit var lastName: FluentWebElement

  fun checkUserDetails(firstName: String, lastName: String): ChangeNamePage {
    assertThat(this.firstName.value()).isEqualTo(firstName)
    assertThat(this.lastName.value()).isEqualTo(lastName)
    return this
  }

  fun setUserDetails(firstName: String, lastName: String): ChangeNamePage {
    this.firstName.fill().withText(firstName)
    this.lastName.fill().withText(lastName)
    assertThat(saveButton.value()).isEqualTo("Save")
    return this
  }

  fun submitUserDetails(firstName: String, lastName: String): ChangeNamePage {
    setUserDetails(firstName, lastName)
    saveButton.click()
    return this
  }

  fun checkCurrentName(name: String): ChangeNamePage {
    assertThat("${firstName.value()} ${lastName.value()}").isEqualTo(name)
    return this
  }
}
