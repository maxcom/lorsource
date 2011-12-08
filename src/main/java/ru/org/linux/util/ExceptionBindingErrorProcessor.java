package ru.org.linux.util;

import org.springframework.beans.PropertyAccessException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultBindingErrorProcessor;
import ru.org.linux.exception.ScriptErrorException;

public class ExceptionBindingErrorProcessor extends DefaultBindingErrorProcessor {
  @Override
  public void processPropertyAccessException(PropertyAccessException e, BindingResult bindingResult) {
    if (e.getCause() instanceof IllegalArgumentException &&
            e.getCause().getCause() instanceof ScriptErrorException) {
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
