/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.user

import java.beans.PropertyEditorSupport

class UserPropertyEditor(userService: UserService) extends PropertyEditorSupport {
  @throws[IllegalArgumentException]
  override def setAsText(s: String): Unit = {
    if (s.isEmpty) {
      setValue(null)
    } else {
      try {
        setValue(userService.getUser(s))
      } catch {
        case e: UserNotFoundException =>
          throw new IllegalArgumentException(e)
      }
    }
  }

  override def getAsText: String = {
    if (getValue == null) {
      ""
    } else {
      getValue.asInstanceOf[User].nick
    }
  }
}

class UserIdPropertyEditor (userService: UserService) extends PropertyEditorSupport {
  @throws[IllegalArgumentException]
  override def setAsText(s: String): Unit = {
    if (s.isEmpty) {
      setValue(null)
    } else {
      try {
        setValue(userService.getUserCached(s.toInt))
      } catch {
        case e: UserNotFoundException =>
          throw new IllegalArgumentException(e)
      }
    }
  }

  override def getAsText: String = {
    if (getValue == null) {
      ""
    } else {
      getValue.asInstanceOf[User].id.toString
    }
  }
}
