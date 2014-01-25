/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.msg;

public class MessageText {
  private final String text;
  private final boolean lorcode;

  public MessageText(String text, boolean lorcode) {
    this.text = text;
    this.lorcode = lorcode;
  }

  public String getText() {
    return text;
  }

  public boolean isLorcode() {
    return lorcode;
  }
}
