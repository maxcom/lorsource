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

// Обработчики Enter в initMarkupToolbar (add-form.js): перевод строки в элементе списка

'use strict';

const {describe, test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

require('../../main/webapp/js/markup-editor.js');
const {fakeTextarea} = require('./fake-textarea');

const addFormSource = fs.readFileSync(path.join(__dirname, '../../main/webapp/js/add-form.js'), 'utf8');

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

// Как и в markup-editor-loading.test.js, add-form.js выполняется в функции, которой window, $script и $
// переданы параметрами; initMarkupToolbar сверх того создаёт тулбар через document
function initToolbar(textarea) {
  const load = new Function('window', '$script', '$', 'document',
      addFormSource + '\nreturn {initMarkupToolbar};');
  const addForm = load({addEventListener() {}}, {ready() {}}, () => {}, {createElement: fakeElement});
  addForm.initMarkupToolbar(fakeElement(), textarea, 'lorcode');
}

// Поле редактора с тулбаром; курсор — в конце текста
function toolbarTextarea(value) {
  const textarea = Object.assign(fakeTextarea(value, value.length), fakeElement());
  initToolbar(textarea);
  return textarea;
}

// Посылает событие в поле и возвращает, вызван ли preventDefault
function send(textarea, type, props) {
  const event = {
    isComposing: false, shiftKey: false, ctrlKey: false, metaKey: false, altKey: false,
    prevented: false,
    preventDefault() { this.prevented = true; },
    ...props,
  };
  textarea.dispatch(type, event);
  return event.prevented;
}

describe('Enter в элементе списка', () => {
  const item = '[*] текст';
  const continued = item + '\n[*] ';
  const enter = {key: 'Enter', keyCode: 13};

  test('продолжает список и подменяет перевод строки браузера', () => {
    const textarea = toolbarTextarea(item);
    assert.strictEqual(send(textarea, 'keydown', enter), true);
    assert.strictEqual(textarea.value, continued);
  });

  test('во время набора в IME (isComposing) оставлен методу ввода', () => {
    const textarea = toolbarTextarea(item);
    assert.strictEqual(send(textarea, 'keydown', {...enter, isComposing: true}), false);
    assert.strictEqual(textarea.value, item);
  });

  // Safari не выставляет isComposing у Enter, подтверждающего набранное в IME, — только keyCode 229
  test('подтверждающий IME-ввод Enter в Safari (keyCode 229, isComposing false) список не продолжает', () => {
    const textarea = toolbarTextarea(item);
    assert.strictEqual(send(textarea, 'keydown', {...enter, keyCode: 229}), false);
    assert.strictEqual(textarea.value, item);
  });

  test('клавиатура, не сообщающая Enter в keydown: список продолжает beforeinput', () => {
    const textarea = toolbarTextarea(item);
    assert.strictEqual(send(textarea, 'keydown', {...enter, keyCode: 229}), false);
    assert.strictEqual(send(textarea, 'beforeinput', {inputType: 'insertLineBreak'}), true);
    assert.strictEqual(textarea.value, continued);
  });
});
