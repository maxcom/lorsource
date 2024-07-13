/*
 * Copyright 1998-2023 Linux.org.ru
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
package ru.org.linux

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import com.typesafe.scalalogging.StrictLogging
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.web.servlet.config.annotation.{EnableWebMvc, ResourceHandlerRegistry, WebMvcConfigurer}
import org.springframework.web.servlet.handler.MappedInterceptor
import ru.org.linux.adv.{AdvCounterActor, AdvCounterDao, AdvCounterInterceptor}
import ru.org.linux.auth.{GalleryPermissionInterceptor, UserpicPermissionInterceptor}
import ru.org.linux.gallery.ImageDao
import ru.org.linux.group.GroupDao
import ru.org.linux.spring.SiteConfig
import ru.org.linux.topic.{TopicDao, TopicPermissionService}
import ru.org.linux.user.UserService

import java.io.File

@Configuration
@EnableWebMvc
class ImagesResourcesConfiguration(siteConfig: SiteConfig) extends WebMvcConfigurer with StrictLogging {
  private val CachePeriod = 31556926

  override def addResourceHandlers(registry: ResourceHandlerRegistry): Unit = {
    val base = new File(siteConfig.getUploadPath).toURI.toString

    logger.info(s"Base data path for uploads: $base")

    registry
      .addResourceHandler("/images/*/*.jpg", "/images/*/*.png", "/images/*/*.gif")
      .addResourceLocations(s"$base/images/").setCachePeriod(CachePeriod)

    registry
      .addResourceHandler("/gallery/preview/*.jpg", "/gallery/preview/*.png", "/gallery/preview/*.gif")
      .addResourceLocations(s"$base/gallery/preview/").setCachePeriod(CachePeriod)

    registry.addResourceHandler("/photos/*").addResourceLocations(s"$base/photos/").setCachePeriod(CachePeriod)
  }

  @Bean
  def galleryPermissionInterceptor(imageDao: ImageDao, topicDao: TopicDao, groupDao: GroupDao,
                                   topicPermissionService: TopicPermissionService, userService: UserService) = {
    val interceptor = new GalleryPermissionInterceptor(imageDao, topicDao, groupDao, topicPermissionService, userService)

    new MappedInterceptor(Array("/images/**", "/gallery/**"), interceptor)
  }

  @Bean
  def userpicPermissionInterceptor(userService: UserService) = {
    val interceptor = new UserpicPermissionInterceptor(userService)

    new MappedInterceptor(Array("/photos/**"), interceptor)
  }

  @Bean
  def advCounterActor(actorSystem: ActorSystem, advCounterDao: AdvCounterDao): ActorRef[AdvCounterActor.Protocol] = {
    import akka.actor.typed.scaladsl.adapter.*

    actorSystem.spawn(AdvCounterActor.behavior(advCounterDao), "AdvCounterActor")
  }

  @Bean
  def advCounterInterceptor(advCounterAcotor: ActorRef[AdvCounterActor.Protocol]) = {
    val interceptor = new AdvCounterInterceptor(advCounterAcotor)

    new MappedInterceptor(Array("/adv/**"), interceptor)
  }
}
