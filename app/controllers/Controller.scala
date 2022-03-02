package controllers

import akka.actor.ActorSystem
import dao._
import helpers.ErrorHandler.errorResponse
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import models.Observation
import network.NetworkUtils

import javax.inject._
import play.api.Logger
import play.api.mvc._
import scanner.NodeProcess

import scala.concurrent.ExecutionContext


@Singleton
class Controller @Inject()(nodeProcess: NodeProcess, networkUtils: NetworkUtils, cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)

  /**
   * Sample controller
   */
  def index: Action[AnyContent] = Action { implicit request =>
    logger.info("index page!")
    Ok("ok")
  }

  /**
   * @return current height of the blockchain
   */
  def height: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    try {
      var result: Json = Json.Null
      result = Json.fromFields(List(
        ("success", Json.fromBoolean(true)),
        ("height", Json.fromLong(networkUtils.getHeight))
      ))
      Ok(result.asJson.toString()).as("application/json")

    } catch {
      case e: Exception => errorResponse(e)
    }
  }
  /**
   * getting vaaData from bank output boxes
   *
   * @param offsetHeight - start height of search
   * @param limitHeight - limit height of search
   *
   * @return
   * "boxesData": [
   *  {
   *    "amount": 100L,
   *    "fee": 10L,
   *    "chainId": "",
   *    "receiverAddress": "",
   *    tokenId: "",
   *    timestamp: 1645872775
   *  }
   * ]
   */
  def getObservations(offsetHeight: Int, limitHeight: Int): Action[AnyContent] = Action { implicit request =>
    try {
      var result: Json = Json.Null
      val VAADataList = nodeProcess.getVAAsData(offsetHeight, limitHeight)
      implicit val VAADataEncoder: Encoder[Observation] = Encoder.instance({ observe: Observation =>
        Map(
          "txId" -> observe.txId.asJson,
          "timestamp" -> observe.timestamp.asJson,
          "nonce" -> observe.nonce.asJson,
          "sequence" -> observe.sequence.asJson,
          "consistencyLevel" -> observe.consistencyLevel.asJson,
          "emitterAddress" -> observe.emitterAddress.asJson,
          "payload" -> observe.payload.asJson,
          "height" -> observe.height.asJson,
        ).asJson
      })
      result = Json.obj("observations" -> VAADataList.asJson)
      Ok(result.toString()).as("application/json")
    }
    catch {
      case e: Exception => errorResponse(e)
    }
  }
}

