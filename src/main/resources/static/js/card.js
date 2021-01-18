window.onload = function () {
  // Loops through dom and finds all elements with card--clickable class
  document.querySelectorAll('.auth-card--clickable').forEach(card => {
    // Check if card has a link within it
    if (card.querySelector('a') !== null) {
      // Clicks the link within the heading to navigate to desired page
      card.addEventListener('click', () => {
        card.querySelector('a').click()
      })
    }
  })
}
