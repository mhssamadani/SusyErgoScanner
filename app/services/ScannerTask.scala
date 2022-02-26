package services

import java.io.{PrintWriter, StringWriter}
import dao._

import javax.inject.Inject
import play.api.Logger

import scala.annotation.tailrec
import models._
import scanner.NodeProcess

class ScannerTask @Inject()(extractedBlockDAO: ExtractedBlockDAO, extractionResultDAO: ExtractionResultDAO,
                            forkedResultDAO: ForkedResultDAO, nodeProcess: NodeProcess) {

  private val logger: Logger = Logger(this.getClass)

  @tailrec
  private def step(lastHeight: Int): Unit = {
    val localId = extractedBlockDAO.getHeaderIdByHeight(lastHeight)
    if (localId == nodeProcess.mainChainHeaderIdAtHeight(lastHeight).get) {
      // no fork
      val newHeight = lastHeight + 1
      nodeProcess.mainChainHeaderAtHeight(newHeight) match {
        case Some(header) =>
          logger.info(s"Processing block at height: $newHeight, id: ${header.id}")
          val extractionResult = nodeProcess.processTransactions(header.id)
          extractionResultDAO.storeOutputsAndRelatedData(extractionResult.createdOutputs, ExtractedBlock(header))
          val extractedCount = extractionResult.createdOutputs.length
          logger.info("Extracted: " + extractedCount + " outputs")
          step(newHeight)
        case None =>
          logger.info(s"No block found @ height $newHeight")
      }
    } else {
      var syncHeight = lastHeight - 1
      while (extractedBlockDAO.getHeaderIdByHeight(lastHeight) !=
        nodeProcess.mainChainHeaderIdAtHeight(syncHeight).get) {
        syncHeight -= 1
      }
      for (height <- syncHeight + 1 until lastHeight) {
        forkedResultDAO.migrateBlockByHeight(height)
      }
      step(syncHeight)
    }
  }


  def getStackTraceStr(e: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }


  def start(): Unit = {
    try {
      val lastHeight = extractedBlockDAO.getLastHeight
      step(lastHeight)
    }
    catch {
      case a: Throwable =>
        logger.error(getStackTraceStr(a))
    }
  }

}
