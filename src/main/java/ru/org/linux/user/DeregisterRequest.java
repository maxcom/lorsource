/*
 * Copyright 1998-2018 Linux.org.ru
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

package ru.org.linux.user;

public class DeregisterRequest {
  private String password;
  private boolean acceptBlock;
  private boolean acceptAnonymous;
  private boolean acceptOneway;

  public String getPassword() {
    return password;
  }

  public boolean getAcceptBlock() {
    return acceptBlock;
  }

  public boolean getAcceptAnonymous() {
    return acceptAnonymous;
  }

  public boolean getAcceptOneway() {
    return acceptOneway;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setAcceptBlock(boolean acceptBlock) {
    this.acceptBlock = acceptBlock;
  }

  public void setAcceptAnonymous(boolean acceptAnonymous) {
    this.acceptAnonymous = acceptAnonymous;
  }

  public void setAcceptOneway(boolean acceptOneway) {
    this.acceptOneway = acceptOneway;
  }
}
