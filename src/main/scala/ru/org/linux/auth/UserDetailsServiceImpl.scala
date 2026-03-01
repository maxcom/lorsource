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
package ru.org.linux.auth

import org.springframework.dao.DataAccessException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import ru.org.linux.user.*

import scala.jdk.CollectionConverters.SeqHasAsJava

object UserDetailsServiceImpl {
  private def retrieveUserAuthorities(user: User) = {
    val results = Vector.newBuilder[GrantedAuthority]

    if (user.activated) {
      results.addOne(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))

      if (user.canCorrect) {
        results.addOne(new SimpleGrantedAuthority("ROLE_CORRECTOR"))
      }

      if (user.isModerator) {
        results.addOne(new SimpleGrantedAuthority("ROLE_MODERATOR"))
      }

      if (user.isAdministrator) {
        results.addOne(new SimpleGrantedAuthority("ROLE_ADMIN"))
      }
    }

    results.result()
  }
}

@Component
class UserDetailsServiceImpl(userService: UserService) extends UserDetailsService {
  @throws[UsernameNotFoundException]
  @throws[DataAccessException]
  override def loadUserByUsername(username: String): UserDetailsImpl = {
    val user: User = if (username.contains("@")) {
      userService.getByEmail(username, searchBlocked = true).getOrElse(throw new UsernameNotFoundException(username))
    } else {
      try {
        userService.getUser(username)
      } catch {
        case _: UserNotFoundException =>
          throw new UsernameNotFoundException(username)
      }
    }

    new UserDetailsImpl(user, UserDetailsServiceImpl.retrieveUserAuthorities(user).asJava, userService.getProfile(user))
  }
}