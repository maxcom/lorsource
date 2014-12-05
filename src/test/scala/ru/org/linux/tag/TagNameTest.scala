package ru.org.linux.tag

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.org.linux.tag.TagName._

import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class TagNameTest extends FunSuite {
  test("test sanitize one") {
    assert(List("linux") === parseAndSanitizeTags("linux").toList)
  }

  test("test sanitize list") {
    assert(List("fedora", "ubuntu", "opensuse") === parseAndSanitizeTags("fedora, ubuntu, opensuse").toList)
  }

  test("ignore extra spaces and commas") {
    val (goodTags, badTags) = parseTags("fedora,, ,   ,  ,").partition(isGoodTag)

    assert (badTags === Set())
    assert (goodTags === Set("fedora"))
  }

  test("html tag is invalid") {
    assert(isGoodTag("<b>") === false)
  }

  test("leading space in tag is invalid") {
    assert(isGoodTag(" test") === false)
  }

  test("trailing space in tag is invalid") {
    assert(isGoodTag("test ") === false)
  }

  test("linux is valid tag") {
    assert(isGoodTag("linux") === true)
  }

  test("linux in mixed case is valid tag") {
    assert(isGoodTag("lInux") === true)
  }

  test("dot in end is not valid") {
    assert(isGoodTag("linux.") === false)
  }

  test("c++ is valid tag") {
    assert(isGoodTag("c++") === true)
  }

  test("-20 is valid tag") {
    assert(isGoodTag("-20") === true)
  }

  test("c is valid tag") {
    assert(isGoodTag("c") === true)
  }

  test("tag can't contain comma") {
    assert(isGoodTag("test,test") === false)
  }
}

