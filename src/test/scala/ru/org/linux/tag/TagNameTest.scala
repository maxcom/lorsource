package ru.org.linux.tag

import scala.collection.JavaConversions._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import TagName._

@RunWith(classOf[JUnitRunner])
class TagNameTest extends FunSuite {
  test("test sanitize one") {
    assert(List("linux") === parseAndSanitizeTags("linux").toList)
  }

  test("test sanitize list") {
    assert(List("fedora", "ubuntu", "opensuse") === parseAndSanitizeTags("fedora, ubuntu, opensuse").toList)
  }

  test("html tag is invalid") {
    assert(isGoodTag("<b>") === false)
  }

  test("leading space in tag is invalid") {
    assert(isGoodTag(" test") === false)
  }

  test("linux is valid tag") {
    assert(isGoodTag("linux") === true)
  }

  test("c++ is valid tag") {
    assert(isGoodTag("c++") === true)
  }
}

