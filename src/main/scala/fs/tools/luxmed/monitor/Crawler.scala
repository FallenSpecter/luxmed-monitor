package fs.tools.luxmed.monitor

import java.io.File

import com.google.common.io.Files
import org.openqa.selenium.By.{ByCssSelector, ByXPath}
import org.openqa.selenium._

import scala.collection.JavaConverters._
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.interactions.Actions
import org.slf4j.LoggerFactory

class Crawler(config: Config) {

  private final val Logger = LoggerFactory.getLogger(classOf[Crawler])

  private var lastSelectedDoctorIndex = 0

  private var driver = createDriver(config.driver.runInBrowser, config.driver.location)

  def run(): Unit = {
    while (true) {
      try {
        performEndlessSearch()
      } catch {
        case e: Any => println(e)
      }
    }
  }

  private def createDriver(headless: Boolean, location: String): WebDriver = {
    val options = new ChromeOptions
    options.addArguments("window-size=1600x900")
    if (headless) {
      options.addArguments("headless")
    }
    System.setProperty("webdriver.chrome.driver", location)
    new ChromeDriver(options)
  }


  private def performEndlessSearch(): Unit = {
    performAuthentication()
    fillInSearchForm()

    while (true) {
      Thread.sleep(5000)
      selectDoctor(getCurrentDoctor, getNextDoctor)
      Thread.sleep(3000)
      submitSearchForm()
      Thread.sleep(3000)
      closePopup()

      if (anyFreeSlot(config.search.timeFrom, config.search.timeTo)) {
        onMatchingSlotFound()
      }

      sleepForAMoment()
    }
  }

  private def performAuthentication(): Unit = {
    openPage()
    logIn(config.credentials.username, config.credentials.password)
    Thread.sleep(5000)
  }

  private def openPage(): Unit = {
    Logger.info("Entering webpage")
    driver.get("https://portalpacjenta.luxmed.pl/PatientPortal/Reservations/Reservation/Find?firstTry=True")
    require(driver.getTitle.contains("LUX MED"))
    driver.screenshot("open_page")
    Logger.info("Lux med webpage opened")
  }

  private def logIn(login: String, password: String): Unit = {
    Logger.info(s"Logging in user with login $login")
    val inputLogin = driver.findElement(new ByCssSelector("form#loginForm input#Login"))
    inputLogin.clear()
    inputLogin.sendKeys(login)
    inputLogin.sendKeys(Keys.TAB)
    val inputPass = driver.findElement(new ByCssSelector("form#loginForm input#Password"))
    inputPass.sendKeys(password)
    val inputSubmit = driver.findElement(new ByCssSelector("form#loginForm input[type=submit]"))
    inputSubmit.click()
    Logger.info(s"User '$login' logged in ")
  }

  private def fillInSearchForm(): Unit = {
    selectServiceGroup(config.search.serviceGroup)
    Thread.sleep(5000)
    selectAppointmentButton()
    Thread.sleep(5000)
    selectService(config.search.service)
    Thread.sleep(2000)
    selectLocation(config.search.location)
    Thread.sleep(2000)
    selectDates(config.search.dateFrom, config.search.dateTo)
    Thread.sleep(2000)
  }

  private def selectServiceGroup(serviceGroup: String): Unit = {
    driver.screenshot("select_service_group")
    Logger.info(s"Selecting service group '$serviceGroup'")
    driver.findElement(new ByCssSelector(s"a[datasubcategory *= '$serviceGroup']")).click()
    Logger.info(s"Service group '$serviceGroup' successfully selected")
  }

  private def selectAppointmentButton(): Unit = {
    try {
      Logger.info("Pressing 'appointment' button")
      driver.screenshot("select_appointment_button")
      driver.findElement(new ByXPath("//a[contains(@class, 'activity_button')][contains(text(),'Wizyta')]")).click()
    } catch {
      case _: NoSuchElementException => Logger.warn("Appointment page not available")
    }
  }

  private def selectService(serviceName: String): Unit = {
    if (serviceName == null || serviceName.length == 0) {
      return
    }
    Logger.info(s"Selecting service: '$serviceName'")
    selectValueInDropdown(2, 0, serviceName)
    driver.screenshot("select_service")
  }

  private def selectValueInDropdown(columnIndex: Int, selectorIndex: Int, valueToSelect: String): Unit = {
    val dropdownItem = fetchItemFromDropdown(columnIndex, selectorIndex, valueToSelect)
    dropdownItem.click()
    closeDropdown()
    Thread.sleep(3000)
  }

  private def fetchItemFromDropdown(columnIndex: Int, selectorIndex: Int, itemValue: String): WebElement = {
    clickOnDropdown(columnIndex, selectorIndex)
    val dropdownSearch = driver.findElement(new ByCssSelector("input.search-select"))
    dropdownSearch.clear()
    dropdownSearch.sendKeys(itemValue)
    val dropdownItem = driver.findElement(new ByCssSelector("ul#__selectOptions li:not(.hidden)"))
    dropdownItem
  }

  private def clickOnDropdown(columnIndex: Int, selectorIndex: Int): Unit = {
    val cssPath = s"form#advancedResevation div.column$columnIndex div.graphicSelectContainer"
    val dropdown = driver.findElements(new ByCssSelector(cssPath)).get(selectorIndex)
    dropdown.click()
  }

  private def closeDropdown(): Unit = {
    // There is an invisible overlay which has to be destroyed by clicking on any clickable item underneath
    val actions = new Actions(driver)
    val body = driver.findElement(new ByCssSelector("a.logo"))
    actions.moveToElement(body).click().perform()
  }

  private def selectLocation(location: String): Unit = {
    if (location == null || location.length == 0) {
      return
    }

    Logger.info(s"Selecting location: '$location'")
    selectValueInDropdown(1, 1, location)
    driver.screenshot("select_location")
  }

  private def selectDates(startDate: String, stopDate: String): Unit = {
    Logger.info(s"Selecting dates.From $startDate, to $stopDate")
    val timePickerInput = driver.findElement(new ByCssSelector("#rangePicker"))
    timePickerInput.clear()
    timePickerInput.sendKeys(startDate + "  |  " + stopDate)
    driver.findElement(new ByCssSelector("body")).click()
    driver.findElement(new ByCssSelector("body")).click()
    driver.screenshot("select_dates")
  }

  private def selectDoctor(currentDoctorName: String, nextDoctorName: String): Unit = {
    if (nextDoctorName.isEmpty) {
      return
    }

    Logger.info(s"Unselecting doctor name: '$currentDoctorName'")
    unselectValueInDropdown(2, 1, currentDoctorName)
    Logger.info(s"Selecting doctor name: '$nextDoctorName'")
    selectValueInDropdown(2, 1, nextDoctorName)
    driver.screenshot("select_doctor")
  }

  private def unselectValueInDropdown(columnIndex: Int, selectorIndex: Int, valueToUnselect: String): Unit = {
    val dropdownItem = fetchItemFromDropdown(columnIndex, selectorIndex, valueToUnselect)
    try {
      // checking if checkbox is checked - sooo ugly, will refactor... I promise !
      dropdownItem.findElement(new ByCssSelector("input[type='checkbox']:checked"))
      dropdownItem.click()
    } catch {
      case _: NoSuchElementException =>
    }
    closeDropdown()
    Thread.sleep(3000)
  }

  private def submitSearchForm(): Unit = {
    Logger.info("Performing search")
    driver.screenshot("submit_search_form")
    val submitButton = driver.findElement(new ByCssSelector("input[type=submit]"))
    submitButton.click()
  }

  private def closePopup(): Unit = {
    try {
      driver.screenshot("close_popup")
      driver.findElement(new ByCssSelector("div#__popup button.reject")).click()
      Logger.info("Closing popup")
    } catch {
      case _: NoSuchElementException => Logger.info("Popup not found")
    }
  }

  private def anyFreeSlot(timeFrom: Int, timeTo: Int): Boolean = {
    val slotsElements = driver.findElements(new ByCssSelector(".reserveTable tbody tr"))
    Logger.info(s"Number of all slots: ${slotsElements.size()}")
    driver.screenshot("any_free_slot")

    Logger.info(s"Applying time filters: from $timeFrom to $timeTo")
    val elements = slotsElements.asScala.filter(e => isSlotBetween(e, timeFrom, timeTo))

    Logger.info(s"Number of matching slots: ${elements.size}")
    elements.nonEmpty
  }

  private def isSlotBetween(slot: WebElement, timeFrom: Int, timeTo: Int): Boolean = {
    val slotHour = getHourFromSlot(slot)
    (timeFrom <= slotHour) && (slotHour <= timeTo)
  }

  private def getHourFromSlot(slot: WebElement): Int = {
    val slotTimeText = slot.findElement(new ByCssSelector("td.hours")).getText
    val hour = slotTimeText.split(':')(0)
    Integer.valueOf(hour).intValue()
  }

  private def onMatchingSlotFound(): Nothing = {
    printSuccessAsciiArt()
    driver.screenshot("free_slots_found")

    // Open browser, log in and search
    val headless = config.driver.runInBrowser
    val openBrowserOnSuccess = config.driver.openBrowserOnSuccessfulSearch
    if (headless && openBrowserOnSuccess) {
      Logger.info("Opening browser")
      driver = createDriver(headless = false, config.driver.location)
      performAuthentication()
      fillInSearchForm()
      submitSearchForm()
    }

    sys.exit(0)
  }

  private def printSuccessAsciiArt(): Unit = {
    println("SUCCESS")
    println("SUCCESS")
    println("SUCCESS")
  }

  private def sleepForAMoment(): Unit = {
    val sleepTime = util.Random.nextInt(12) + 1
    Logger.info(s"About to sleep for $sleepTime seconds")
    Thread.sleep(sleepTime * 1000)
  }

  private def getCurrentDoctor: String = {
    val doctors = config.search.doctors
    if (doctors.isEmpty) {
      ""
    } else {
      doctors(lastSelectedDoctorIndex)
    }
  }

  private def getNextDoctor: String = {
    val doctors = config.search.doctors
    val numberOfDoctors = doctors.size
    if (doctors.isEmpty) {
      ""
    } else {
      lastSelectedDoctorIndex = (lastSelectedDoctorIndex + 1) % numberOfDoctors
      getCurrentDoctor
    }
  }

  private implicit class RichWebDriver(val driver: WebDriver) {
    def screenshot(name: String): Unit = {
      val filepath = s"${config.screenshotPath}/$name.png"
      val file = driver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE)
      Files.copy(file, new File(filepath))
    }
  }

}
