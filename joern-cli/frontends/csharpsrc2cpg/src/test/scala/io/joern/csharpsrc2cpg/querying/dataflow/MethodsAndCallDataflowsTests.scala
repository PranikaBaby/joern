package io.joern.csharpsrc2cpg.querying.dataflow

import io.joern.csharpsrc2cpg.testfixtures.CSharpCode2CpgFixture
import io.shiftleft.semanticcpg.language.*
import io.joern.dataflowengineoss.language.*

class MethodsAndCallDataflowsTests extends CSharpCode2CpgFixture(withDataFlow = true) {
  "method and call dataflows" should {
    val cpg = code(
      """
        |public class Foo {
        | public int bar(int pBar) {
        |   return pBar + 1;
        | }
        | public void baz() {
        |   int a = 10;
        |   int b = bar(a);
        |   Console.Write(b);
        |
        |   int quxCall = qux(a, 20);
        | }
        |
        | public int qux(int pQux, int ppQux) {
        |   return pQux / ppQux;
        | }
        |}
        |""".stripMargin,
      "Foo.cs"
    )

    "should find a path from a to Write through a call" in {
      val src  = cpg.identifier.nameExact("a").lineNumber(7).l
      val sink = cpg.call.nameExact("Write").l
      sink.reachableBy(src).size shouldBe 1
    }

    "should find a path from 20 and a to quxCall and between args" in {
      val literalSrc = cpg.literal.codeExact("20").l
      val argSrc     = cpg.call.nameExact("qux").argument.isIdentifier.l
      val sink       = cpg.identifier.nameExact("quxCall").l

      sink.reachableBy(literalSrc).size shouldBe 1
      sink.reachableBy(argSrc).size shouldBe 1

      argSrc.reachableBy(literalSrc).size shouldBe 1
    }

  }

  "method and call dataflows across files" should {
    val cpg = code(
      """
        |using HelloBaz.Bazz.Baz;
        |
        |namespace HelloWorld {
        |public class Foo {
        | public int bar(int pBar) {
        |   var b = new Baz();
        |   int res = b.qux(1, "hello");
        |   return res;
        | }
        |}
        |}
        |""".stripMargin,
      "Foo.cs"
    ).moreCode(
      """
        |namespace HelloBaz.Bazz {
        |public class Baz {
        | public int qux(int pQux, string ppQux) {
        |   return pQux;
        | }
        |}
        |}
        |""".stripMargin,
      fileName = "Baz.cs"
    )

    "find a path from 1 to res" in {
      val src  = cpg.literal.codeExact("1").l
      val sink = cpg.identifier.nameExact("res").lineNumber(8).l
      sink.reachableBy(src).size shouldBe 1
    }

    "not find a path from \"hello\" to res" in {
      val src  = cpg.literal.codeExact("\"hello\"").l
      val sink = cpg.identifier.nameExact("res").lineNumber(8).l
      sink.reachableBy(src).size shouldBe 0
    }
  }

}