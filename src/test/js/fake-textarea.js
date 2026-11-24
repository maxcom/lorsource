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

'use strict';

// Заглушка <textarea>: значение, выделение и методы, к которым обращается редактор
function fakeTextarea(value, selectionStart = 0, selectionEnd = selectionStart) {
  return {
    value, selectionStart, selectionEnd, focused: false,
    focus() { this.focused = true; },
    setSelectionRange(start, end) { this.selectionStart = start; this.selectionEnd = end; },
    setRangeText(text, start, end) { this.value = this.value.slice(0, start) + text + this.value.slice(end); },
  };
}

// Подменяет document.execCommand на время body. В Node объекта document нет,
// и редактор вставляет текст через setRangeText
function withExecCommand(execCommand, body) {
  const had = Object.hasOwn(globalThis, 'document');
  const previous = globalThis.document;
  globalThis.document = {execCommand};
  try {
    return body();
  } finally {
    if (had) {
      globalThis.document = previous;
    } else {
      delete globalThis.document;
    }
  }
}

module.exports = {fakeTextarea, withExecCommand};
