package com.github.pqian.app

import java.io.{File, FileOutputStream, InputStream, OutputStream}

import DocumentsDAO._
import slick.driver.H2Driver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class DocumentStore(base: String, db: Database) {

  // add a new document file and return document id
  def add(docId: Int, fileName: String, in: InputStream): Future[Int] = {
    val out = new FileOutputStream(asFile(docId, fileName))
    copyStream(in, out)
    db.run(updateDocumentFileName(docId, Some(fileName)).transactionally) map { _ => docId }
  }

  def asFile(docId: Int, fileName: String): File = new File(f"$base/${docId}_$fileName")

  def asWartermarkedFile(docId: Int, fileName: String): File = new File(f"$base/${docId}_watermarked_$fileName")

  def getWartermarkedFile(docId: Int, fileName: Option[String]): Option[File] = {
    fileName match {
      case Some(name) =>
        val file = asWartermarkedFile(docId, name)
        if (file.exists()) Some(file) else None
      case None => None
    }
  }

  // writes an input stream to an output stream
  private def copyStream(input: InputStream, output: OutputStream) {
    val buffer = Array.ofDim[Byte](1024)
    var bytesRead: Int = 0
    while (bytesRead != -1) {
      bytesRead = input.read(buffer)
      if (bytesRead > 0) output.write(buffer, 0, bytesRead)
    }
  }

}
