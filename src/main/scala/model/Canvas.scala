package model

import org.jsoup.Jsoup
import sttp.client3.*
import sttp.model.*

import java.time.*
import java.time.format.DateTimeFormatterBuilder
import java.util.regex.Pattern
import scala.util.{Failure, Success, Try}
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global
import cats.data.EitherT
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend

enum SubmissionStatus:
  case SubmissionSuccess
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

case class Canvas(courseId: String, quizId: String, questionId: String):
  val authenticityToken =
    "W8JHKnU4bm5Qy0BZbS8sp6NlhJB4d9rPUVjYSRXStJEsmjBOEEAnDAC8cm08ABvK9Q3KxE0mvLoUEp5wIZz81Q=="
  val cookies =
    "nmstat=cfe8174b-a965-3f84-51e6-d1a7fec5e7df; _ga_QHQ86LM5JZ=GS1.1.1683606973.1.0.1683607323.0.0.0; _ga_PDQN1TTJB7=GS1.1.1692427937.1.1.1692428005.0.0.0; _gcl_au=1.1.1631128518.1693194443; fpestid=g9yE_eKrVlJxe2AG-qGYG1k90KmNujN4fV2n2O4kcjCQIqXCjaVSRbRdqUOwmt09vYbU4w; _fbp=fb.1.1693318838854.417857518; _ga_NSB0YFP7QY=GS1.1.1693435664.1.0.1693435669.0.0.0; _ga_HJYMJN3PH0=GS1.1.1693498538.6.0.1693498540.58.0.0; cebs=1; _cc_id=47b965b0e15928d903a1a7c765c86d12; _ga_EJ5CY9NB8S=GS1.1.1694806892.6.0.1694806900.52.0.0; _ga_RJSVG73M20=GS1.1.1695784075.1.1.1695784096.0.0.0; _ga_07TCNE98Y0=GS1.1.1695850076.6.0.1695850076.0.0.0; _ga_WVEW83W2Q4=GS1.1.1696638363.1.1.1696638458.60.0.0; _ga_T6P0NVFNJD=GS1.1.1697037726.1.0.1697037732.0.0.0; _ga_6Q7PSKX96G=GS1.2.1697037742.1.0.1697037742.0.0.0; _ga=GA1.1.1380335681.1682352774; log_session_id=204fe43da98f3d85aee67d33f9a4d29c; cebsp_=27; _ga_BR8TQSBJZ3=GS1.1.1698156567.37.0.1698156570.0.0.0; _ce.s=v~c0455fa2e8cb2a021b4e405b100ca87d749d8e2c~vpv~1~v11.rlc~1693315894729~ir~1~lcw~1698156568655~v11.fhb~1698156568649~v11.lhb~1698156568649~gtrk.la~llz7o9v1~v11.cs~205175~v11.s~f1faffd0-7276-11ee-aa6f-f1bc9eb33e8c~v11.sla~1698156570262~lcw~1698156570262; _hp2_id.3001039959=%7B%22userId%22%3A%223553615765878325%22%2C%22pageviewId%22%3A%224458384553211521%22%2C%22sessionId%22%3A%226378249562731792%22%2C%22identity%22%3Anull%2C%22trackerVersion%22%3A%224.0%22%7D; _csrf_token=mziQAHzt%2B5lninpe9kBwMQTXDtVrWlU9v%2B%2BGb8OBubjsYOdkGZWy%2Bzf9SGqnb0dcUr9AgV4LM0j6pcBW98%2Fx%2FA%3D%3D; _legacy_normandy_session=gxVBl5lpZp6Acp7G8-CLtg+cCrIeaa5RGBCBoJtVJ_jedVYTCzAdjFDGbXYyA9p2l1kOJKeSo7I4Bsv2SbMb4DpV1lEvCrAobAEIq9poONynXhwLR9YSNvNniPh5sy9gsR3qbVNsuxJfPCiS8L_UhY-C1kGKmLcr1uoxrqlfux8Fw-9FNtbCHqLkmioWNIWB519mDAYjB69AtTcg3ZsciimNMAv-UH4pN6yjvv9TgLffOTdPSxEbrSoV_sFVYAUh64bp1848pn1sKpSv52MWTeUygf_gV6uH7tv_bqNuuTwLJTSXnJE_E9VnDXvzFxD6jd9o6n9IG0Xegm3zF4Wo3kXxadNRQiVz0DwMEs57SyltATgWCowcztCRBNDJ6T1RMvXFxGYrgfOXs4g5-uzSsmCYsVJLwF4QKJnIiJftUniYk6aG-vf1_pFvXqGG_nXVgwD1TpuICjd1CeH9tncd_vyFuOs4LBUzV-Bq9Pa3WI8Ew1z2jfHco5b-1Ugt5AADLDIebuHGo-aP1hkCPBl1G2hAxv5j20sadpoEJ8s8WfaSZITD6c9tPiIPSl97OSPz5fFzwgfAMpjq7_fqWfQfmfQRp4m7wWdCKOVIpdujvkjMtP93ZJcSSmgDkjgRSnvr1hUl0VLsPqYNmYrg_TSSo_wqwD80mSa-ePeqdbs6KQwUtxTTktEY-h7kGAxfEXVF-Dtn7GHcyMp3k2M2fFHi3SNHsLwBuNRvuX162_OhBGQrQu79foHc-WKnCnCIohaFiWNywEKb-LJLNhhv87g8UDh.p2vCZk3Ej6J4h0f0GI1HSxT81OY.ZTm_4g; canvas_session=gxVBl5lpZp6Acp7G8-CLtg+cCrIeaa5RGBCBoJtVJ_jedVYTCzAdjFDGbXYyA9p2l1kOJKeSo7I4Bsv2SbMb4DpV1lEvCrAobAEIq9poONynXhwLR9YSNvNniPh5sy9gsR3qbVNsuxJfPCiS8L_UhY-C1kGKmLcr1uoxrqlfux8Fw-9FNtbCHqLkmioWNIWB519mDAYjB69AtTcg3ZsciimNMAv-UH4pN6yjvv9TgLffOTdPSxEbrSoV_sFVYAUh64bp1848pn1sKpSv52MWTeUygf_gV6uH7tv_bqNuuTwLJTSXnJE_E9VnDXvzFxD6jd9o6n9IG0Xegm3zF4Wo3kXxadNRQiVz0DwMEs57SyltATgWCowcztCRBNDJ6T1RMvXFxGYrgfOXs4g5-uzSsmCYsVJLwF4QKJnIiJftUniYk6aG-vf1_pFvXqGG_nXVgwD1TpuICjd1CeH9tncd_vyFuOs4LBUzV-Bq9Pa3WI8Ew1z2jfHco5b-1Ugt5AADLDIebuHGo-aP1hkCPBl1G2hAxv5j20sadpoEJ8s8WfaSZITD6c9tPiIPSl97OSPz5fFzwgfAMpjq7_fqWfQfmfQRp4m7wWdCKOVIpdujvkjMtP93ZJcSSmgDkjgRSnvr1hUl0VLsPqYNmYrg_TSSo_wqwD80mSa-ePeqdbs6KQwUtxTTktEY-h7kGAxfEXVF-Dtn7GHcyMp3k2M2fFHi3SNHsLwBuNRvuX162_OhBGQrQu79foHc-WKnCnCIohaFiWNywEKb-LJLNhhv87g8UDh.p2vCZk3Ej6J4h0f0GI1HSxT81OY.ZTm_4g"

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
    val submissionData = Map(
      "_method" -> "put",
      "authenticity_token" -> authenticityToken,
      "override_scores" -> "true",
      "hide_student_name" -> "0",
      "submission_version_number" -> "1",
      s"question_score_${questionId}" -> grade,
      s"question_score_${questionId}_visible" -> grade,
      s"question_comment_${questionId}" -> comment,
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
      case Failure(exception) => Left(exception.getMessage())
      case Success(doc) =>
        val resp = doc.connection().response()
        if resp.statusCode / 100 != 2 then
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
      "X-Csrf-Token" -> "Y+Qj+KJF7Huybp/nQnfGg/tSzTWWU6F0z6blMNkVqkpVh3uCkHOCVMQt6q0sHqf1jB+eGu4llz/789B8rnzlPQ=="
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
        .get(uri)
        .headers(headers)
        .get(uri"$uri")
        .send(AsyncHttpClientFutureBackend())
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
      "X-Csrf-Token" -> "Y+Qj+KJF7Huybp/nQnfGg/tSzTWWU6F0z6blMNkVqkpVh3uCkHOCVMQt6q0sHqf1jB+eGu4llz/789B8rnzlPQ=="
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
          .send(AsyncHttpClientFutureBackend())
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
      canvasDuration match {
        case _ if canvasDuration > 120 + 5 =>
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

    getCanvasSubmissionTime(link).value.map {
      case Left(_) =>
        println("what?")
        SubmissionStatus.SubmissionDoesNotSubmitOnCanvas
      case Right(time, duration) => matchTime(gradeScopeTime, time, duration)
    }
