package model

import org.jsoup.Jsoup
import sttp.client3.*
import sttp.model.*

import java.time.*
import java.time.format.DateTimeFormatterBuilder
import java.util.regex.Pattern
import scala.util.{Failure, Random, Success, Try}
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import cats.data.EitherT
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

enum SubmissionStatus:
  case SubmissionSuccess
  case SubmissionFailed(err: String)
  case SubmissionTimeOut(duration: Int)
  case SubmissionDoesNotMatch(
      gradeScopeTime: LocalDateTime,
      canvasTime: LocalDateTime
  )
  case SubmissionDoesNotSubmitOnCanvas
  case SubmissionMultipleTimes(
      gradeScopeTime: LocalDateTime,
      canvasTime: LocalDateTime
  )

case class CanvasSubmission(name: String, link: String)

case class Canvas(
    courseId: String,
    quizId: String,
    questionId: String,
    cookies: String,
    token: String,
    timeOut: Int
):
  def postQuizGrade(
      link: String,
      grade: String,
      comment: String = ""
  ): Either[String, String] = {
    val pattern = """.*/(\d+)/quizzes/(\d+)/history\?quiz_submission_id=(\d+)"""
    val matchResult = pattern.r.findFirstMatchIn(link).get

    val submissionId = matchResult.group(3)
    val url =
      s"https://canvas.its.virginia.edu/courses/$courseId/quizzes/$quizId/submissions/$submissionId"

    val dummyQuestionId = "2196510"
    val submissionData = Map(
      "_method" -> "put",
      "authenticity_token" -> token,
      "override_scores" -> "true",
      "hide_student_name" -> "0",
      "submission_version_number" -> "1",
      s"question_score_${questionId}" -> grade,
      s"question_score_${questionId}_visible" -> grade,
      s"question_comment_${questionId}" -> comment,
      s"question_score_${dummyQuestionId}" -> "1",
      s"question_score_$dummyQuestionId}_visible" -> "1",
      s"question_comment_${dummyQuestionId}" -> "",
      "fudge_points" -> "0.0"
    )

    val headers = Map(
      "Path" -> link,
      "Authority" -> "canvas.its.virginia.edu",
      "Accept" -> "application/json, text/javascript, application/json+canvas-string-ids, */*; q=0.01",
      "Origin" -> "https://canvas.its.virginia.edu",
      "Sec-Ch-Ua" -> "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"",
      "Sec-Ch-Ua-Mobile" -> "?0",
      "Sec-Ch-Ua-Platform" -> "\"macOS\"",
      "Sec-Fetch-Dest" -> "empty",
      "Sec-Fetch-Mode" -> "cors",
      "Sec-Fetch-Site" -> "same-origin",
      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
      "Cookie" -> cookies,
      "Referer" -> url
    )
    import scala.jdk.CollectionConverters._

    def post = Jsoup
      .connect(url)
      .data(submissionData.asJava)
      .headers(headers.asJava)
      .post()

    Try(post) match
      case Failure(exception) => Left(exception.getMessage)
      case Success(doc) =>
        val resp = doc.connection().response()
        if resp.statusCode / 100 != 2 then
          println(s"${resp.statusMessage()} - ${resp.statusCode()}")
          Left(s"${resp.statusCode()} ${resp.statusMessage()}")
        else Right("")
  }

  def getStudentSubmission()
      : EitherT[Future, String, Map[String, CanvasSubmission]] = {
    import scala.jdk.CollectionConverters._

    val headers = Map(
      "Authority" -> "canvas.its.virginia.edu",
      "Method" -> "GET",
      "Path" -> s"/courses/72193/quizzes/${quizId}/managed_quiz_data",
      "Scheme" -> "https",
      "Referer" -> s"https://canvas.its.virginia.edu/courses/72193/quizzes/${quizId}",
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
      "Accept-Language" -> "zh-CN,zh;q=0.9",
      "Cookie" -> cookies,
      "Sec-Ch-Ua" -> "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"",
      "Sec-Ch-Ua-Mobile" -> "?0",
      "Sec-Ch-Ua-Platform" -> "\"macOS\"",
      "Sec-Fetch-Dest" -> "empty",
      "Sec-Fetch-Mode" -> "cors",
      "Sec-Fetch-Site" -> "same-origin",
      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
      "X-Csrf-Token" -> token
    )

    val uri =
      uri"https://canvas.its.virginia.edu/courses/${courseId}/quizzes/${quizId}/managed_quiz_data"

    def retryUntilSuccess[A](action: => A, maxAttempts: Int): A = {
      @annotation.tailrec
      def retry(currentAttempt: Int): A = {
        if (currentAttempt >= maxAttempts) {
          throw new RuntimeException("Exceeded maximum retry attempts")
        } else {
          Try(action) match {
            case Success(value) => value
            case Failure(_)     => retry(currentAttempt + 1)
          }
        }
      }

      retry(0)
    }

    EitherT {
      basicRequest
        .get { uri }
        .headers { headers }
        .get { uri"$uri" }
        .send { AsyncHttpClientFutureBackend() }
        .map {
          _.body
            .map {
              Jsoup
                .parse(_)
                .select("#quiz_details a") match {
                case links if links.isEmpty =>
                  Map.empty[String, CanvasSubmission]
                case links =>
                  links.asScala.map { link =>
                    val href = link.attr("href")
                    val text = link.text.trim
                    text -> CanvasSubmission(text, href)
                  }.toMap
              }
            }
        }
    }
  }

  private val asyncBackend = AsyncHttpClientFutureBackend()

  def getSubmissionStatus(
      link: String,
      gradeScopeTime: LocalDateTime
  ): Future[SubmissionStatus] =
    val headers = Map(
      "Authority" -> "canvas.its.virginia.edu",
      "Method" -> "GET",
      "Path" -> s"/courses/72193/quizzes/${quizId}/managed_quiz_data",
      "Scheme" -> "https",
      "Referer" -> s"https://canvas.its.virginia.edu/courses/72193/quizzes/${quizId}",
      "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
      "Accept-Language" -> "zh-CN,zh;q=0.9",
      "Cookie" -> cookies,
      "Sec-Ch-Ua" -> "\"Google Chrome\";v=\"117\", \"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"117\"",
      "Sec-Ch-Ua-Mobile" -> "?0",
      "Sec-Ch-Ua-Platform" -> "\"macOS\"",
      "Sec-Fetch-Dest" -> "document",
      "Sec-Fetch-Mode" -> "navigate",
      "Sec-Fetch-Site" -> "none",
      "User-Agent" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36",
      "X-Csrf-Token" -> token
    )

    def extractNumber(input: String): Option[Int] = {
      val pattern = Pattern.compile("""(\d+)""")
      val matcher = pattern.matcher(input)
      if (matcher.find()) {
        Try(matcher.group(1).toInt) match {
          case Success(number) => Some(number)
          case Failure(_) =>
            println(s"Cannot parse ${matcher.group(1)} to an integer")
            None
        }
      } else {
        println(s"Cannot match $input with a number")
        None
      }
    }

    def extractDateTime(input: String): Option[LocalDateTime] = {
      val pattern =
        Pattern.compile("""(\w{3} \d{1,2}) at (\d{1,2}(:\d{2})?(?:am|pm))""")
      val matcher = pattern.matcher(input)
      if (matcher.find()) {
        val dateStr = matcher.group(1)
        val timeStrRaw = matcher.group(2)
        val timeStr =
          if (timeStrRaw.contains(":")) timeStrRaw
          else {
            val first = timeStrRaw.substring(0, timeStrRaw.length - 2)
            val second = timeStrRaw.substring(timeStrRaw.length - 2)
            s"$first:00$second"
          }
        val currentYear = LocalDateTime.now.getYear
        val completeDateStr = s"$dateStr $currentYear $timeStr"
        Try(
          LocalDateTime.parse(
            completeDateStr,
            new DateTimeFormatterBuilder()
              .parseCaseInsensitive()
              .appendPattern("MMM d yyyy h:mma")
              .toFormatter
          )
        ) match {
          case Success(dateTime) => Some(dateTime)
          case Failure(_) =>
            println(s"$completeDateStr is not a valid date time string")
            None
        }
      } else {
        println(s"Date time invalid: $input")
        None
      }
    }


    def getCanvasSubmissionTime(
        link: String
    ): EitherT[Future, String, (LocalDateTime, Int)] = {
      val url: String = s"https://canvas.its.virginia.edu${link}"
      EitherT {
        basicRequest
          .get(uri"${url}")
          .headers(headers)
          .send(asyncBackend)
          .map {
            _.body
              .flatMap { content =>
                val htmlDoc = Jsoup.parse(content)
                val timeDiv = htmlDoc.select("div:contains(Submitted)").first()
                val durationDiv = htmlDoc.select("div.quiz_duration").first()

                if (timeDiv == null || durationDiv == null) {
                  println("Could not find quiz div")
                  Left("Could not find quiz div")
                } else {
                  val timeText = timeDiv.text()
                  val durationText = durationDiv.text()

                  (
                    extractDateTime(timeText),
                    extractNumber(durationText)
                  ) match {
                    case (Some(time), Some(duration)) => Right((time, duration))
                    case _ => Left(s"Parse error: $timeText, $durationText")
                  }
                }
              }
          }

      }
    }

    def matchTime(
        gradeScopeTime: LocalDateTime,
        canvasTime: LocalDateTime,
        canvasDuration: Int
    ): SubmissionStatus =
      println(s"time ${link} ${canvasDuration}")

      canvasDuration match {
        case _ if canvasDuration > timeOut + 5 =>
          SubmissionStatus.SubmissionTimeOut(canvasDuration)
        case _ =>
          val minutesDiff =
            Duration.between(gradeScopeTime, canvasTime).toMinutes
          minutesDiff match {
            case min if min > 200 =>
              SubmissionStatus.SubmissionMultipleTimes(
                gradeScopeTime,
                canvasTime
              )
            case min if min < -10 =>
              SubmissionStatus.SubmissionDoesNotMatch(
                gradeScopeTime,
                canvasTime
              )
            case _ => SubmissionStatus.SubmissionSuccess
          }
      }
    val random = new Random
    val randomMilliseconds = 200 + random.nextInt(300) // Generates a random number between 200 and 1999

    Thread.sleep(randomMilliseconds)

    getCanvasSubmissionTime(link).value.map {
      case Left(err) =>
        println(s"what? ${err}")
        SubmissionStatus.SubmissionDoesNotSubmitOnCanvas
      case Right(time, duration) => matchTime(gradeScopeTime, time, duration)
    }
