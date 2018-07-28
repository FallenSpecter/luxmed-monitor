package fs.tools.luxmed.monitor

case class Config(driver: Driver,
                  credentials: Credentials,
                  search: Search,
                  email: Email,
                  screenshotPath: String)

case class Driver(location: String,
                  runInBrowser: Boolean,
                  openBrowserOnSuccessfulSearch: Boolean)

case class Credentials(username: String,
                       password: String)

case class Search(serviceGroup: String,
                  service: String,
                  doctors: List[String],
                  dateFrom: String,
                  dateTo: String,
                  timeFrom: Int,
                  timeTo: Int,
                  location: String)

case class Email(smtpUsername: String,
                 smtpPassword: String,
                 smtpUrl: String,
                 smtpPort: Option[Int],
                 sender: String,
                 recipient: String)
