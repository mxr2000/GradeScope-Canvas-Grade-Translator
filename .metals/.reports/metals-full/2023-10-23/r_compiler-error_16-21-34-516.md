file://<WORKSPACE>/src/main/scala/Canvas.scala
### file%3A%2F%2F%2FUsers%2Fmxr%2FDocuments%2Fsrc%2Fscala%2FGradeTranslator%2FGradeTranslator%2Fsrc%2Fmain%2Fscala%2FCanvas.scala:10: error: illegal start of definition identifier
enum SubmissionStatus:
^

occurred in the presentation compiler.

action parameters:
uri: file://<WORKSPACE>/src/main/scala/Canvas.scala
text:
```scala
import org.jsoup.Jsoup
import sttp.client3.*
import sttp.model.*

import java.time.*
import java.time.format.DateTimeFormatterBuilder
import java.util.regex.Pattern
import scala.util.{Failure, Success, Try}

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

case class Canvas(courseId: String, quizId: String):
  val authenticityToken =
    "W8JHKnU4bm5Qy0BZbS8sp6NlhJB4d9rPUVjYSRXStJEsmjBOEEAnDAC8cm08ABvK9Q3KxE0mvLoUEp5wIZz81Q=="
  val cookies =
    "nmstat=cfe8174b-a965-3f84-51e6-d1a7fec5e7df; _ga_QHQ86LM5JZ=GS1.1.1683606973.1.0.1683607323.0.0.0; _ga_PDQN1TTJB7=GS1.1.1692427937.1.1.1692428005.0.0.0; _gcl_au=1.1.1631128518.1693194443; fpestid=g9yE_eKrVlJxe2AG-qGYG1k90KmNujN4fV2n2O4kcjCQIqXCjaVSRbRdqUOwmt09vYbU4w; _fbp=fb.1.1693318838854.417857518; _ga_NSB0YFP7QY=GS1.1.1693435664.1.0.1693435669.0.0.0; _ga_HJYMJN3PH0=GS1.1.1693498538.6.0.1693498540.58.0.0; cebs=1; _cc_id=47b965b0e15928d903a1a7c765c86d12; _ga_EJ5CY9NB8S=GS1.1.1694806892.6.0.1694806900.52.0.0; _ga_RJSVG73M20=GS1.1.1695784075.1.1.1695784096.0.0.0; _ga_07TCNE98Y0=GS1.1.1695850076.6.0.1695850076.0.0.0; _ga_WVEW83W2Q4=GS1.1.1696638363.1.1.1696638458.60.0.0; _ga_T6P0NVFNJD=GS1.1.1697037726.1.0.1697037732.0.0.0; _ga_6Q7PSKX96G=GS1.2.1697037742.1.0.1697037742.0.0.0; _ga=GA1.1.1380335681.1682352774; log_session_id=204fe43da98f3d85aee67d33f9a4d29c; _ce.clock_event=1; _ce.clock_data=-13%2C199.111.213.144%2C1%2C84fb6a68ab92a6d30981c69a1117885c; cebsp_=26; _ga_BR8TQSBJZ3=GS1.1.1698021330.36.0.1698021377.0.0.0; _ce.s=v~c0455fa2e8cb2a021b4e405b100ca87d749d8e2c~vpv~1~v11.rlc~1693315894729~ir~1~lcw~1698021378052~v11.fhb~1698021331070~v11.lhb~1698021331070~gtrk.la~llz7o9v1~v11.cs~205175~v11.s~0e5ac120-6fb0-11ee-94f9-7936e010d7ba~v11.sla~1698021378599~v11.send~1698021377884~lcw~1698021378599; _hp2_id.3001039959=%7B%22userId%22%3A%223553615765878325%22%2C%22pageviewId%22%3A%223543960007063772%22%2C%22sessionId%22%3A%228558702319509691%22%2C%22identity%22%3Anull%2C%22trackerVersion%22%3A%224.0%22%7D; _hp2_ses_props.3001039959=%7B%22r%22%3A%22https%3A%2F%2Fcanvas.its.virginia.edu%2Fcourses%2F72193%2Fgradebook%2Fspeed_grader%3Fassignment_id%3D335473%26student_id%3D1205786%22%2C%22ts%22%3A1698089583209%2C%22d%22%3A%22canvas.its.virginia.edu%22%2C%22h%22%3A%22%2Fcourses%2F72193%2Fgradebook%2Fspeed_grader%22%2C%22q%22%3A%22%3Fassignment_id%3D335473%26student_id%3D1205786%22%7D; _legacy_normandy_session=CqHHWkjSPJUamUKwRBqn-Q+p30W_CbIncV-xpPTvfhU_OEinuxVCKCKAk_HIcdmsajiUU6j8gyExkhH6i_63lNXViXLG_tM-yAxUcAO0dtb7gRcjN3hZv4xZfHSLox8pDB-Z1VyrISD-VGv1Sireg0qE6y9Fum2yH7Gygx1GtZvZMru9MqNIzR9OFgFVIbZY7gipgT9ePA7j88mzXO5nohie6B-C5tsaIMQkj92yAZ-8Zc0D0QA3nFqRZ83AeCuezjDPdCKcKDOC9qoGjRjl14jASBYGr7O4i-4DR1FcAm0t-88t7liKzZUME5gAGlo3dhTOF8_y2a7F7hl3BWe_3GXZIhD-Hqc1nXvqsHYiHo2BEm596OeK6LsX0kqENmdE3JEPEsGlgCF80Ndtef_ByDoocveo__TZzlhi311pYwg_yyt-Xnw_JtrPViXTc9Shb7O-3SWeQ2IB_woCbCIUa7KQuxNofV0979ruUuKP1mUQR6faKsp7aZaGCh7hYMd_FZaTH1T5gUFkepdb5NkKLGwVUGD8SSwhQYodx56dq2qQzGunuJDjgrbgMPCTJmyGtDmZvrqWYDpPaRLfPHmzx55DHzgPbx35jQgeUea1DtYzzIY-oKhmdtZnROS4KGQTszkb4byz7kmIIahF4tMOSF4tIAQStvcVlsWAP3yDdF8rHq2dANGwMl2I34FHyKbkRtjzN3TCXND6yjbTy-cFWYaAt0cSlt_bmi78b753MXhXyuZmifqWFByFJtVzdTIPbpWDVSEB_Dwtmy8yLCIl1fq.Ge_QzFTlaCfCA4I74sb8b7vLcG8.ZTbLLQ; canvas_session=CqHHWkjSPJUamUKwRBqn-Q+p30W_CbIncV-xpPTvfhU_OEinuxVCKCKAk_HIcdmsajiUU6j8gyExkhH6i_63lNXViXLG_tM-yAxUcAO0dtb7gRcjN3hZv4xZfHSLox8pDB-Z1VyrISD-VGv1Sireg0qE6y9Fum2yH7Gygx1GtZvZMru9MqNIzR9OFgFVIbZY7gipgT9ePA7j88mzXO5nohie6B-C5tsaIMQkj92yAZ-8Zc0D0QA3nFqRZ83AeCuezjDPdCKcKDOC9qoGjRjl14jASBYGr7O4i-4DR1FcAm0t-88t7liKzZUME5gAGlo3dhTOF8_y2a7F7hl3BWe_3GXZIhD-Hqc1nXvqsHYiHo2BEm596OeK6LsX0kqENmdE3JEPEsGlgCF80Ndtef_ByDoocveo__TZzlhi311pYwg_yyt-Xnw_JtrPViXTc9Shb7O-3SWeQ2IB_woCbCIUa7KQuxNofV0979ruUuKP1mUQR6faKsp7aZaGCh7hYMd_FZaTH1T5gUFkepdb5NkKLGwVUGD8SSwhQYodx56dq2qQzGunuJDjgrbgMPCTJmyGtDmZvrqWYDpPaRLfPHmzx55DHzgPbx35jQgeUea1DtYzzIY-oKhmdtZnROS4KGQTszkb4byz7kmIIahF4tMOSF4tIAQStvcVlsWAP3yDdF8rHq2dANGwMl2I34FHyKbkRtjzN3TCXND6yjbTy-cFWYaAt0cSlt_bmi78b753MXhXyuZmifqWFByFJtVzdTIPbpWDVSEB_Dwtmy8yLCIl1fq.Ge_QzFTlaCfCA4I74sb8b7vLcG8.ZTbLLQ; _csrf_token=ttVTVQf881gYOD%2B2UNXWJRzEugrIynFw%2B8EYLbjFB4nBjSQxYoS6OkhPDYIB%2BuFISqz0Xv2bFwW%2Bi14UjItPzQ%3D%3D"

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
      "question_score_2075376" -> grade,
      "question_score_2075376_visible" -> grade,
      "question_comment_2075376" -> comment,
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

    val doc = Jsoup
      .connect(url)
      .data(submissionData.asJava)
      .headers(headers.asJava)
      .post()

    val resp = doc.connection().response()
    if resp.statusCode / 100 != 2 then
      Left(s"${resp.statusCode()} ${resp.statusMessage()}")
    else Right("")
  }

  def getStudentSubmission(): Either[String, Map[String, CanvasSubmission]] = {
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

    basicRequest
      .get(uri)
      .headers(headers)
      .get(uri"$uri")
      .send(HttpURLConnectionBackend())
      .body
      .map { content =>
        Jsoup
          .parse(content)
          .select("#quiz_details a") match {
          case links if links.isEmpty => Map.empty[String, CanvasSubmission]
          case links =>
            links.asScala.map { link =>
              val href = link.attr("href")
              val text = link.text.trim
              text -> CanvasSubmission(text, href)
            }.toMap
        }
      }
  }

  def getSubmissionStatus(
      link: String,
      gradeScopeTime: LocalDateTime
  ): SubmissionStatus =
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
    ): Either[String, (LocalDateTime, Int)] = {
      val url: String = s"https://canvas.its.virginia.edu${link}"
      basicRequest
        .get(uri"${url}")
        .headers(headers)
        .send(HttpURLConnectionBackend())
        .body
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

            (extractDateTime(timeText), extractNumber(durationText)) match {
              case (Some(time), Some(duration)) => Right((time, duration))
              case _ => Left(s"Parse error: $timeText, $durationText")
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

    getCanvasSubmissionTime(link) match
      case Left(_) => SubmissionStatus.SubmissionDoesNotSubmitOnCanvas
      case Right(time, duration) => matchTime(gradeScopeTime, time, duration)

```



#### Error stacktrace:

```
scala.meta.internal.parsers.Reporter.syntaxError(Reporter.scala:16)
	scala.meta.internal.parsers.Reporter.syntaxError$(Reporter.scala:16)
	scala.meta.internal.parsers.Reporter$$anon$1.syntaxError(Reporter.scala:22)
	scala.meta.internal.parsers.Reporter.syntaxError(Reporter.scala:17)
	scala.meta.internal.parsers.Reporter.syntaxError$(Reporter.scala:17)
	scala.meta.internal.parsers.Reporter$$anon$1.syntaxError(Reporter.scala:22)
	scala.meta.internal.parsers.ScalametaParser.statSeqBuf(ScalametaParser.scala:4464)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$batchSource$13(ScalametaParser.scala:4696)
	scala.Option.getOrElse(Option.scala:189)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$batchSource$1(ScalametaParser.scala:4696)
	scala.meta.internal.parsers.ScalametaParser.atPos(ScalametaParser.scala:319)
	scala.meta.internal.parsers.ScalametaParser.autoPos(ScalametaParser.scala:365)
	scala.meta.internal.parsers.ScalametaParser.batchSource(ScalametaParser.scala:4652)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$source$1(ScalametaParser.scala:4645)
	scala.meta.internal.parsers.ScalametaParser.atPos(ScalametaParser.scala:319)
	scala.meta.internal.parsers.ScalametaParser.autoPos(ScalametaParser.scala:365)
	scala.meta.internal.parsers.ScalametaParser.source(ScalametaParser.scala:4645)
	scala.meta.internal.parsers.ScalametaParser.entrypointSource(ScalametaParser.scala:4650)
	scala.meta.internal.parsers.ScalametaParser.parseSourceImpl(ScalametaParser.scala:135)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$parseSource$1(ScalametaParser.scala:132)
	scala.meta.internal.parsers.ScalametaParser.parseRuleAfterBOF(ScalametaParser.scala:59)
	scala.meta.internal.parsers.ScalametaParser.parseRule(ScalametaParser.scala:54)
	scala.meta.internal.parsers.ScalametaParser.parseSource(ScalametaParser.scala:132)
	scala.meta.parsers.Parse$.$anonfun$parseSource$1(Parse.scala:29)
	scala.meta.parsers.Parse$$anon$1.apply(Parse.scala:36)
	scala.meta.parsers.Api$XtensionParseDialectInput.parse(Api.scala:25)
	scala.meta.internal.semanticdb.scalac.ParseOps$XtensionCompilationUnitSource.toSource(ParseOps.scala:17)
	scala.meta.internal.semanticdb.scalac.TextDocumentOps$XtensionCompilationUnitDocument.toTextDocument(TextDocumentOps.scala:206)
	scala.meta.internal.pc.SemanticdbTextDocumentProvider.textDocument(SemanticdbTextDocumentProvider.scala:54)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$semanticdbTextDocument$1(ScalaPresentationCompiler.scala:356)
```
#### Short summary: 

file%3A%2F%2F%2FUsers%2Fmxr%2FDocuments%2Fsrc%2Fscala%2FGradeTranslator%2FGradeTranslator%2Fsrc%2Fmain%2Fscala%2FCanvas.scala:10: error: illegal start of definition identifier
enum SubmissionStatus:
^