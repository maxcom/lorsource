/*
 * Copyright 1998-2015 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.tag

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import ru.org.linux.tag.TagName._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class TagNameSpec extends Specification {
  "parseAndSanitizeTags" should {
    "parse one simple tag" in {
      parseAndSanitizeTags("linux").asScala must containTheSameElementsAs(Seq("linux"))
    }

    "parse list of simple tags" in {
      parseAndSanitizeTags("fedora, ubuntu, opensuse").asScala must
        containTheSameElementsAs(Seq("fedora", "ubuntu", "opensuse"))
    }
  }

  "parseTags" should {
    "ignore extra spaces and commas" in {
      val (goodTags, badTags) = parseTags("fedora,, ,   ,  ,").partition(isGoodTag)

      badTags must be empty

      goodTags must containTheSameElementsAs(Seq("fedora"))
    }
  }

  "isGoodTag" should {
    "reject html in tag" in {
      isGoodTag("<b>") must beFalse
    }

    "reject leading space" in {
      isGoodTag(" test") must beFalse
    }

    "reject trailing space in tag" in {
      isGoodTag("test ") must beFalse
    }

    "accept 'linux' tag" in {
      isGoodTag("linux") must beTrue
    }

    "accept mixes case tag" in {
      isGoodTag("lInux") must beTrue
    }

    "reject dot in end of tag" in {
      isGoodTag("linux.") must beFalse
    }

    "accept 'c++' tag" in {
      isGoodTag("c++") must beTrue
    }

    "accept '-20' tag" in {
      isGoodTag("-20") must beTrue
    }

    "accept 'c' tag" in {
      isGoodTag("c") must beTrue
    }

    "reject comma" in {
      isGoodTag("test,test") must beFalse
    }

    "accept 'функциональное программирование' tag" in {
      isGoodTag("функциональное программирование") must beTrue
    }
  }
}

