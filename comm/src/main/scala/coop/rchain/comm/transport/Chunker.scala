package coop.rchain.comm.transport

import coop.rchain.comm.rp.ProtocolHelper
import coop.rchain.shared.Compression._
import coop.rchain.comm.protocol.routing._
import monix.eval._

object Chunker {

  def chunkIt(networkId: String, blob: Blob, maxMessageSize: Int): Task[Iterator[Chunk]] =
    Task.delay {
      val raw      = blob.packet.content.toByteArray
      val kb500    = 1024 * 500
      val compress = raw.length > kb500
      val content  = if (compress) raw.compress else raw

      def header: Chunk =
        Chunk().withHeader(
          ChunkHeader()
            .withCompressed(compress)
            .withContentLength(raw.length)
            .withSender(ProtocolHelper.node(blob.sender))
            .withTypeId(blob.packet.typeId)
            .withNetworkId(networkId)
        )
      val buffer    = 2 * 1024 // 2 kbytes for protobuf related stuff
      val chunkSize = maxMessageSize - buffer
      def data: Iterator[Chunk] =
        content.sliding(chunkSize, chunkSize).map { data =>
          Chunk().withData(ChunkData().withContentData(ProtocolHelper.toProtocolBytes(data)))
        }

      Iterator(header) ++ data
    }

}
