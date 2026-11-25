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

// За чем следит ResizeObserver в initMarkupToolbar (add-form.js): за textarea, а не за nav и тулбаром

'use strict';

const {describe, test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

require('../../main/webapp/js/markup-editor.js');
const {fakeTextarea} = require('./fake-textarea');
const {fakeElement} = require('./fake-element');

const addFormSource = fs.readFileSync(path.join(__dirname, '../../main/webapp/js/add-form.js'), 'utf8');

// Как и в markup-editor-loading.test.js, add-form.js выполняется в функции, которой window, $script и $
// переданы параметрами; initMarkupToolbar сверх того создаёт тулбар через document
function initToolbar(nav, textarea) {
  const load = new Function('window', '$script', '$', 'document',
      addFormSource + '\nreturn {initMarkupToolbar};');
  const addForm = load({addEventListener() {}}, {ready() {}}, () => {}, {createElement: fakeElement});
  addForm.initMarkupToolbar(nav, textarea, 'lorcode');
}

// Элементы, за которыми initMarkupToolbar следит через ResizeObserver. В Node его нет: без подмены
// тулбар слушает resize окна, и проверять нечего
function observedByResizeObserver(nav, textarea) {
  const observed = [];
  const had = Object.hasOwn(globalThis, 'ResizeObserver');
  const previous = globalThis.ResizeObserver;
  globalThis.ResizeObserver = class {
    observe(target) { observed.push(target); }
  };
  try {
    initToolbar(nav, textarea);
  } finally {
    if (had) {
      globalThis.ResizeObserver = previous;
    } else {
      delete globalThis.ResizeObserver;
    }
  }
  return observed;
}

describe('за чем следит тулбар разметки', () => {
  test('за textarea: её размер колбэк не меняет, а nav и тулбар — меняет', () => {
    const nav = fakeElement();
    const textarea = Object.assign(fakeTextarea(''), fakeElement());

    assert.deepEqual(observedByResizeObserver(nav, textarea), [textarea]);
  });
});
