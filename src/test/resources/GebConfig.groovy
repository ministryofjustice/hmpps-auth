import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

atCheckWaiting = true

waiting {
    timeout = 10
}

environments {
    chrome {
        driver = { new ChromeDriver() }
    }

    chromeHeadless {
        driver = {
            ChromeOptions options = new ChromeOptions()
            options.addArguments('headless')
            options.add_argument('disable-extensions')
            options.add_argument('headless')
            options.add_argument('disable-gpu')
            options.add_argument('no-sandbox')
            options.add_argument('disable-application-cache')
            new ChromeDriver(options)
        }
    }
}

// Default if geb.env is not set to one of 'chrome', or 'chromeHeadless'
driver = {
    new ChromeDriver()
}

baseUrl = "http://localhost:8080"

reportsDir = "build/geb-reports"

// uncomment to keep the browser open at the end of the test
// quitCachedDriverOnShutdown = false
