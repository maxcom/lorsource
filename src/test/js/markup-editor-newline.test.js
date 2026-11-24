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

// Перевод строки в элементе списка: продолжение списка и выход из него (editor.newline)

'use strict';

const {describe, test} = require('node:test');
const assert = require('node:assert/strict');

require('../../main/webapp/js/markup-editor.js');
const {fakeTextarea, withExecCommand} = require('./fake-textarea');

const MarkupEditor = globalThis.MarkupEditor;

// Ожидаемый текст записан с «|» на месте курсора; null — перевод строки оставлен браузеру, текст не меняется.
// caret — курсор (начало выделения) в исходном тексте, по умолчанию конец; selEnd — конец выделения
function enter(name, mode, initial, expected, caret = initial.length, selEnd = caret) {
  test(name, () => {
    const ta = fakeTextarea(initial, caret, selEnd);
    const handled = MarkupEditor.createEditor(ta, MarkupEditor.dialect(mode)).newline();

    if (expected === null) {
      assert.strictEqual(handled, false, 'перевод строки остаётся браузеру');
      assert.strictEqual(ta.value, initial, 'текст не меняется');
      return;
    }

    const at = expected.indexOf('|');
    assert.notStrictEqual(at, -1, 'в ожидаемом тексте должен быть отмечен курсор — «|»');
    assert.strictEqual(handled, true);
    assert.strictEqual(ta.value, expected.replace('|', ''));
    assert.deepStrictEqual([ta.selectionStart, ta.selectionEnd], [at, at]);
    assert.ok(ta.focused, 'фокус должен остаться в поле');
  });
}

describe('Markdown: продолжение списка', () => {
  enter('маркированный: тот же маркер', 'markdown', '* один', '* один\n* |');
  enter('маркер - сохраняется', 'markdown', '- item', '- item\n- |');
  enter('маркер + сохраняется', 'markdown', '+ item', '+ item\n+ |');
  enter('нумерованный: номер + 1', 'markdown', '1. один', '1. один\n2. |');
  enter('нумерованный: номер берётся из текущей строки', 'markdown', '3. x', '3. x\n4. |');
  enter('нумерованный: 9. → 10.', 'markdown', '9. x', '9. x\n10. |');
  enter('отступ до 3 пробелов перед маркером повторяется', 'markdown', '  * x', '  * x\n  * |');
  enter('вложенный с отступом 4: отступ повторяется', 'markdown', '* a\n    * b', '* a\n    * b\n    * |');
  enter('вложенный: третья строка с тем же отступом', 'markdown', '* a\n    * b\n    * c',
      '* a\n    * b\n    * c\n    * |');
  enter('курсор в середине элемента: хвост переходит в новый элемент', 'markdown', '* одиндва', '* один\n* |два', 6);
  enter('выделение заменяется', 'markdown', '* одиндва', '* один\n* |', 6, 9);
  enter('выделение захватывает следующие строки', 'markdown', '* один\ndва', '* один\n* |', 6, 10);
});

describe('Markdown: выход из списка', () => {
  enter('пустой элемент: маркер снимается, курсор остаётся на строке', 'markdown', '* один\n* ', '* один\n|');
  enter('пробелы после маркера пустого элемента', 'markdown', '* один\n*   ', '* один\n|');
  enter('пустой элемент с отступом', 'markdown', '* a\n  * ', '* a\n|');
  enter('нумерованный', 'markdown', '1. a\n2. ', '1. a\n|');
  enter('отмена в середине списка', 'markdown', '* a\n* \n* b', '* a\n|\n* b', 6);
});

describe('Markdown: обычный перевод строки', () => {
  enter('не элемент списка', 'markdown', 'текст', null);
  enter('пустое поле', 'markdown', '', null);
  enter('звёздочка без пробела — курсив, не список', 'markdown', '*курсив* текст', null);
  enter('курсор в начале строки', 'markdown', '* abc', null, 0);
  enter('курсор внутри маркера', 'markdown', '* abc', null, 1);
  enter('курсор внутри номера', 'markdown', '1. abc', null, 2);
  enter('нумерованный со скобкой не распознаётся', 'markdown', '1) x', null);
  enter('список в цитате не распознаётся', 'markdown', '> * x', null);
});

describe('Markdown: блоки кода', () => {
  enter('незакрытое ограждение ```', 'markdown', '```\n* foo', null);
  enter('ограждение с языком', 'markdown', '```bash\n* foo', null);
  enter('строка с языком внутри блока не закрывает его', 'markdown', '```\n```bash\n* foo', null);
  enter('ограждение ~~~', 'markdown', '~~~\n* foo', null);
  enter('после закрывающего ограждения список продолжается', 'markdown', '```\n* foo\n```\n* bar',
      '```\n* foo\n```\n* bar\n* |');
  enter('апострофы в тексте — не ограждение', 'markdown', 'a `x` b\n* foo', 'a `x` b\n* foo\n* |');

  enter('блок с отступом после пустой строки', 'markdown', 'текст\n\n    * foo', null);
  enter('блок с отступом из двух строк', 'markdown', 'текст\n\n    * foo\n    * bar', null);
  enter('блок с отступом, первая строка — не список', 'markdown', 'текст\n\n    x = 1\n    * foo', null);
  enter('блок с отступа в начале текста', 'markdown', '    * foo', null);
  enter('блок с отступа табуляцией', 'markdown', 'текст\n\n\t* foo', null);
});

describe('LORCODE: продолжение списка', () => {
  enter('элемент продолжается', 'lorcode', '[list]\n[*] один', '[list]\n[*] один\n[*] |');
  enter('перед закрывающим тегом', 'lorcode', '[list]\n[*] uno\n[/list]', '[list]\n[*] uno\n[*] |\n[/list]', 14);
  enter('нумерованный список: элементы те же', 'lorcode', '[list="1"]\n[*] uno', '[list="1"]\n[*] uno\n[*] |');
  enter('вложенный: отступ повторяется', 'lorcode', '[list]\n[*] one\n  [*] two',
      '[list]\n[*] one\n  [*] two\n  [*] |');
  enter('курсор в середине элемента', 'lorcode', '[list]\n[*] одиндва', '[list]\n[*] один\n[*] |два', 15);
  enter('без открывающего тега', 'lorcode', 'intro\n\n[*] x', 'intro\n\n[*] x\n[*] |');
  enter('ntobr — тот же диалект', 'ntobr', '[list]\n[*] x', '[list]\n[*] x\n[*] |');
});

describe('LORCODE: выход из списка закрывает его', () => {
  enter('незакрытый список: вставляется [/list]', 'lorcode', '[list]\n[*] uno\n[*] ', '[list]\n[*] uno\n[/list]\n|');
  enter('шаблон кнопки: закрывающий тег уже есть', 'lorcode', '[list]\n[*] uno\n[*] \n[/list]',
      '[list]\n[*] uno\n[/list]\n|', 19);
  enter('сразу после закрывающего тега — текст: добавляется пустая строка', 'lorcode',
      '[list]\n[*] uno\n[*] \n[/list]\nТекст', '[list]\n[*] uno\n[/list]\n|\nТекст', 19);
  enter('после закрывающего тега пустая строка: курсор на ней', 'lorcode', '[list]\n[*] uno\n[*] \n[/list]\n\nТекст',
      '[list]\n[*] uno\n[/list]\n|\nТекст', 19);
  enter('текст после пустой строки: список закрывается перед ним', 'lorcode', '[list]\n[*] uno\n[*] \n\nТекст',
      '[list]\n[*] uno\n[/list]\n|\nТекст', 19);
  enter('закрывающий тег слеплен с текстом: курсор на строке после него', 'lorcode',
      '[list]\n[*] uno\n[*] \n[/list]Текст', '[list]\n[*] uno\n[/list]Текст\n|', 19);
  const twoLists = '[list]\n[*] a\n[*] \n\n[list]\n[*] b\n[/list]';
  enter('следующий список отдельный: закрывается только первый', 'lorcode', twoLists,
      '[list]\n[*] a\n[/list]\n|\n[list]\n[*] b\n[/list]', 17);
  enter('нумерованный', 'lorcode', '[list="1"]\n[*] uno\n[*] ', '[list="1"]\n[*] uno\n[/list]\n|');
  enter('отступ элемента повторяется в закрывающем теге', 'lorcode', '[list]\n[*] one\n  [*] two\n  [*] ',
      '[list]\n[*] one\n  [*] two\n  [/list]\n|');
  enter('пробелы в пустом элементе', 'lorcode', '[list]\n[*] uno\n[*]   ', '[list]\n[*] uno\n[/list]\n|');
  enter('открывающий тег в другом регистре', 'lorcode', '[LIST]\n[*] uno\n[*] ', '[LIST]\n[*] uno\n[/list]\n|');
  enter('открывающий тег в одной строке с элементом', 'lorcode', '[list][*]uno\n[*] ', '[list][*]uno\n[/list]\n|');

  const nested = '[list]\n[*] one\n[*] [list]\n  [*] two\n  [*] \n  [/list]\n[/list]';
  enter('вложенный: закрывается внутренний, курсор остаётся во внешнем элементе', 'lorcode', nested,
      '[list]\n[*] one\n[*] [list]\n  [*] two\n  [/list]\n|\n[/list]', 42);
  enter('вложенный без внутреннего [/list]: тег вставляется', 'lorcode',
      '[list]\n[*] one\n[*] [list]\n    [*] two\n    [*] ', '[list]\n[*] one\n[*] [list]\n    [*] two\n    [/list]\n|');
  enter('три уровня: закрывается самый внутренний', 'lorcode',
      '[list]\n[*] a\n[*] [list]\n  [*] b\n  [*] [list]\n    [*] c\n    [*] \n    [/list]\n  [/list]\n[/list]',
      '[list]\n[*] a\n[*] [list]\n  [*] b\n  [*] [list]\n    [*] c\n    [/list]\n|\n  [/list]\n[/list]', 63);
});

describe('LORCODE: выход только снимает маркер', () => {
  enter('отмена в середине списка', 'lorcode', '[list]\n[*] uno\n[*] \n[*] dos\n[/list]',
      '[list]\n[*] uno\n|\n[*] dos\n[/list]', 19);
  enter('отмена в середине незакрытого списка', 'lorcode', '[list]\n[*] uno\n[*] \n[*] dos',
      '[list]\n[*] uno\n|\n[*] dos', 19);
  enter('отмена в середине списка после пустой строки', 'lorcode', '[list]\n[*] uno\n[*] \n\n[*] dos\n[/list]',
      '[list]\n[*] uno\n|\n\n[*] dos\n[/list]', 19);
  enter('ниже вложенный список', 'lorcode', '[list]\n[*] uno\n[*] \n[*] [list]\n  [*] b\n  [/list]\n[/list]',
      '[list]\n[*] uno\n|\n[*] [list]\n  [*] b\n  [/list]\n[/list]', 19);
  enter('незакрытый вложенный, ниже элемент внешнего', 'lorcode',
      '[list]\n[*] one\n[*] [list]\n    [*] two\n    [*] \n[*] three\n[/list]',
      '[list]\n[*] one\n[*] [list]\n    [*] two\n|\n[*] three\n[/list]', 46);

  enter('нет открывающего тега: закрывать нечего', 'lorcode', '[*]uno\n[*] ', '[*]uno\n|');
  enter('нет открывающего тега, ниже текст', 'lorcode', '[*]uno\n[*] \nТекст', '[*]uno\n|\nТекст', 11);
  enter('нет открывающего тега, ниже чужой [/list]', 'lorcode', '[*]uno\n[*] \n[/list]', '[*]uno\n|\n[/list]', 11);
});

describe('LORCODE: блоки кода', () => {
  enter('[code]', 'lorcode', '[code]\n[*] x', null);
  enter('[code=java]', 'lorcode', '[code=java]\n[*] x', null);
  enter('[pre]', 'lorcode', '[pre]\n[*] x', null);
  enter('[inline]', 'lorcode', '[inline][*] x', null);
  enter('тег в любом регистре', 'lorcode', '[CODE]\n[*] x', null);
  enter('тег в середине строки', 'lorcode', 'text [code][*] x', null);
  enter('после [/code] список продолжается', 'lorcode', '[code=java]\n[*] x\n[/code]\n[*] y',
      '[code=java]\n[*] x\n[/code]\n[*] y\n[*] |');
  enter('[[code]] — экранирование, не тег', 'lorcode', '[[code]]\n[*] x', '[[code]]\n[*] x\n[*] |');
  enter('компактная запись: после [/list] курсор уже вне списка', 'lorcode', '[list]\n[*] uno[/list]', null);
});

describe('набор списка от кнопки', () => {
  // печатает текст в позиции курсора и ставит курсор за ним: setRangeText заглушки выделение не сдвигает
  const type = (ta, text) => {
    const at = ta.selectionStart;
    ta.setRangeText(text, at, ta.selectionEnd);
    ta.setSelectionRange(at + text.length, at + text.length);
  };

  test('Markdown', () => {
    const ta = fakeTextarea('');
    const editor = MarkupEditor.createEditor(ta, MarkupEditor.dialect('markdown'));

    editor.run('list');
    assert.strictEqual(ta.value, '* ');

    type(ta, 'uno');
    assert.strictEqual(editor.newline(), true);
    assert.deepStrictEqual([ta.value, ta.selectionStart], ['* uno\n* ', 8]);

    type(ta, 'dos');
    assert.strictEqual(editor.newline(), true);
    assert.deepStrictEqual([ta.value, ta.selectionStart], ['* uno\n* dos\n* ', 14]);

    assert.strictEqual(editor.newline(), true);
    assert.deepStrictEqual([ta.value, ta.selectionStart], ['* uno\n* dos\n', 12]);
    assert.strictEqual(editor.newline(), false, 'дальше — обычный перевод строки');
  });

  test('LORCODE', () => {
    const ta = fakeTextarea('');
    const editor = MarkupEditor.createEditor(ta, MarkupEditor.dialect('lorcode'));

    editor.run('list');
    assert.strictEqual(ta.value, '[list]\n[*] \n[/list]');

    type(ta, 'uno');
    assert.strictEqual(editor.newline(), true);
    assert.strictEqual(ta.value, '[list]\n[*] uno\n[*] \n[/list]');

    type(ta, 'dos');
    assert.strictEqual(editor.newline(), true);
    assert.strictEqual(ta.value, '[list]\n[*] uno\n[*] dos\n[*] \n[/list]');

    assert.strictEqual(editor.newline(), true);
    assert.deepStrictEqual([ta.value, ta.selectionStart], ['[list]\n[*] uno\n[*] dos\n[/list]\n', 31]);
    assert.strictEqual(editor.newline(), false, 'дальше — обычный перевод строки');
  });
});

describe('вставка текста', () => {
  test('через execCommand, как в браузере: правка идёт одной заменой', () => {
    const ta = fakeTextarea('* один', 6);
    const editor = MarkupEditor.createEditor(ta, MarkupEditor.dialect('markdown'));
    const calls = [];

    withExecCommand((command, _ui, text) => {
      calls.push([command, ta.selectionStart, ta.selectionEnd, text]);
      ta.setRangeText(text, ta.selectionStart, ta.selectionEnd);
      return true;
    }, () => assert.strictEqual(editor.newline(), true));

    assert.deepStrictEqual(calls, [['insertText', 6, 6, '\n* ']]);
    assert.deepStrictEqual([ta.value, ta.selectionStart], ['* один\n* ', 9]);
  });
});
