package scanner

import dao.DAOUtils
import io.circe.parser.parse
import mocked.MockedNetworkUtils
import models.{ExtractedBlock, ExtractedOutput, ExtractedOutputModel, ExtractedOutputResultModel, VAAData}
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, ErgoToken, InputBox}
import org.ergoplatform.modifiers.ErgoFullBlock
import org.ergoplatform.modifiers.history.Header

import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import scorex.util.encode.Base16
import settings.Configuration

import testHandlers.TestSuite

import scala.collection.mutable
import scala.io.Source.fromFile
import scala.language.postfixOps

class NodeProcessSpec extends TestSuite {
  private val logger: Logger = Logger(this.getClass)

  // Defining stealth class parameters
  val networkUtils = new MockedNetworkUtils

  val mockedDBConfigProvider: DatabaseConfigProvider = mock[DatabaseConfigProvider]

  def readJsonFile(filePath: String): String = {
    val sourceFile = fromFile(filePath)
    val jsonString = sourceFile.getLines.mkString
    sourceFile.close()
    jsonString
  }

  val daoUtils = new DAOUtils(mockedDBConfigProvider)

  def createNodeProcessObject = new NodeProcess(
    networkUtils.getMocked,
    DaoContext.outputDAO,
    daoUtils
  )

  def readingHeaderData(): Header = {
    val headerJson = parse(readJsonFile("./test/dataset/SampleStealth_ergoFullBlockHeader.json")).toOption.get
    headerJson.as[Header].toOption.get
  }

  def readingErgoFullBlockData(): ExtractedOutputResultModel = {
    val txsAsJson = parse(readJsonFile("./test/dataset/SampleStealth_ergoFullBlock.json")).toOption.get
    val ergoFullBlock = txsAsJson.as[ErgoFullBlock].toOption.get
    val createdOutputs = mutable.Buffer[ExtractedOutputModel]()
    val nodeProcessObj = createNodeProcessObject
    networkUtils.getMocked.getCtxClient(implicit ctx => {
      ergoFullBlock.transactions.foreach { tx =>
        var inputTokens: Long = 0L
        tx.inputs.zipWithIndex.foreach {
          case (input, _) =>
            val inputBox: InputBox = ctx.newTxBuilder().outBoxBuilder()
              .value(1000L)
              .tokens(new ErgoToken("Token_1", 1),
                new ErgoToken("Token_2", 10L))
              .contract(new ErgoTreeContract(Address.create(Configuration.serviceConf.bankScriptAddress).getErgoAddress.script))
              .build().convertToInputWith(Base16.encode(input.boxId), 1)

              ctx.getBoxesById(Base16.encode(input.boxId)).head
            if (nodeProcessObj.checkBank(inputBox)) {
              inputTokens = inputBox.getTokens.get(1).getValue
            }
        }
        tx.outputs.foreach { out =>
          val outputBox = nodeProcessObj.convertToInputBox(out)
          if (nodeProcessObj.checkBank(outputBox) && nodeProcessObj.checkBox(outputBox, inputTokens)) {
            createdOutputs += ExtractedOutput(
              out,
              ergoFullBlock.header
            )
          }
        }
      }
    })
    ExtractedOutputResultModel(createdOutputs)
  }

  /**
   * Name: getVAAsData
   * Purpose: Testing function returns a list of vaaDaa based on given offset and limit height.
   * Dependencies:
   * networkUtils
   * OutPutDAO
   */
  property("getVAAsData stealth address") {
    // reading header data
    val header = readingHeaderData()
    // reading ergo full block data
    val extractionResult = readingErgoFullBlockData()
    // storing outputs
    DaoContext.extractionResultDAO.storeOutputsAndRelatedData(extractionResult.createdOutputs, ExtractedBlock(header))

    val nodeProcessObj = createNodeProcessObject

    val vaaDataList = nodeProcessObj.getVAAsData(138778, 138900)

    vaaDataList.size should not be 0
  }

}
