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

import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

@Configuration
class JsonConfiguration {
  @Bean(name = Array("jacksonMessageConverter"))
  def jsonConverter: MappingJackson2HttpMessageConverter = {
    val converter = new MappingJackson2HttpMessageConverter

    converter.setPrettyPrint(true)
    converter.getObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    converter
  }

  @Bean(name = Array("stringMessageConverter"))
  def stringMessageConverter = new StringHttpMessageConverter
}