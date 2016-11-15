# Watermark #

## Tech ##
This example uses a number of open source projects to work properly:
* Scalatra - web and restful framework
* Slick - data persistence
* h2 - embedded database
* Akka - async message dispatcher
* itextpdf - PDF manipulation

## Data Model ##
There is only one table named "Documents"

| Column   | Type    | default | nullable |
| :------- | :------ | :------ | :------- |
| id       | Int     |         |          |
| content  | enum(book, journal) |    |   |
| title    | String  |         |          |
| author   | String  |         |          |
| topic    | enum(business, science, media) |    | true |
| status   | enum(initial, pending, done, error) | initial | |
| ticket   | String  |         |          |
| fileName | String  |         |   true   |
| message  | String  |         |   true   |

## Service APIs ##

| Method   | Endpoint | Status Code   | Short Description |
| :------- | :------- | :------------ | :-----------------|
| GET      | /documents               | 200     | return a list all of documents metadata. |
| GET      | /documents/:ticket       | 200     | return a document metadata by ticket, it can be used to check status. |
|          |                          | 404     | document metadata not found by ticket |
| GET      | /documents/:ticket/watermarkedfile | 200     | find a document meta by ticket and return the document pdf rendered with text watermark. |
|          |                          | 404     | document metadata not found or watermarked pdf not found by ticket |
| POST     | /documents               | 200     | create new document metadata, the payload contains content, title, author, and topic(it is required for book content), and return the document metadata, you can find ticket out. |
|          |                          | 400     | bad request |
| POST     | /documents/:ticket/file  | 202     | upload a pdf for the specified document meatdata, the payload is multipart/form-data including only one fieldItem named "file". the file will be processed if it is accepted |
|          |                          | 406     | if the file to upload is unacceptable, e.g. it is not a pdf |
|          |                          | 400     | bad request |



## Build & Run ##

```sh
$ cd Watermark
$ ./sbt
> jetty:start
> browse
```

If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.

## Test ##
```sh
$ cd Watermark
$ ./sbt test
```

## curl Samples ##
Create document metadata
```sh
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
	"content":"book",
	"title":"Code Complete",
	"author": "Steve McConnell",
	"topic":"science"
}' "http://localhost:8080/documents"
```

Upload original pdf
```sh
curl -F "file=@/Users/pqian/Documents/test.pdf" "http://localhost:8080/documents/e86f2885-33b5-4afd-b78d-4324f44f3f76/file"
```

