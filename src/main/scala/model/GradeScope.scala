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
    assignmentId: String,
    questionId: String,
    cookies: String,
    token: String
):

  def getManualGradingList: EitherT[Future, String, List[(String, String)]] = {
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
                    val rawName = link match {
                      case namePattern(name) => name
                      case _ =>
                        println(link)
                        "???"
                    }
                    val name = rawName.trim()
                    val score = row.select("td").asScala(4).text()
                    (name, score)
                  }
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
      "X-Csrf-Token" -> token,
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
