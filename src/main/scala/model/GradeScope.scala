package model

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import sttp.client3._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern
import scala.util.Try
import scala.util.matching.Regex
import io.circe.parser.decode
import Util.SubmissionItem
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import cats.data.EitherT

case class GradeScopeSubmission(
    name: String,
    score: String,
    time: LocalDateTime,
    link: String
)

case class GradeScope(
    courseId: String,
    questionId: String,
    assignmentId: String
):
  val cookies =
    "_ga=GA1.2.1949017578.1694029043; __stripe_mid=2c43359b-c434-4392-8517-d916f0655d2ae88050; signed_token=bTlZOTVkYWVWZzBkMnlaNGlaa2xpVnVIZzdudHJYNitsUk1GMjJyTmdOcz0tLTErMlVZMkFpRlQwVTBhbHllTithU1E9PQ%3D%3D--cc0286c8d7934337043a98059095b5b78b06666e; remember_me=c1g5WFJ2NjZWRXhDZFVSbmpUR1pUUT09LS10T3QxVXg1Wk9aeEE2VUdZTUlacVVnPT0%3D--33e46c3a130448bc6399b8c04817d2a6bdedc8b3; apt.uid=AP-1BQVLBSZC216-2-1697431162899-44621736.0.2.42316860-d368-469b-a854-4c575f87a1c7; apt.sid=AP-1BQVLBSZC216-2-1698089600896-81673667; __stripe_sid=00120863-c18c-494e-9719-5e873e139c030e1ad7; _gradescope_session=WVlzT1JNMVZRWi96eWJBaWhzT2VDcm5RTmdCQ0hkV3VVOVFGZ1JZeU9OVXh0R1JUMkVVNS94dVowQVZ4TEtDWEpaY0JldnovaHZTMFZBR2xFMHhSa0hKcFJtUVlwSW81SkNWZFliWFdaS1VCY0lOK0s0SFFXYm9TV3hFMGRHOGN1cnFHN00wb0pIRHNnUDV0Q0RRSHNwcFpaVWp0cTR5YXIvbS9NRUNDNFRnM3R2QVdERFQ3aGVFUEJYTC9tR2VYdWczZkhrVW1Mbkl2RUhyNUtybjk1b0JjbFpiTkx0cGJxQXZ3V1dMeCthVXE2Vkd4blB2ZFpsY0ZWc3FiRUE4K1B3YnlVd0YrRUdXaktTeHhWWGtQZm5MeDUyRktITm82TXlIZDV3di9MVElxTW1janhXOUIyYzFsdlJhejhVZUZKUlRGTGN2T0pYTEd4WHZtU29VZXJJZzBYVnhZK3l0N01uRWxpK3d2UnhVeCtlS2U1T29kVk4za0xXeC81c2ZjNU52bTF2NWFaa3lXTVZ3KzliYjVzZDN3NzhhT1BuSmpMc0IvWEtUTklvTTJQd2g5NnNFR1kvTC92d2tvUlNJUDdUNHdqOWl4TmVaSTVBOHQ0dWRnd3Q1VEI2ODNaYys3RmoveG9NOWRHWi9ra0lBaS8yOFlkR0R4cWczc3QwZCs5dHk1VE1UZjRrN3M5WkhzWFhXalpwZXpEYkE3VVBxZnloT3dWVGgxQVRGdUZDR1hYQnMzNFNFWFZrQnpiMTBXSmxTd1dSZE1zWkxCbjd0Rm9wY2cxUT09LS1JRldRcXBiT0UrZGFzcGlpYUhxZGNBPT0%3D--14cb4faa35e14deff811d98678155c60745d1924"

  def getManualGradingList: EitherT[Future, String, List[String]] = {
    import scala.jdk.CollectionConverters._
    import java.net.URLDecoder

    val url =
      s"https://www.gradescope.com/courses/${courseId}/questions/${questionId}/submissions"
    val headers = Map(
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
      "Accept-Language" -> "zh-CN,zh;q=0.9",
      "Cache-Control" -> "max-age=0",
      "Connection" -> "keep-alive",
      "Host" -> "www.gradescope.com",
      "Sec-Ch-Ua" -> "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"",
      "Sec-Ch-Ua-Mobile" -> "?0",
      "Sec-Ch-Ua-Platform" -> "\"macOS\"",
      "Sec-Fetch-Dest" -> "document",
      "Sec-Fetch-Mode" -> "navigate",
      "Sec-Fetch-Site" -> "same-origin",
      "Sec-Fetch-User" -> "?1",
      "Upgrade-Insecure-Requests" -> "1",
      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
      "Cookie" -> cookies
    )

    val sttpBackend = AsyncHttpClientFutureBackend()

    val raw = basicRequest
      .headers(headers)
      .get(uri"$url")
      .send(sttpBackend)
      .map { resp =>
        resp.body
          .flatMap { content =>
            val htmlDoc = Jsoup.parse(content)
            htmlDoc.select("table#question_submissions").first() match
              case null => Left("Table is null")
              case table =>
                val qualifiedRows = table
                  .select("tr")
                  .asScala
                  .filter { row =>
                    val tds = row.select("td").asScala
                    tds.size > 3 && tds(2).text() != "" && tds(2).text() != null
                  }
                  .map { row =>
                    val e = row.select("td").asScala(1).select("a").first()
                    val link = e.text()
                    val namePattern: Regex = """^([\w\s'-]+)\s+\(.+""".r
                    link match {
                      case namePattern(name) => name
                      case _ =>
                        println(link)
                        "???"
                    }
                  }
                  .map { name => name.trim() }
                  .toList
                println(s"qualifiedRows.size = ${qualifiedRows.size}")
                Right(qualifiedRows)
          }
      }
    EitherT(raw)
  }

  import scala.collection.parallel._

  def getScores: EitherT[Future, String, Map[String, GradeScopeSubmission]] = {
    import scala.jdk.CollectionConverters._
    val headers = Map(
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
      "Accept-Language" -> "zh-CN,zh;q=0.9",
      "Cache-Control" -> "max-age=0",
      "Connection" -> "keep-alive",
      "Host" -> "www.gradescope.com",
      "Sec-Ch-Ua" -> "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"",
      "Sec-Ch-Ua-Mobile" -> "?0",
      "Sec-Ch-Ua-Platform" -> "\"macOS\"",
      "Sec-Fetch-Dest" -> "document",
      "Sec-Fetch-Mode" -> "navigate",
      "Sec-Fetch-Site" -> "same-origin",
      "Sec-Fetch-User" -> "?1",
      "Upgrade-Insecure-Requests" -> "1",
      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
      "Cookie" -> cookies
    )

    val datePattern: Regex =
      """(\w{3} \d{1,2}) at\s*(\d{1,2}:\d{2}(?:AM|PM))""".r

    def extractDateTime(input: String): Option[LocalDateTime] = {
      input match {
        case datePattern(dateStr, timeStr) =>
          val currentYear = LocalDateTime.now().getYear
          val completeDateStr = s"$dateStr $currentYear $timeStr"
          Try(
            LocalDateTime.parse(
              completeDateStr,
              DateTimeFormatter.ofPattern("MMM dd yyyy h:mma")
            )
          ).toOption
        case _ =>
          None
      }
    }

    def parseTableRow(trElement: Element): Option[GradeScopeSubmission] = {
      trElement.select("td").asScala.toList match
        case firstTdElement :: _ :: thirdTdElement :: fourthTdElement :: _ =>
          val firstTdContent = firstTdElement.text().trim()
          val thirdTdContent = thirdTdElement.text
          val fourthTdContent = fourthTdElement.text
          val link = firstTdElement.select("a").first().attr("href")
          val time = extractDateTime(thirdTdContent)
          time.map {
            GradeScopeSubmission(firstTdContent, fourthTdContent, _, link)
          }
        case _ => None
    }

    def getStudentsScore(
        html: org.jsoup.nodes.Document
    ): Map[String, GradeScopeSubmission] = {
      val tables = html
        .select("table.table.js-programmingAssignmentSubmissionsTable")
        .asScala
      val submissions = tables.flatMap { table =>
        table.select("tbody tr").asScala.flatMap { tr =>
          parseTableRow(tr)
        }
      }
      println(submissions.size)
      import scala.collection.parallel.immutable.ParMap
      submissions
        .map(submission => submission.name -> submission)
        .toMap
    }

    val url =
      s"https://www.gradescope.com/courses/${courseId}/assignments/${assignmentId}/submissions"

    EitherT {
      basicRequest
        .headers(headers)
        .get(uri"$url")
        .send(AsyncHttpClientFutureBackend())
        .map { resp =>
          resp.body.flatMap { content =>
            val htmlDoc = Jsoup.parse(content)
            Right(getStudentsScore(htmlDoc))
          }
        }
    }
  }

  def getSubmissionItems(
      submissionId: String
  ): EitherT[Future, String, List[SubmissionItem]] =
    import scala.jdk.CollectionConverters._
    import java.net.URLDecoder

    val url =
      s"https://www.gradescope.com/courses/${courseId}/assignments/${assignmentId}/submissions/${submissionId}.json?content=react&only_keys%5B%5D=past_submissions"

    val headers = Map(
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
      "Accept-Language" -> "zh-CN,zh;q=0.9",
      "Cache-Control" -> "max-age=0",
      "Connection" -> "keep-alive",
      "Host" -> "www.gradescope.com",
      "Sec-Ch-Ua" -> "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"",
      "Sec-Ch-Ua-Mobile" -> "?0",
      "Sec-Ch-Ua-Platform" -> "\"macOS\"",
      "Sec-Fetch-Dest" -> "document",
      "Sec-Fetch-Mode" -> "navigate",
      "Sec-Fetch-Site" -> "same-origin",
      "Sec-Fetch-User" -> "?1",
      "Upgrade-Insecure-Requests" -> "1",
      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
      "Cookie" -> cookies,
      "X-Csrf-Token" -> "sGN9mPEIS07414gDw4lcoSsAwRVnP953af4aNQUUYLJZ8KfWf8Muzw9PeTwkQjuivdv2yBWTw08d94cYcwVBcw==",
      "X-Requested-With" -> "XMLHttpRequest"
    )

    EitherT {
      basicRequest
        .headers(headers)
        .get(uri"$url")
        .send(AsyncHttpClientFutureBackend())
        .map { resp =>
          resp.body
            .flatMap(Util.parseSubmissionItems)
        }
    }

  def activateSubmission(submission: SubmissionItem): Either[String, String] =
    val submissionData = Map(
      "authenticity_token" -> "KatZtzvPMLqyzAC%2FG%2B8WqPlxiyhha8pxOw%2B2uLcXT1%2FAOIP5tQRVO0VU8YD8JHGrb6q89RPH10lPBiuVwQZung%3"
    )

    val headers = Map(
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
      "Accept-Language" -> "zh-CN,zh;q=0.9",
      "Cache-Control" -> "max-age=0",
      "Connection" -> "keep-alive",
      "Host" -> "www.gradescope.com",
      "Sec-Ch-Ua" -> "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"",
      "Sec-Ch-Ua-Mobile" -> "?0",
      "Sec-Ch-Ua-Platform" -> "\"macOS\"",
      "Sec-Fetch-Dest" -> "document",
      "Sec-Fetch-Mode" -> "navigate",
      "Sec-Fetch-Site" -> "same-origin",
      "Sec-Fetch-User" -> "?1",
      "Upgrade-Insecure-Requests" -> "1",
      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
      "Cookie" -> cookies
    )

    val url = s"https://www.gradescope.com${submission.activate_path}"

    import scala.jdk.CollectionConverters._

    val doc = Jsoup
      .connect(url)
      .data(submissionData.asJava)
      .headers(headers.asJava)
      .post()

    val resp = doc.connection().response()
    if resp.statusCode / 100 != 2 then
      Left(s"${resp.statusCode()} ${resp.statusMessage()}")
    else Right("")
