/*
 * Copyright 1998-2016 Linux.org.ru
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

package ru.org.linux.util;

import org.springframework.beans.PropertyAccessException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultBindingErrorProcessor;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.user.UserNotFoundException;

public class ExceptionBindingErrorProcessor extends DefaultBindingErrorProcessor {
  @Override
  public void processPropertyAccessException(PropertyAccessException e, BindingResult bindingResult) {
    if (e.getCause() instanceof IllegalArgumentException &&
            (e.getCause().getCause() instanceof ScriptErrorException || e.getCause().getCause() instanceof UserNotFoundException)) {
      bindingResult.rejectValue(
              e.getPropertyChangeEvent().getPropertyName(),
              null,
              e.getCause().getCause().getMessage()
      );
    } else {
      super.processPropertyAccessException(e, bindingResult);
    }
  }
}
