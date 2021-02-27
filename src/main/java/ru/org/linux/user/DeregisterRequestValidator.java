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

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class DeregisterRequestValidator implements Validator {
  public DeregisterRequestValidator(boolean isFullDelete) {
    this.isFullDelete = isFullDelete;
  }
  private boolean isFullDelete;
  @Override
  public boolean supports(Class aClass) {
    return DeregisterRequest.class.equals(aClass);
  }

  @Override
  public void validate(Object o, Errors errors) {
    DeregisterRequest form = (DeregisterRequest) o;

    String msgDereg;
    if(isFullDelete)  msgDereg = "с удалением"; else msgDereg = "с блокировкой";
    
    if (!form.getAcceptBlock()) {
      errors.reject("acceptBlock", null, "Вы не согласились " + msgDereg + " аккаунта");
    }

    if(isFullDelete && !form.getAcceptMoveToDeleted()) {
      errors.reject("acceptMoveToDeleted", null, "Вы не согласились с передачей всех сообщений специальному пользователю");
    }
    
    if (!form.getAcceptOneway()) {
      errors.reject("acceptOneway", null, "Вы не согласились с невозможностью восстановления аккаунта");
    }
  }
}
