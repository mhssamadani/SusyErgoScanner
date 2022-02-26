package models

import io.circe.syntax._

import org.ergoplatform.modifiers.history.Header
import org.ergoplatform.ErgoBox
import sigmastate.serialization.ValueSerializer
import scorex.util.ModifierId
import scorex.util.encode.Base16
import org.ergoplatform.wallet.serialization.JsonCodecsWrapper._


case class ExtractedBlockModel(headerId: String, parentId: String, height: Int, timestamp: Long)

object ExtractedBlock {
  def apply(header: Header): ExtractedBlockModel = {
    ExtractedBlockModel(header.id, header.parentId, header.height, header.timestamp)
  }
}


case class ExtractedRegisterModel(id: String, boxId: String, value: Array[Byte])

object ExtractedRegister {
  def apply(register: (org.ergoplatform.ErgoBox.NonMandatoryRegisterId, _ <: sigmastate.Values.EvaluatedValue[_ <: sigmastate.SType]), ergoBox: ErgoBox): ExtractedRegisterModel = {
    ExtractedRegisterModel(register._1.toString(), Base16.encode(ergoBox.id), ValueSerializer.serialize(register._2))
  }
}

case class ExtractedAssetModel(tokenId: String, boxId: String, headerId: String, index: Short, value: Long)

object ExtractedAsset {
  def apply(token: (ModifierId, Long), ergoBox: ErgoBox, headerId: String, index: Short): ExtractedAssetModel = {
    ExtractedAssetModel(token._1.toString, Base16.encode(ergoBox.id), headerId, index, token._2)
  }
}

case class ExtractedOutputModel(boxId: String, txId: String, headerId: String, value: Long, creationHeight: Int, index: Short, ergoTree: String, timestamp: Long, bytes: Array[Byte], additionalRegisters: String, additionalTokens: String, spent: Boolean = false)

object ExtractedOutput {
  def apply(ergoBox: ErgoBox, header: Header): ExtractedOutputModel = {
    ExtractedOutputModel(
      Base16.encode(ergoBox.id), ergoBox.transactionId, header.id, ergoBox.value, ergoBox.creationHeight,
      ergoBox.index, Base16.encode(ergoBox.ergoTree.bytes), header.timestamp, ergoBox.bytes,
      ergoBox.additionalRegisters.asJson.toString(),
      ergoBox.additionalTokens.toArray.toSeq.asJson.toString()
    )
  }
}

case class ExtractedOutputResultModel(createdOutputs: Seq[ExtractedOutputModel])

case class VAAData(amount: Long, fee: Long, chainId: String, receiverAddress: String, tokenId:String)