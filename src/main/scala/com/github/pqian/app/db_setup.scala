package com.github.pqian.app

import slick.driver.H2Driver.api._
import scala.concurrent.ExecutionContext.Implicits._

import slick.jdbc.ResultSetAction
import slick.jdbc.GetResult._

import Tables._

object DbSetup {

  // test if a table exists
  def testTableExists(tableName: String): DBIO[Boolean] = {
    def localTables: DBIO[Vector[String]] =
      ResultSetAction[(String,String,String, String)](_.conn.getMetaData().getTables("", "", null, null)).map { ts =>
        ts.filter(_._4.toUpperCase == "TABLE").map(_._3).sorted
      }

    localTables map (_.exists(_.toLowerCase == tableName.toLowerCase))
  }

  val insertDocuments: DBIO[Option[Int]] = {
    documents ++= Seq(
      Document(1, Content.book, "The Dark Code", "Bruce Wayne", Some(Topic.science), Status.done,"e833a0ca-7dec-4c10-9464-42a2b9990461", Some("The Dark Code.pdf"), None),
      Document(2, Content.book, "How to make money", "Dr. Evil", Some(Topic.business), Status.initial,"fd23f84f-38d8-4377-a8c3-bea37ab51bb0", None, None),
      Document(3, Content.journal, "Journal of human flight routes", "Clark Kent", None, Status.done,"55463e84-1dfc-44e8-93e2-4d47a5ebf3b3", Some("Journal of human flight routes.pdf"), None)
    )
  }

  // create schema if it not exists
  val createDatabase: DBIO[Unit] = {

    val createDatabase0: DBIO[Unit] = for {
      _ <- (documents.schema).create
      _ <- insertDocuments
    } yield ()

    for {
      exists <- testTableExists("documents")
      _ <- if (!exists) createDatabase0 else DBIO.successful()
    } yield ()

  }

  // drop schema if it exists
  val dropDatabase: DBIO[Unit] = {
    testTableExists("documents") flatMap {
      case true => (documents.schema).drop
      case false => DBIO.successful()
    }
  }

}