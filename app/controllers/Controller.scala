package controllers

import akka.actor.ActorSystem
import dao._
import helpers.ErrorHandler.errorResponse
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import models.VAAData

import javax.inject._
import play.api.Logger
import play.api.mvc._
import scanner.NodeProcess

import scala.concurrent.ExecutionContext


@Singleton
class Controller @Inject()(nodeProcess: NodeProcess, outputDAO: OutputDAO, cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  private val logger: Logger = Logger(this.getClass)

  /**
   * Sample controller
   */
  def index: Action[AnyContent] = Action { implicit request =>
    logger.info("index page!")
    Ok("ok")
  }

  /**
   * List boxes which are unSpent for spec scanId. Route: /scan/unspentBoxes/{scanId}
   */
  def getBoxesData(offsetHeight: Int, limitHeight: Int): Action[AnyContent] = Action { implicit request =>
    try {
      var result: Json = Json.Null
      val VAADataList = nodeProcess.getVAAsData(offsetHeight, limitHeight)
      implicit val VAADataEncoder: Encoder[VAAData] = Encoder.instance({ vaaData: VAAData =>
        Map(
          "amount" -> vaaData.amount.asJson,
          "fee" -> vaaData.fee.asJson,
          "chainId" -> vaaData.chainId.asJson,
          "receiverAddress" -> vaaData.receiverAddress.asJson,
          "tokenId" -> vaaData.tokenId.asJson,
        ).asJson
      })
      result = Json.obj("boxesData" -> VAADataList.asJson)
      Ok(result.toString()).as("application/json")
    }
    catch {
      case e: Exception => errorResponse(e)
    }
  }
}

