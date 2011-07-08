/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring.commons;

import java.util.Properties;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 1:36:06
 */
public class PropertiesFacade {
  private Properties general;
  private Properties dist;

  public Properties getGeneral() {
    return general;
  }

  public void setGeneral(Properties general) {
    this.general = general;
  }

  public Properties getDist() {
    return dist;
  }

  public void setDist(Properties dist) {
    this.dist = dist;
  }

  public Properties getProperties(){
    if (general.isEmpty()){
      return dist;
    }
    return general;
  }
}
