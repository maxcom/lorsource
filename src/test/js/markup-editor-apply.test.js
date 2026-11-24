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

// Применение разметки редактором: одно нажатие команды

'use strict';

const {describe, test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

require('../../main/webapp/js/markup-editor.js');
const {fakeTextarea, withExecCommand} = require('./fake-textarea');

const MarkupEditor = globalThis.MarkupEditor;

const toolbarSource = fs.readFileSync(path.join(__dirname, '../../main/webapp/js/add-form.js'), 'utf8');

// value → expected, выделение и фокус после нажатия. viaExec: document.execCommand есть и вставляет сам;
// иначе он отказывает (или его нет) и работает setRangeText
function check(name, mode, value, start, end, command, expected, expSel, viaExec) {
  test(name, () => {
    const ta = fakeTextarea(value, start, end);
    const execCommand = viaExec
      ? (cmd, ui, text) => {
        ta.setRangeText(text, ta.selectionStart, ta.selectionEnd);
        return true;
      }
      : () => false;

    withExecCommand(execCommand, () => MarkupEditor.createEditor(ta, MarkupEditor.dialect(mode)).run(command));

    assert.strictEqual(ta.value, expected);
    assert.deepStrictEqual([ta.selectionStart, ta.selectionEnd], expSel);
    assert.ok(ta.focused, 'фокус должен остаться в поле');
  });
}

describe('строчные команды', () => {
  check('bold empty', 'markdown', '', 0, 0, 'bold', '****', [2, 2]);
  check('bold selection', 'markdown', 'hello world', 0, 5, 'bold', '**hello** world', [2, 7]);
  check('italic', 'markdown', 'abc', 0, 3, 'italic', '*abc*', [1, 4]);
  check('strike', 'markdown', 'abc', 0, 3, 'strike', '~~abc~~', [2, 5]);
  check('inline code', 'markdown', 'abc', 1, 2, 'code', 'a`b`c', [2, 3]);
  check('bold via execCommand', 'markdown', 'abc', 0, 3, 'bold', '**abc**', [2, 5], true);
});

describe('ссылка', () => {
  check('md link with selection → url placeholder selected', 'markdown', 'hello world', 6, 11, 'link',
      'hello [world](url)', [14, 17]);
  check('md link no selection → text placeholder selected', 'markdown', 'abc', 3, 3, 'link',
      'abc[текст ссылки](url)', [4, 16]);
  check('lor link with selection → url placeholder selected', 'lorcode', 'ab', 0, 2, 'link', '[url=url]ab[/url]',
      [5, 8]);
  check('lor link no selection', 'lorcode', 'ab', 2, 2, 'link', 'ab[url=url]текст ссылки[/url]', [11, 23]);
});

describe('упоминание', () => {
  check('mention mid-word gets spaces', 'markdown', 'hithere', 2, 2, 'user', 'hi @ник there', [4, 7]);
  check('mention at start', 'markdown', 'hi', 0, 0, 'user', '@ник hi', [1, 4]);
  check('mention before letter → trailing space', 'markdown', 'x', 0, 0, 'user', '@ник x', [1, 4]);
  check('mention after paren → no leading space', 'markdown', 'see ()', 5, 5, 'user', 'see (@ник)', [6, 9]);
  check('mention with selection', 'lorcode', 'hi bob!', 3, 6, 'user', 'hi [user]bob[/user]!', [9, 12]);
  check('mention no selection', 'lorcode', '', 0, 0, 'user', '[user]ник[/user]', [6, 9]);
});

describe('LORCODE, строчные команды', () => {
  check('lor bold', 'lorcode', 'ab', 0, 2, 'bold', '[b]ab[/b]', [3, 5]);
  check('lor inline code', 'lorcode', 'ab', 0, 2, 'code', '[inline]ab[/inline]', [8, 10]);
  check('lor strike', 'lorcode', 'ab', 0, 2, 'strike', '[s]ab[/s]', [3, 5]);
});

describe('цитата', () => {
  // --- line prefix ---
  check('quote middle line', 'markdown', 'line1\nline2\nline3', 8, 9, 'quote', 'line1\n\n> line2\n\nline3', [7, 14]);
  check('quote empty sel mid-line', 'markdown', 'hello world', 6, 6, 'quote', '> hello world', [8, 8]);
  check('quote after blank line', 'markdown', 'para\n\n text', 7, 7, 'quote', 'para\n\n>  text', [9, 9]);
  check('quote caret on 2nd line', 'markdown', 'a\nb', 2, 2, 'quote', 'a\n\n> b', [5, 5]);
  check('quote whole text', 'lorcode', 'ab\ncd', 0, 4, 'quote', '> ab\n> cd', [0, 9]);
  check('quote selection ending with newline', 'markdown', 'a\nb\nc', 2, 4, 'quote', 'a\n\n> b\n\nc', [3, 6]);
  check('quote caret on empty line', 'markdown', 'a\n\n', 3, 3, 'quote', 'a\n\n> ', [5, 5]);
  check('quote at end of first line', 'markdown', 'a\nb', 1, 1, 'quote', '> a\n\nb', [3, 3]);
  check('quote blank lines kept', 'markdown', 'a\n\nb', 0, 4, 'quote', '> a\n\n> b', [0, 8]);
  check('quote already quoted → unquoted', 'markdown', '> a', 0, 3, 'quote', 'a', [0, 1]);
});

describe('списки Markdown', () => {
  check('ordered list two lines', 'markdown', 'a\nb', 0, 3, 'orderedList', '1. a\n2. b', [0, 9]);
  check('ordered list empty sel', 'markdown', 'x', 0, 0, 'orderedList', '1. x', [3, 3]);
  check('ul with blank line inside', 'markdown', 'a\n\nb', 0, 4, 'list', '* a\n\n* b', [0, 8]);
  check('ul after paragraph', 'markdown', 'p\nitem', 2, 6, 'list', 'p\n\n* item', [3, 9]);
  check('ul in the middle of line', 'markdown', 'x', 1, 1, 'list', '* x', [3, 3]);
});

describe('блок кода', () => {
  // --- block ---
  check('code block selection', 'markdown', 'x = 1\ny = 2', 0, 9, 'codeBlock', '```\nx = 1\ny = 2\n```', [0, 19]);
  check('code block partial selection → whole lines', 'markdown', 'a\nbcd\ne', 2, 4, 'codeBlock',
      'a\n\n```\nbcd\n```\n\ne', [3, 14]);
  check('code block empty mid-line', 'markdown', 'hello world', 6, 6, 'codeBlock', 'hello \n\n```\n\n```\n\nworld',
      [12, 12]);
  check('code block empty at line start', 'markdown', 't\n', 2, 2, 'codeBlock', 't\n\n```\n\n```', [7, 7]);
  check('code block empty on blank line', 'markdown', 'a\n\nb', 2, 2, 'codeBlock', 'a\n\n```\n\n```\n\nb', [7, 7]);
  check('code block empty in empty textarea', 'markdown', '', 0, 0, 'codeBlock', '```\n\n```', [4, 4]);
});

describe('LORCODE: списки и блок кода', () => {
  check('lor list selection', 'lorcode', 'a\nb', 0, 3, 'list', '[list]\n[*] a\n[*] b\n[/list]', [0, 26]);
  check('lor list empty', 'lorcode', '', 0, 0, 'list', '[list]\n[*] \n[/list]', [11, 11]);
  check('lor ordered list', 'lorcode', 'a', 0, 1, 'orderedList', '[list="1"]\n[*] a\n[/list]', [0, 24]);
  check('lor code block', 'lorcode', 'a', 0, 1, 'codeBlock', '[code]\na\n[/code]', [0, 16]);
  check('lor list skips blank lines', 'lorcode', 'intro\nalpha\nbeta\n\ngamma\noutro', 6, 21, 'list',
      'intro\n\n[list]\n[*] alpha\n[*] beta\n\n[*] gamma\n[/list]\n\noutro', [7, 51]);
  check('lor ordered list skips blank lines', 'lorcode', 'a\n\nb', 0, 4, 'orderedList',
      '[list="1"]\n[*] a\n\n[*] b\n[/list]', [0, 31]);
});

describe('описания команд', () => {
  const commands = Array.from(toolbarSource.matchAll(/\{command: '(\w+)', label:/g), (m) => m[1]);
  const iconBlock = toolbarSource.match(/const TOOLBAR_ICONS = \{([\s\S]*?)\n\};/)[1];
  const icons = Array.from(iconBlock.matchAll(/^  (\w+): '/gm), (m) => m[1]);

  test('кнопок десять, у каждой есть определение в обоих диалектах и иконка', () => {
    assert.strictEqual(commands.length, 10);
    for (const id of commands) {
      for (const mode of ['markdown', 'lorcode']) {
        const dialect = MarkupEditor.dialect(mode);
        assert.ok(dialect.inline[id] || dialect.prefix[id] || dialect.block[id], `${id}: нет определения для ${mode}`);
      }
      assert.ok(icons.includes(id), `${id}: нет иконки`);
    }
  });

  test('диалект определяется по data-format-mode; ntobr — это LORCODE, plain и неизвестные значения — null', () => {
    assert.strictEqual(MarkupEditor.dialect('ntobr'), MarkupEditor.dialect('lorcode'));
    assert.notStrictEqual(MarkupEditor.dialect('markdown'), MarkupEditor.dialect('lorcode'));
    for (const mode of ['plain', 'constructor', '__proto__', '', undefined]) {
      assert.strictEqual(MarkupEditor.dialect(mode), null, String(mode));
    }
  });
});
