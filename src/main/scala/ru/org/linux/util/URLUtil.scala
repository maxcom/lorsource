/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.util

import java.util.regex.Pattern

object URLUtil {
  private val IsUrl: Pattern = Pattern.compile(
    "(((https?)|(ftp))://(([0-9\\p{L}.-]+\\.[0-9\\p{L}]+)|(\\d+\\.\\d+\\.\\d+\\.\\d+))(:[0-9]+)?(/[^ ]*)?)|(mailto:[a-z0-9_+-.]+@[0-9a-z.-]+\\.[a-z]+)|(news:[a-z0-9.-]+)|(((www)|(ftp))\\.(([0-9a-z.-]+\\.[a-z]+(:[0-9]+)?(/[^ ]*)?)|([a-z]+(/[^ ]*)?)))",
    Pattern.CASE_INSENSITIVE
  )

  def fixURL(url: String): String = {
    val trimmed = url.trim

    if (!isUrl(trimmed)) {
      trimmed
    } else if (trimmed.toLowerCase.startsWith("www.")) {
      "http://" + trimmed
    } else if (trimmed.toLowerCase.startsWith("ftp.")) {
      "ftp://" + trimmed
    } else {
      trimmed
    }
  }

  def isUrl(x: String): Boolean = IsUrl.matcher(x).matches
}