/*
 * Copyright 1998-2014 Linux.org.ru
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

package ru.org.linux.poll;

import ru.org.linux.site.ScriptErrorException;

public class PollNotFoundException extends ScriptErrorException {
  public PollNotFoundException(int id) {
    super("Голосование #" + id + " не существует");    
  }
  public PollNotFoundException() {
    super("Голосование не существует");
  }

  public PollNotFoundException(String info) {
    super(info);
  }
}
