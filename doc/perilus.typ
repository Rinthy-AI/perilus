#import "@preview/fletcher:0.5.8" as fletcher: diagram, node, edge, shapes.trapezium

#set page(width: auto, height: auto)

#let reg(x, y, id) = {
  node((x, y), raw(id), name: id, stroke: 1pt)
}

#let mux(x, y, id, n) = {
  let i = 0
  while i < n {
    node((x, {y + i}), raw(str(i)), name: id + "_" + str(i))
    i += 1
  }
  let i = 0
  let enclose = while i < n {
    (label(id + "_" + str(i)),)
    i += 1
  }
  node(
    name: id,
    enclose: enclose,
    shape: trapezium.with(dir: right),
    stroke: 1pt,
    inset: 0mm,
  )
}

#let module(x, y, id, io_ids, width, alignment) = {
  let io_y = y
  for io_id in io_ids {
    let name = id + "_" + io_id
    node((x, io_y), align(alignment)[#raw(io_id)], name: name, width: width)
    io_y = io_y + 1
  }
  let io_names = for io_id in io_ids {
    (label(id + "_" + io_id),)
  }
  node(
    enclose: io_names,
    name: label(id),
    stroke: 1pt,
    inset: 0mm,
  )
}

#diagram(
  debug: 0,
  spacing: (5mm, 1mm),

  module(
    24, 0, "controlUnit",
    ("op", "funct3", "funct7-5", "zero", "adrSrc", "irWrite", "memWrite", "pcWrite", "regWrite",
     "aluSrcA", "aluSrcB", "resultSrc", "immSrc", "aluControl", "dataMask", "lessThan"),
    23mm,
    left,
  ),
  module(0, 0, "registerFile", ("a1", "a2", "a3", "writeData3", "writeEnable3", "rd1", "rd2"), 27mm,
right),
  module(0, 7, "extendUnit", ("input", "immSrc", "immExt"), 27mm, right),
  module(0, 10, "memory", ("address", "writeData", "writeEnable", "readData", "dataMask"), 27mm, right),
  module(0, 15, "alu", ("aluControl", "srcA", "srcB", "aluResult", "zero", "lessThan"), 27mm, right),

  reg(12.5, 16.5, "pc"),
  reg(14, 16.5, "oldPc"),
  reg(12, 1, "instr"),
  reg(8, 16, "readDataBuf"),
  reg(8, 5, "rd1Buf"),
  reg(8, 6, "rd2Buf"),
  reg(8, 7, "four"),
  reg(8, 15, "aluOutBuf"),

  edge(<controlUnit_pcWrite.west>, (12.5, 7), <pc.north>, "->"),

  edge(<controlUnit_irWrite.west>, (16, 5), (16, 0), (12, 0), <instr.north>, "->"),
  edge(<pc.east>, <oldPc.west>, "->"),

  edge(<controlUnit_irWrite.west>, (14, 5), <oldPc.north>, "->"),
  edge(<memory_readData.east>, (7, 13), (7, 1), <instr.west>, "->"),

  edge(<memory_readData.east>, (7, 13), (7, 16), <readDataBuf.west>, "->"),

  edge(<registerFile_rd1.east>, <rd1Buf.west>, "->"),
  edge(<registerFile_rd2.east>, <rd2Buf.west>, "->"),

  edge(<alu_aluResult.east>, (5, 18), (5, 15), <aluOutBuf.west>, "->"),

  mux(9, 15, "muxResult", 4),
  edge(<controlUnit_resultSrc.west>, (9, 11), <muxResult.north>, "->"),
  edge(<muxResult.east>, (10, 16.5), <pc.west>, "->", label: `result`, label-pos: 0.5, label-side: right),
  edge(<aluOutBuf.east>, <muxResult_0.west>, "->"),
  edge(<readDataBuf.east>, <muxResult_1.west>, "->"),
  edge(<alu_aluResult.east>, (5, 18), (5, 17), <muxResult_2.west>, "->"),
  edge(<extendUnit_immExt.east>, (2, 9), (2, 17.5), (6, 17.5), (6, 18), <muxResult_3.west>, "->"),

  edge(<controlUnit_aluControl.west>, (16, 13), (16, 13.75), (3, 13.75), (3, 15), <alu_aluControl.east>, "->"),

  mux(17, 15.5, "muxAluSrcA", 3),
  edge(<controlUnit_aluSrcA.west>, (17, 9), <muxAluSrcA.north>, "->"),
  edge(<muxAluSrcA.east>, (18, 16.5), (18, 18.75), (3, 18.75), (3, 16), <alu_srcA.east>, "->", label: `A`, label-pos: 0.15),
  edge(<pc.east>, (13, 16.5), (13, 15.5), <muxAluSrcA_0.west>, "->"),
  edge(<oldPc.east>, <muxAluSrcA_1.west>, "->"),
  edge(<rd1Buf.east>, (8.5, 5), (8.5, 3.5), (11.5, 3.5), (11.5, 17.5), <muxAluSrcA_2.west>, "->"),

  mux(10, 5, "muxAluSrcB", 3),
  edge(<controlUnit_aluSrcB.west>, (18, 10), (18, 3), (10, 3), <muxAluSrcB.north>, "->"),
  edge(<muxAluSrcB.east>, (11, 6), (11, 13.5), (4, 13.5), (4, 17), <alu_srcB.east>, "->", label: `B`, label-pos: 0.15),
  edge(<rd2Buf.east>, (9, 6), (9, 5), <muxAluSrcB_0.west>, "->"),
  edge(<extendUnit_immExt.east>, (9.5, 9), (9.5, 6), <muxAluSrcB_1.west>, "->"),
  edge(<four.east>, <muxAluSrcB_2.west>, "->"),

  edge(<alu_zero.east>, (22, 19), (22, 3), <controlUnit_zero.west>, "->"),
  edge(<alu_lessThan.east>, (23, 20), (23, 15), <controlUnit_lessThan.west>, "->"),

  edge(<instr.east>, (18, 1), (18, 0), <controlUnit_op.west>, "->", label: `[6:0]`, label-pos: 0.82),
  edge(<instr.east>, (18, 1), <controlUnit_funct3.west>, "->", label: `[14:12]`, label-pos:
  0.75),
  edge(<instr.east>, (18, 1), (18, 2), <controlUnit_funct7-5.west>, "->", label: `[30]`, label-pos:
  0.82),

  edge(<instr.east>, (14, 1), (14, -1), (5, -1), (5, 7), <extendUnit_input.east>, "->", label: `[31:7]`, label-pos: 0.9),
  edge(<controlUnit_immSrc.west>, (21, 12), (21, 8), <extendUnit_immSrc.east>, "->"),

  mux(15, 12, "muxMemAddr", 2),
  edge(<controlUnit_adrSrc.west>, (15, 4), <muxMemAddr.north>, "->"),
  edge(<muxMemAddr.east>, (16, 12.5), (16, 10), <memory_address.east>, "->"),
  edge(<muxResult.east>, (10, 16.5), (10, 12), <muxMemAddr_0.west>, "->"),
  edge(<pc.east>, (13, 16.5), (13, 13), <muxMemAddr_1.west>, "->"),

  edge(<rd2Buf.east>, (8.5, 6), (8.5, 11), <memory_writeData.east>, "->"),
  edge(<controlUnit_memWrite.west>, (14.5, 6), (14.5, 10.5), (9.5, 10.5), (9.5, 12), <memory_writeEnable.east>, "->"),
  edge(<controlUnit_dataMask.west>, <memory_dataMask.east>, "->"),

  edge(<instr.east>, (14, 1), (14, -1), (5, -1), (5, 0), <registerFile_a1.east>, "->", label:
  `[19:15]`, label-pos: 0.9),
  edge(<instr.east>, (14, 1), (14, -1), (5, -1), (5, 1), <registerFile_a2.east>, "->", label:
  `[24:20]`, label-pos: 0.9),
  edge(<instr.east>, (14, 1), (14, -1), (5, -1), (5, 2), <registerFile_a3.east>, "->", label:
  `[11:7]`, label-pos: 0.9),
  edge(<muxResult.east>, (10, 16.5), (10, 9.5), (4, 9.5), (4, 3), <registerFile_writeData3.east>, "->"),
  edge(<controlUnit_regWrite.west>, (23, 8), (23, 4.5), (11, 4.5), (11, 4), <registerFile_writeEnable3.east>, "->"),
)
