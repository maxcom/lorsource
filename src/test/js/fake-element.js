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

// Элемент страницы в том объёме, в каком его создаёт и настраивает initMarkupToolbar: тулбар с кнопками
// и контейнер вкладок. Слушатели событий собираются, чтобы тест мог их вызвать
function fakeElement() {
  const listeners = {};
  return {
    className: '', dataset: {}, children: [],
    classList: {remove() {}, toggle() {}},
    setAttribute() {},
    appendChild(child) { this.children.push(child); },
    after() {},
    getBoundingClientRect: () => ({top: 0, bottom: 0}),
    addEventListener(type, listener) { (listeners[type] ??= []).push(listener); },
    dispatch(type, event) { (listeners[type] || []).forEach((listener) => listener(event)); },
  };
}

module.exports = {fakeElement};
