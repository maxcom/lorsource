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

// Порядок загрузки markup-editor.js и add-form.js: страница грузит их параллельно, в произвольном
// порядке, и регистрирует оба в $script под именем 'markup-editor'; initPreviewTabs ждёт этой готовности

'use strict';

const {describe, test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const webapp = path.join(__dirname, '../../main/webapp');
const addFormSource = fs.readFileSync(path.join(webapp, 'js/add-form.js'), 'utf8');
const EDITOR_READY = 'markup-editor';
const WIRING = "$script(['/js/markup-editor.js?MAVEN_BUILD_TIMESTAMP', '/js/add-form.js?MAVEN_BUILD_TIMESTAMP'],"
  + " 'markup-editor')";

// В браузере add-form.js — обычный скрипт с глобальными window, $script и $. Здесь он выполняется
// в функции, которая получает их параметрами и возвращает нужные тесту функции.
function loadAddForm(scriptLoader) {
  const load = new Function('window', '$script', '$', addFormSource + '\nreturn {initPreviewTabs};');
  return load({}, scriptLoader, () => {});
}

// $script с очередью ready: колбэк ждёт своего имени и выполняется, когда тест объявит имя готовым
function fakeScriptLoader() {
  const waiting = [];
  return {
    ready(names, callback) {
      (Array.isArray(names) ? names : [names]).forEach((name) => waiting.push({name, callback}));
    },
    fire(name) {
      waiting.filter((entry) => entry.name === name).forEach((entry) => entry.callback());
    },
  };
}

function recordingForm() {
  const selectors = [];
  return {
    selectors,
    querySelector(selector) {
      selectors.push(selector);
      return null;
    },
  };
}

// Тексты вызовов загрузчика $script(...) в странице — со всеми вложенными скобками
function scriptCalls(text) {
  const calls = [];
  let from = text.indexOf('$script(');
  while (from !== -1) {
    let depth = 0;
    let quote = null;
    let end = from;
    for (; end < text.length; end++) {
      const ch = text[end];
      if (quote) {
        if (ch === '\\') {
          end++;
        } else if (ch === quote) {
          quote = null;
        }
      } else if (ch === "'" || ch === '"') {
        quote = ch;
      } else if (ch === '(') {
        depth++;
      } else if (ch === ')' && --depth === 0) {
        break;
      }
    }
    calls.push(text.slice(from, end + 1).replace(/\s+/g, ' '));
    from = text.indexOf('$script(', end);
  }
  return calls;
}

function* jspFiles(dir) {
  for (const entry of fs.readdirSync(dir, {withFileTypes: true})) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      yield* jspFiles(full);
    } else if (entry.name.endsWith('.jsp')) {
      yield full;
    }
  }
}

describe('загрузка редактора разметки', () => {
  test('add-form.js выполняется и без MarkupEditor: файлы грузятся в произвольном порядке', () => {
    assert.doesNotThrow(() => loadAddForm(fakeScriptLoader()));
  });

  test('initPreviewTabs ждёт готовности markup-editor и до этого не трогает форму', () => {
    const loader = fakeScriptLoader();
    const {initPreviewTabs} = loadAddForm(loader);
    const form = recordingForm();

    initPreviewTabs(form);
    assert.deepEqual(form.selectors, [], 'до готовности редактора форма не читается');

    loader.fire(EDITOR_READY);
    assert.deepEqual(form.selectors, ['[data-format-mode]'], 'после готовности инициализация начинается');
  });

  test('каждая страница подключает редактор одним вызовом и под именем markup-editor', () => {
    const offenders = [];
    let wired = 0;
    for (const file of jspFiles(path.join(webapp, 'WEB-INF/jsp'))) {
      for (const call of scriptCalls(fs.readFileSync(file, 'utf8'))) {
        if (!call.includes('add-form.js')) continue;
        if (call === WIRING) {
          wired++;
        } else {
          offenders.push(path.basename(file) + ': ' + call);
        }
      }
    }
    assert.ok(wired > 0, 'ни одна страница не подключает редактор этим вызовом');
    assert.deepEqual(offenders, [], 'без имени markup-editor initPreviewTabs не выполнится никогда');
  });
});
