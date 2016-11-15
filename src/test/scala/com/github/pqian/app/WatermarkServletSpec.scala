package com.github.pqian.app

import akka.actor.{ActorSystem, Props}
import org.scalatra.test.specs2._
import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration


// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html
class WatermarkServletSpec extends MutableScalatraSpec {

  val jdbcUrl = "jdbc:h2:mem:chapter10;DB_CLOSE_DELAY=-1"
  val jdbcDriverClass = "org.h2.Driver"
  val db = Database.forURL(jdbcUrl, driver = jdbcDriverClass)

  val store = DocumentStore("store", db)

  val system = ActorSystem()

  val pdfActor = system.actorOf(Props(PdfActor(store, db)))

  val app = new WatermarkServlet(db, store, system, pdfActor)

  val res = db.run(DbSetup.createDatabase)
  Await.result(res, Duration(5, "seconds"))

  addServlet(app, "/*")

  "GET /documents" should {
    "return status 200" in {
      get("/documents") {
        status must_== 200
      }
    }
  }

  "GET /documents/:ticket" should {
    "return status 404" in {
      get("/documents/invalid-ticket") {
        status must_== 404
      }
    }
    "return status 200" in {
      get("/documents/e833a0ca-7dec-4c10-9464-42a2b9990461") {
        status must_== 200
      }
    }
  }

  "GET /documents/:ticket/watermarkedfile" should  {
    "return status 404" in {
      get("/documents/invalid-ticket/watermarkedfile") {
        status must_== 404
      }
    }
    "return status 200" in {
      get("/documents/e833a0ca-7dec-4c10-9464-42a2b9990461/watermarkedfile") {
        status must_== 200
      }
    }
  }

  "POST /documents" should {
    "return status 400" in {
      post("/documents", "{\"content\":\"book\",\"title\":\"Code Complete\",\"author\":\"Steve McConnell\"}".getBytes("UTF-8"),
        Seq("Content-Type" -> "application/json; charset=utf-8")) {
        status must_== 400
      }
    }
    "return status 200" in {
      post("/documents", "{\"content\":\"book\",\"title\":\"Code Complete\",\"author\":\"Steve McConnell\",\"topic\":\"science\"}".getBytes("UTF-8"),
        Seq("Content-Type" -> "application/json; charset=utf-8")) {
        status must_== 200
      }
    }
  }

  "POST /documents/:ticket/file" should {
    "return status 406" in {
      post("/documents/fd23f84f-38d8-4377-a8c3-bea37ab51bb0/file", Map(), Map("file" -> new java.io.File("README.md"))) {
        status must_== 406
      }
    }
    "return status 202" in {
      post("/documents/fd23f84f-38d8-4377-a8c3-bea37ab51bb0/file", Map(), Map("file" -> new java.io.File("sample/How to make money.pdf"))) {
        status must_== 202
      }
    }

  }

}
