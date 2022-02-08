import core.Valkyrie
import org.scalatest.ParallelTestExecution
import org.scalatest.funsuite.AnyFunSuite

class End2EndTester extends AnyFunSuite with ParallelTestExecution {

  test("An empty List should have size 0") {
    assert(List.empty.isEmpty)
  }

  //  Valkyrie.json.build

  test("Not Null") {
    assert(Valkyrie.json.notNull().build.evaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |    {
        |      "company": "zycus",
        |      "location": "Andheri",
        |      "order": 1,
        |      "date": "08-06-2015"
        |    },
        |    {
        |      "company": "CitiusTech",
        |      "location": "Airoli",
        |      "order": 2,
        |      "date": "19-02-2018"
        |    }
        |  ]
        |}""".stripMargin))
  }

  test("Selective Not Null") {
    assert(Valkyrie.json.notNull("$.work[1]").build.evaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |    {
        |      "company": "zycus",
        |      "location": "Andheri",
        |      "order": 1,
        |      "date": "08-06-2015"
        |    },
        |    {
        |      "company": "CitiusTech",
        |      "location": "Airoli",
        |      "order": 2,
        |      "date": "19-02-2018"
        |    }
        |  ]
        |}""".stripMargin))
  }

  test("Is Null") {
    assert(Valkyrie.json.isNull("$.work[1].date").build.evaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |    {
        |      "company": "zycus",
        |      "location": "Andheri",
        |      "order": 1,
        |      "date": null
        |    },
        |    {
        |      "company": "CitiusTech",
        |      "location": "Airoli",
        |      "order": 2,
        |      "date": null
        |    }
        |  ]
        |}""".stripMargin))
  }

  test("Is Blank") {
    assert(Valkyrie.json.isBlank("$.work[0].date").build.evaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |    {
        |      "company": "zycus",
        |      "location": "Andheri",
        |      "order": 1,
        |      "date": ""
        |    },
        |    {
        |      "company": "CitiusTech",
        |      "location": "Airoli",
        |      "order": 2,
        |      "date": null
        |    }
        |  ]
        |}""".stripMargin))
  }

  test("Is Null for Array") {
    assert(Valkyrie.json
      .isNull("$.work[*]").build.evaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |  ]
        |}""".stripMargin))
  }

  test("Is value equal for Int") {
    assert(Valkyrie.json
      .isEqualTo("6", "$.experience").build.evaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |  ]
        |}""".stripMargin))
  }

  test("Is value equal for String") {
    assert(Valkyrie.json
      .isEqualTo("kartik", "$.name").build.evaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |  ]
        |}""".stripMargin))
  }

  test("Is value not equal for Int") {
    assert(Valkyrie.json
      .isNotEqualTo("5", "$.experience").build.evaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |  ]
        |}""".stripMargin))
  }

  test("Is value not equal for String") {
    assert(Valkyrie.json
      .isNotEqualTo("kitark", "$.name").build.evaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |  ]
        |}""".stripMargin))
  }

 /* test("Deep Evaluate Not Null") {
    // had no null elements in the entire json

    val outcome = Valkyrie.json.notNull().build.traceEvaluate(
      """{
        |  "name": "kartik",
        |  "experience": 6,
        |  "work": [
        |    {
        |      "company": null,
        |      "location": "Andheri",
        |      "order": 1,
        |      "date": "08-06-2015"
        |    },
        |    {
        |      "company": "CitiusTech",
        |      "location": "Airoli",
        |      "order": 2,
        |      "date": "19-02-2018"
        |    }
        |  ]
        |}""".stripMargin)

   /* val outcome = Valkyrie.json.notNull().build.deepEvaluate(
      """{
        |  "name": null
        }""".stripMargin)*/

    assert(!outcome.isValid)
  }*/

  test("Is Null str") {

    /*for (x <- 1 to 10000) {
      (Valkyrie.json
        .notNull().build.evaluate(
        """{
        | "name": "kartik",
        | "experience": 6,
        | "work": [
        |   {
        |     "company": null,
        |     "location": "Andheri",
        |     "order": 1,
        |     "date": "08-06-2015"
        |   },
        |   {
        |     "company": "CitiusTech",
        |     "location": "Airoli",
        |     "order": 2,
        |     "date": "19-02-2018"
        |   }
        | ]
        | }""".stripMargin))
    }*/

    assert(Valkyrie.json
      .notNull().build.evaluate(
      """{
      | "name": "kartik",
      | "experience": 6,
      | "work":
      |   [{
      |     "company": "Zycus",
      |     "location": "Andheri",
      |     "order": 1,
      |     "date": "08-06-2015"
      |   },{
      |     "company": "null",
      |     "location": "Andheri",
      |     "order": 1,
      |     "date": "08-06-2015"
      |   }]
      | }""".stripMargin))
  }
}
