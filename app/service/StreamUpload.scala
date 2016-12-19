package service

import java.io.File
import java.nio.file.{Files, Path}
import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.attribute.PosixFilePermissions
import java.util
import javax.inject.Inject

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import play.api.libs.ws.{StreamedResponse, WSClient}
import play.api.mvc.MultipartFormData.FilePart
import play.core.parsers.Multipart.FileInfo

import scala.concurrent.Future

/**
  * Created by huangzhibo on 19/12/2016.
  */
class StreamUpload @Inject()(wSClient: WSClient){
    private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

    private def stream2File(streamedResponse: StreamedResponse): FilePart[File] = {
        case FileInfo(partName, filename, contentType) =>
            val attr = PosixFilePermissions.asFileAttribute(util.EnumSet.of(OWNER_READ, OWNER_WRITE))
            val path: Path = Files.createTempFile("multipartBody", "tempFile", attr)
            val file = path.toFile
            val fileSink: Sink[ByteString, Future[IOResult]] = FileIO.toFile(file)
            val iOResultFuture = streamedResponse.body.runWith(fileSink)
            iOResultFuture.map {
                case IOResult(count, status) =>
                    logger.info(s"count = $count, status = $status")
                    FilePart(partName, filename, contentType, file)
            }(play.api.libs.concurrent.Execution.defaultContext)
    }
    private def operateOnTempFile(file: File) = {
        val size = Files.size(file.toPath)
        logger.info(s"size = $size")
        Files.deleteIfExists(file.toPath)
        size
    }

    def uploadstreamImg(streamedResponse: Future[StreamedResponse], clientId: String, applyid: String) = {
        val routerHost = ""
        val uploadIDFrontImgUrl = ""
        val postUrl = routerHost + uploadIDFrontImgUrl + "/" + clientId + "/" + applyid
        logger.info(s"postUrl:$postUrl")
        val filePart = streamedResponse.map(stream => stream2File(stream))
        filePart.map{
            case FilePart(key, filename, contentType, file) =>
                logger.info(s"key = $key, filename = $filename, contentType = $contentType, file = $file")
                wSClient.url(postUrl)
                    .post(
                        Source(
                            FilePart("file", "frontImg.jpg", Option("image/jpeg"), FileIO.fromFile(file))
                                :: List()
                        )
                    )
                val data = operateOnTempFile(file)
                data
        }

    }
}
