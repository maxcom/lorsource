/*
 * Copyright 1998-2022 Linux.org.ru
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
package ru.org.linux.spring

import io.circe.Json
import io.circe.parser.*
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.http.converter.{AbstractHttpMessageConverter, StringHttpMessageConverter}
import org.springframework.http.{HttpInputMessage, HttpOutputMessage, MediaType}
import org.springframework.util.StreamUtils

import java.nio.charset.StandardCharsets

@Configuration
class JsonConfiguration {
  @Bean(name = Array("circeMessageConverter"))
  def circeConverter = new AbstractHttpMessageConverter[Json](MediaType.APPLICATION_JSON) {
    override def supports(clazz: Class[?]): Boolean = classOf[Json].isAssignableFrom(clazz)

    override def readInternal(clazz: Class[? <: Json], inputMessage: HttpInputMessage): Json = {
      parse(StreamUtils.copyToString(inputMessage.getBody, StandardCharsets.UTF_8)).toTry.get
    }

    override def writeInternal(t: Json, outputMessage: HttpOutputMessage): Unit = {
      StreamUtils.copy(t.noSpaces, StandardCharsets.UTF_8, outputMessage.getBody)
    }
  }
}