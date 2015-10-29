package TidbitsTestbenches

import Chisel._
import TidbitsPlatformWrapper._
import TidbitsDMA._
import TidbitsStreams._

// read and sum a contiguous stream of 32-bit uints from main memory
class TestSum(p: PlatformWrapperParams) extends GenericAccelerator(p) {
  val numMemPorts = 1
  val io = new GenericAcceleratorIF(numMemPorts, p) {
    val start = Bool(INPUT)
    val finished = Bool(OUTPUT)
    val baseAddr = UInt(INPUT, width = 64)
    val byteCount = UInt(INPUT, width = 32)
    val sum = UInt(OUTPUT, width = 32)
  }
  io.signature := makeDefaultSignature()

  val rdP = new StreamReaderParams(
    streamWidth = 32, fifoElems = 8, mem = p.toMemReqParams(),
    maxBeats = 1, chanID = 0
  )

  val reader = Module(new StreamReader(rdP)).io
  val red = Module(new StreamReducer(32, 0, {_+_})).io

  reader.start := io.start
  reader.baseAddr := io.baseAddr
  reader.byteCount := io.byteCount

  red.start := io.start
  red.byteCount := io.byteCount

  io.sum := red.reduced
  io.finished := red.finished

  reader.req <> io.memPort(0).memRdReq
  io.memPort(0).memRdRsp <> reader.rsp

  reader.out <> red.streamIn
}