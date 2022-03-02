package scanner

import dao._
import special.collection.Coll
import helpers.Utils.addressEncoder
import io.circe.Decoder
import io.circe.parser.parse
import models.{ExtractedOutput, ExtractedOutputModel, ExtractedOutputResultModel, Observation, VAAPayload}
import network.GetURL.getOrErrorStr
import network.NetworkUtils
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.{Address, ErgoId, ErgoToken, InputBox}
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.modifiers.ErgoFullBlock
import org.ergoplatform.modifiers.history.Header
import scorex.util.encode.Base16
import settings.Configuration

import java.nio.{ByteBuffer, ByteOrder}
import javax.inject.Inject
import scala.collection.mutable
import scala.util.{Failure, Success}


class NodeProcess @Inject()(networkUtils: NetworkUtils,
                            outputDAO: OutputDAO,
                            extractedBlockDAO: ExtractedBlockDAO,
                            daoUtils: DAOUtils) {

  val serverUrl: String = Configuration.serviceConf.serverUrl


  private def getJsonAsString(url: String): String = {
    getOrErrorStr(url) match {
      case Right(Some(json)) => json
      case Right(None) => throw new Exception("Node returned error 404")
      case Left(ex) => throw ex
    }
  }

  def lastHeight: Int = {
    val infoUrl = serverUrl + s"info"
    parse(getJsonAsString(infoUrl)).toTry match {
      case Success(infoJs) =>
        infoJs.hcursor.downField("fullHeight").as[Int].getOrElse(throw new Exception("can't parse fullHeight"))
      case Failure(exception) => throw exception
    }
  }

  def mainChainHeaderIdAtHeight(height: Int): Option[String] = {
    val blockUrl = serverUrl + s"blocks/at/$height"

    val parseResult = parse(getJsonAsString(blockUrl)).toOption.get

    val mainChainId = parseResult.as[Seq[String]].toOption.get.headOption
    mainChainId
  }

  def mainChainHeaderWithHeaderId(headerId: String): Option[Header] = {
    implicit val headerDecoder: Decoder[Header] = Header.jsonDecoder

    val blockHeaderUrl = serverUrl + s"blocks/$headerId/header"
    val parseResultBlockHeader = parse(getJsonAsString(blockHeaderUrl)).toOption.get
    val blockHeader = parseResultBlockHeader.as[Header].toOption
    blockHeader
  }

  def mainChainHeaderAtHeight(height: Int): Option[Header] = {
    val mainChainId = mainChainHeaderIdAtHeight(height)
    if (mainChainId.nonEmpty) mainChainHeaderWithHeaderId(mainChainId.get)
    else None
  }

  def mainChainFullBlockWithHeaderId(headerId: String): Option[ErgoFullBlock] = {
    implicit val txDecoder: Decoder[ErgoFullBlock] = ErgoFullBlock.jsonDecoder

    val txsAsString = getJsonAsString(serverUrl + s"blocks/$headerId")
    val txsAsJson = parse(txsAsString).toOption.get

    val ergoFullBlock = txsAsJson.as[ErgoFullBlock].toOption
    ergoFullBlock
  }

  def getVAAsData(offsetHeight: Int, limitHeight: Int): mutable.Buffer[Observation] = {
    val boxes = outputDAO.selectOutPutBoxesByHeight(offsetHeight, limitHeight)
    var amount = 0L
    var fee = 0L
    var receiverAddress = "".getBytes()
    var chainId = "".getBytes()
    val observationList = mutable.Buffer[Observation]()

    boxes.foreach { box =>
      box.additionalRegisters.foreach(
        register => {
          println(register._1.toString())
          if (register._1.toString() == "4") {
            amount = register._2.value.asInstanceOf[Coll[Byte]](0)
            fee = register._2.value.asInstanceOf[Coll[Byte]](1)
          }
          else if (register._1.toString() == "5") {
            chainId = register._2.value.asInstanceOf[Coll[Coll[Byte]]](0).toArray
            receiverAddress = register._2.value.asInstanceOf[Coll[Coll[Byte]]](1).toArray
          }
        }
      )
      val payloadMap = Seq[Array[Byte]](
        0.toShort.toString.getBytes(),
        ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN).putLong(amount).array(),
        ErgoId.create(box.additionalTokens(1)._1.toString).getBytes,
        ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(2).array(), // Ergo
        Address.create("9fRAWhdxEsTcdb8PhGNrZfwqa65zfkuYHAMmkQLcic1gdLSV5vA").getErgoAddress.script.bytes,
        chainId, // toChain
        ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN).putLong(fee).array()
      )
      var payload: Array[Byte] = Array[Byte]()
      payloadMap.foreach(item =>
        payload = payload ++ item
      )
      val queryBox: ExtractedOutputModel = daoUtils.awaitResult(outputDAO.getByBoxId(Base16.encode(box.id))).get
      val height = daoUtils.execAwait(extractedBlockDAO.getByHeaderId(queryBox.headerId)).head.height
      observationList += Observation(queryBox.txId, queryBox.timestamp, queryBox.txIndex, queryBox.sequence,
        Configuration.serviceConf.consistencyLevel, Configuration.serviceConf.emitterAddress, payload, height)
    }
    observationList
  }

  def convertToInputBox(box: ErgoBox): InputBox = {
    networkUtils.getCtxClient { implicit ctx =>
      val txB = ctx.newTxBuilder()
      val input = txB.outBoxBuilder()
        .value(box.value)
        .tokens(new ErgoToken(box.additionalTokens(0)._1, box.additionalTokens(0)._2),
          new ErgoToken(box.additionalTokens(1)._1, box.additionalTokens(1)._2))
        .contract(new ErgoTreeContract(box.ergoTree))
        .build().convertToInputWith(Base16.encode(box.id), 1)
      input
    }
  }

  def checkBank(box: InputBox): Boolean = {
    val boxAddress = addressEncoder.fromProposition(box.getErgoTree).get.toString
    if (boxAddress == Configuration.serviceConf.bankScriptAddress) {
      return true
    }
    false
  }

  def checkBox(box: InputBox, inputTokens: Long): Boolean = {
    if (box.getTokens.get(1).getValue > inputTokens) {
      return true
    }
    false
  }

  def processTransactions(
                           headerId: String,
                         ): ExtractedOutputResultModel = {

    val ergoFullBlock = mainChainFullBlockWithHeaderId(headerId).get

    val createdOutputs = mutable.Buffer[ExtractedOutputModel]()
    networkUtils.getCtxClient(implicit ctx => {
      ergoFullBlock.transactions.zipWithIndex.foreach {
        case (tx, index) =>
          var inputTokens: Long = 0L
          tx.inputs.zipWithIndex.foreach {
            case (input, _) =>
              val inputBox: InputBox = ctx.getBoxesById(Base16.encode(input.boxId)).head
              if (checkBank(inputBox)) {
                inputTokens = inputBox.getTokens.get(1).getValue
              }
          }
          tx.outputs.foreach { out =>
            val outputBox = convertToInputBox(out)
            if (checkBank(outputBox) && checkBox(outputBox, inputTokens)) {
              createdOutputs += ExtractedOutput(
                out,
                index,
                ergoFullBlock.header
              )
            }
          }
      }
    })
    ExtractedOutputResultModel(createdOutputs)
  }
}
