class UtilTest extends munit.FunSuite {
  test("json parse") {
    val json = """{
    "past_submissions": [
        {
            "id": 200570509,
            "created_at": "2023-10-08T10:44:09.798629-07:00",
            "owners": [
                {
                    "id": 4097976,
                    "active": false,
                    "initials": "SC",
                    "name": "Sanjula Chitty"
                }
            ],
            "show_path": "/courses/576725/assignments/3480775/submissions/200570509",
            "active": false,
            "activate_path": "/courses/576725/assignments/3480775/submissions/200570509/activate",
            "can_activate": true,
            "score": "24.0"
        }
    ],
    "alert": null
    }"""
    Util.parseSubmissionItems(json) match {
      case Left(err) => fail(err)
      case Right(items) =>
        assertEquals(1, items.size)
        println("Haha")
    }
  }
}
