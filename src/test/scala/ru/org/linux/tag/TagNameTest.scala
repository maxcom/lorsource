package ru.org.linux.tag

import scala.collection.JavaConversions._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import TagName._

@RunWith(classOf[JUnitRunner])
class TagNameTest extends FunSuite {
  test("test sanitize one") {
    List("linux") === parseAndSanitizeTags("linux").toList
  }

  test("test sanitize list") {
    List("fedora", "ubuntu", "opensuse") === parseAndSanitizeTags("fedora, ubuntu, opensuse").toList
  }
}

