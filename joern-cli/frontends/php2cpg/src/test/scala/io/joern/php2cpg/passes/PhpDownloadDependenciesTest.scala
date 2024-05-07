package io.joern.php2cpg.passes

import com.github.sh4869.semver_parser.SemVer
import io.joern.php2cpg.Config
import io.joern.php2cpg.testfixtures.PhpCode2CpgFixture
import io.joern.php2cpg.utils.*
import io.shiftleft.semanticcpg.language.*

class PhpDownloadDependenciesTest extends PhpCode2CpgFixture() {

  "semantic versions from Packagist" should {

    "parse successfully" in {
      "3.0.0.0-beta1".asSemver shouldBe SemVer(3, 0, 0, None, None)
      "3.304.4".asSemver shouldBe SemVer(3, 304, 4, None, None)
      "v2.7.3".asSemver shouldBe SemVer(2, 7, 3, None, None)
    }

  }

  "a PHP project downloading the AWS SDK" should {

    val cpg = code(
      """
        |{
        |    "require": {
        |        "aws/aws-sdk-php": "3.304.4",
        |        "doctrine/orm": "v2.7.3",
        |        "ext-json": "*"
        |    }
        |}
        |""".stripMargin,
      "composer.json"
    )
      .moreCode("""<?php
          |require 'vendor/autoload.php';
          |
          |$s3 = new S3Client([
          |    'version' => 'latest',
          |    'region'  => 'us-east-1',
          |    'credentials' => [
          |        'key'    => 'YOUR_AWS_ACCESS_KEY_ID',
          |        'secret' => 'YOUR_AWS_SECRET_ACCESS_KEY',
          |    ]
          |]);
          |
          |""".stripMargin)
      .withConfig(Config().withDownloadDependencies(true))

    "download the AWS library and populate the CPG with external high-level nodes" in {
      inside(cpg.typeDecl.fullNameExact("Aws\\S3\\S3Client").headOption) {
        case Some(s3Client) =>
          s3Client.isExternal shouldBe true
          s3Client.method.size should be > 0
        case None => fail("Expected a fully qualified AWS S3Client type stub")
      }
    }

    "download the AWS library and infer the full name of `S3Client` correctly" in {
      cpg.call.nameExact("__construct").methodFullName.toSet shouldBe Set("Aws\\S3\\S3Client->__construct")
      cpg.identifier.nameExact("s3").typeFullName.toSet shouldBe Set("Aws\\S3\\S3Client")
    }

  }

}