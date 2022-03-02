package scanner

import dao.DAOUtils
import io.circe.parser.parse
import mocked.MockedNetworkUtils
import models.{ExtractedBlock, ExtractedOutput, ExtractedOutputModel, ExtractedOutputResultModel}
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
    DaoContext.extractedBlockDAO,
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
      ergoFullBlock.transactions.zipWithIndex.foreach {
        case (tx, index) =>
          var inputTokens: Long = 0L
          tx.inputs.zipWithIndex.foreach {
            case (input, _) =>
              val inputBox: InputBox = ctx.newTxBuilder().outBoxBuilder()
                .value(1000L)
                .tokens(new ErgoToken("6eb9719309e89902978749f1f1219e4a1e381c628f763a862d92176a17a8db19", 1),
                  new ErgoToken("f9bb38db0a8aad038695d5c04672c1232a5689579408af5509f31e285516a1e2", 10L))
                .contract(new ErgoTreeContract(Address.create(Configuration.serviceConf.bankScriptAddress).getErgoAddress.script))
                .build().convertToInputWith(Base16.encode(input.boxId), 1)

              if (nodeProcessObj.checkBank(inputBox)) {
                inputTokens = inputBox.getTokens.get(1).getValue
              }
          }
          tx.outputs.foreach { out =>
            val outputBox = nodeProcessObj.convertToInputBox(out)
            if (nodeProcessObj.checkBank(outputBox) && nodeProcessObj.checkBox(outputBox, inputTokens)) {
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

  /**
   * Name: getVAAsData
   * Purpose: Testing function returns a list of vaaDaa based on given offset and limit height.
   * Dependencies:
   * networkUtils
   * OutPutDAO
   */
  property("getVAAsData") {
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
