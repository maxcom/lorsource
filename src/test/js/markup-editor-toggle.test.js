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

// Снятие разметки повторным нажатием

'use strict';

const {describe, test} = require('node:test');
const assert = require('node:assert/strict');

require('../../main/webapp/js/markup-editor.js');
const {fakeTextarea} = require('./fake-textarea');

const MarkupEditor = globalThis.MarkupEditor;

const caret = (text, offset) => [text, [offset, offset]];
const sel = (text, a, b) => [text, [a, b]];

// Несколько нажатий подряд; states — [value, [selStart, selEnd]] после каждого нажатия,
// commands — строка или по одному на каждое нажатие
function seq(name, mode, initial, start, end, commands, states) {
  if (typeof states[0] === 'string') states = [states];
  test(name, () => {
    const ta = fakeTextarea(initial, start, end);
    ta.focus();
    const editor = MarkupEditor.createEditor(ta, MarkupEditor.dialect(mode));
    const list = Array.isArray(commands) ? commands : Array(states.length).fill(commands);

    states.forEach(([value, selection], i) => {
      editor.run(list[i]);
      assert.deepStrictEqual(
        [ta.value, ta.selectionStart, ta.selectionEnd, ta.focused],
        [value, selection[0], selection[1], true],
        `после нажатия ${i + 1} (${list[i]})`
      );
    });
  });
}

describe('inline: применить → снять', () => {
  const mdMarkers = {bold: ['**', '**'], italic: ['*', '*'], strike: ['~~', '~~'], code: ['`', '`']};
  const lorMarkers = {
    bold: ['[b]', '[/b]'],
    italic: ['[i]', '[/i]'],
    strike: ['[s]', '[/s]'],
    code: ['[inline]', '[/inline]'],
  };
  for (const [mode, markers] of [['markdown', mdMarkers], ['lorcode', lorMarkers]]) {
    for (const [command, [open, close]] of Object.entries(markers)) {
      const wrapped = open + 'hello' + close + ' world';
      seq(`${mode} ${command} выделение`, mode, 'hello world', 0, 5, command,
          [sel(wrapped, open.length, open.length + 5), sel('hello world', 0, 5)]);
      const empty = 'hello' + open + close + ' world';
      seq(`${mode} ${command} курсор`, mode, 'hello world', 5, 5, command,
          [caret(empty, 5 + open.length), caret('hello world', 5)]);
    }
  }
});

describe('inline: уже размечено', () => {
  seq('md bold: курсор внутри', 'markdown', '**hello** world', 5, 5, 'bold', sel('hello world', 0, 5));
  seq('md bold: выделен текст, маркеры вокруг', 'markdown', '**hello** world', 2, 7, 'bold', sel('hello world', 0, 5));
  seq('md bold: выделение вместе с маркерами', 'markdown', '**hello** world', 0, 9, 'bold', sel('hello world', 0, 5));
  seq('md bold: выделено с открывающим маркером — снимается и парный закрывающий', 'markdown', '**hello** world', 0,
      7, 'bold', sel('hello world', 0, 5));
  seq('md italic внутри bold не снимает bold (курсор)', 'markdown', '**text**', 4, 4, 'italic', caret('**te**xt**', 5));
  seq('md italic внутри bold не снимает bold (выделение)', 'markdown', '**text**', 2, 6, 'italic',
      sel('***text***', 3, 7));
  seq('md italic: звёздочки с пробелами — не разметка; второе нажатие снимает вставленную пару', 'markdown',
      'a * b * c', 5, 5, 'italic', [caret('a * b** * c', 6), caret('a * b * c', 5)]);
  seq('md code: двойные апострофы — не пара для одинарного; второе нажатие снимает пару', 'markdown', '``code``', 4,
      4, 'code', [caret('``co``de``', 5), caret('``code``', 4)]);
  seq('md bold в конце текста', 'markdown', 'x **y**', 5, 5, 'bold', sel('x y', 2, 3));
  seq('lor bold: курсор внутри', 'lorcode', '[b]hello[/b] world', 5, 5, 'bold', sel('hello world', 0, 5));
  seq('lor bold: выделение вместе с тегами', 'lorcode', '[b]hello[/b] world', 0, 12, 'bold', sel('hello world', 0, 5));
  seq('lor bold: пара на другой строке не подходит; второе нажатие снимает вставленную пару', 'lorcode',
      '[b]x[/b]\nfoo\n[b]y[/b]', 11, 11, 'bold',
      [caret('[b]x[/b]\nfo[b][/b]o\n[b]y[/b]', 14), caret('[b]x[/b]\nfoo\n[b]y[/b]', 11)]);
  seq('lor bold: пробел рядом с тегом — допустимо', 'lorcode', '[b] x [/b]', 4, 4, 'bold', sel(' x ', 0, 3));
});

describe('ссылка', () => {
  const mdLink = 'see [GitHub](http://github.com) now';
  const openAt = mdLink.indexOf('['), textAt = mdLink.indexOf('GitHub'), closeAt = mdLink.lastIndexOf(')');
  seq('md link: применить → снять (выделение → выделен url)', 'markdown', 'see GitHub now', 4, 10, 'link',
      [sel('see [GitHub](url) now', 13, 16), sel('see GitHub now', 4, 10)]);
  seq('md link: применить → снять (без выделения, плейсхолдер не тронут)', 'markdown', 'see Git now', 7, 7, 'link',
      [sel('see Git[текст ссылки](url) now', 8, 20), caret('see Git now', 7)]);
  seq('md link: курсор внутри текста', 'markdown', mdLink, 9, 9, 'link', sel('see GitHub now', 4, 10));
  seq('md link: курсор внутри url', 'markdown', mdLink, 15, 15, 'link', sel('see GitHub now', 4, 10));
  seq('md link: выделен текст ссылки', 'markdown', mdLink, textAt, textAt + 6, 'link', sel('see GitHub now', 4, 10));
  seq('md link: выделена вся ссылка', 'markdown', mdLink, openAt, closeAt + 1, 'link', sel('see GitHub now', 4, 10));
  seq('md link: скобки в url', 'markdown', '[x](http://a/(b))', 1, 1, 'link', sel('x', 0, 1));
  seq('md link: ссылка дальше по строке, курсор до неё — применяется', 'markdown', 'a [b](u) c', 1, 1, 'link',
      sel('a[текст ссылки](url) [b](u) c', 2, 14));
  seq('md link: вторая ссылка в строке', 'markdown', '[a](1) and [b](2)', 12, 12, 'link', sel('[a](1) and b', 11, 12));
  seq('lor link: применить → снять (выделение → выделен url)', 'lorcode', 'see GitHub now', 4, 10, 'link',
      [sel('see [url=url]GitHub[/url] now', 9, 12), sel('see GitHub now', 4, 10)]);
  seq('lor link: применить → снять (без выделения, плейсхолдер не тронут)', 'lorcode', 'see Git now', 7, 7, 'link',
      [sel('see Git[url=url]текст ссылки[/url] now', 16, 28), caret('see Git now', 7)]);
  seq('lor link: курсор внутри', 'lorcode', 'see [url=http://x]text[/url] now', 6, 6, 'link',
      sel('see text now', 4, 8));
  seq('lor link: только url', 'lorcode', '[url]http://x[/url]', 5, 5, 'link', sel('http://x', 0, 8));
});

describe('упоминание', () => {
  seq('md user: применить → снять', 'markdown', 'hi ', 3, 3, 'user', [sel('hi @ник', 4, 7), caret('hi ', 3)]);
  seq('md user: курсор после ника', 'markdown', 'hi @maxcom', 10, 10, 'user', sel('hi maxcom', 3, 9));
  seq('md user: выделен ник', 'markdown', 'hi @maxcom', 4, 10, 'user', sel('hi maxcom', 3, 9));
  seq('md user: выделено с @', 'markdown', 'hi @maxcom', 3, 10, 'user', sel('hi maxcom', 3, 9));
  seq('md user: email — не упоминание', 'markdown', 'email@example', 13, 13, 'user', sel('email@example @ник', 15, 18));
  seq('md user: @ в другом слове строки не подходит', 'markdown', 'a @b cd', 7, 7, 'user', sel('a @b cd @ник', 9, 12));
  seq('lor user: применить → снять', 'lorcode', '', 0, 0, 'user', [sel('[user]ник[/user]', 6, 9), caret('', 0)]);
  seq('lor user: курсор внутри', 'lorcode', 'hi [user]maxcom[/user]', 9, 9, 'user', sel('hi maxcom', 3, 9));
});

describe('цитата', () => {
  for (const mode of ['markdown', 'lorcode']) {
    seq(`${mode} quote: применить → снять (выделение)`, mode, 'a\nb', 0, 3, 'quote',
        [sel('> a\n> b', 0, 7), sel('a\nb', 0, 3)]);
    seq(`${mode} quote: применить → снять (курсор)`, mode, 'hello', 2, 2, 'quote',
        [caret('> hello', 4), caret('hello', 2)]);
    seq(`${mode} quote: выделение процитировано`, mode, '> a\n> b', 0, 7, 'quote', sel('a\nb', 0, 3));
    seq(`${mode} quote: пустая строка внутри`, mode, '> a\n\n> b', 0, 8, 'quote', sel('a\n\nb', 0, 4));
    seq(`${mode} quote: вложенная снимается на уровень`, mode, '>> a', 0, 4, 'quote', sel('> a', 0, 3));
    seq(`${mode} quote: смешанное выделение добавляет уровень`, mode, 'plain\n> quoted', 0, 13, 'quote',
        sel('> plain\n>> quoted', 0, 17));
  }
});

describe('списки Markdown', () => {
  seq('md list: применить → снять', 'markdown', 'a\nb', 0, 3, 'list', [sel('* a\n* b', 0, 7), sel('a\nb', 0, 3)]);
  seq('md list: маркеры -, +', 'markdown', '- a\n+ b', 0, 7, 'list', sel('a\nb', 0, 3));
  seq('md orderedList: применить → снять', 'markdown', 'a\nb', 0, 3, 'orderedList',
      [sel('1. a\n2. b', 0, 9), sel('a\nb', 0, 3)]);
  seq('md orderedList: курсор в элементе', 'markdown', 'x\n1. item', 8, 8, 'orderedList', caret('x\nitem', 5));
  seq('md list: нумерованный → маркированный', 'markdown', '1. a\n2. b', 0, 9, 'list', sel('* a\n* b', 0, 7));
  seq('md orderedList: маркированный → нумерованный', 'markdown', '* a\n* b', 0, 7, 'orderedList',
      sel('1. a\n2. b', 0, 9));
  seq('md list: строка без маркера — применяется', 'markdown', '* a\nb', 0, 5, 'list', sel('* * a\n* b', 0, 9));
});

describe('списки LORCODE', () => {
  const lorList = '[list]\n[*] a\n[*] b\n[/list]';
  const lorNested = '[list]\n[*] one\n[*] [list]\n  [*] two\n  [/list]\n[/list]';
  seq('lor list: применить → снять (пустой)', 'lorcode', '', 0, 0, 'list',
      [caret('[list]\n[*] \n[/list]', 11), caret('', 0)]);
  seq('lor list: применить → снять (выделение)', 'lorcode', 'a\nb', 0, 3, 'list',
      [sel(lorList, 0, 26), sel('a\nb', 0, 3)]);
  seq('lor list: курсор внутри', 'lorcode', lorList, 10, 10, 'list', sel('a\nb', 0, 3));
  seq('lor list: выделены элементы', 'lorcode', 'intro\n\n[list]\n[*] a\n[/list]\n\noutro', 14, 19, 'list',
      sel('intro\n\na\n\noutro', 7, 8));
  seq('lor orderedList: снять', 'lorcode', '[list="1"]\n[*] a\n[/list]', 0, 24, 'orderedList', sel('a', 0, 1));
  seq('lor list: нумерованный → маркированный', 'lorcode', '[list="1"]\n[*] a\n[/list]', 13, 13, 'list',
      caret('[list]\n[*] a\n[/list]', 9));
  seq('lor orderedList: маркированный → нумерованный', 'lorcode', '[list]\n[*] a\n[/list]', 0, 20, 'orderedList',
      sel('[list="1"]\n[*] a\n[/list]', 0, 24));
  seq('lor list: внешний список вокруг вложенного', 'lorcode', lorNested, 7, 7, 'list',
      sel('one\n[list]\n  [*] two\n  [/list]', 0, 30));
  seq('lor list: вложенный в элемент — вливается, маркер элемента остаётся', 'lorcode', lorNested, 32, 32, 'list',
      sel('[list]\n[*] one\n  [*] two\n[/list]', 15, 24));
  seq('lor list: строка вне списка → элемент списка; повторное нажатие снимает (пустая строка остаётся)', 'lorcode',
      'x\n[list]\n[*] a\n[/list]', 0, 0, 'list',
      [caret('[list]\n[*] x\n[/list]\n\n[list]\n[*] a\n[/list]', 11), sel('x\n\n[list]\n[*] a\n[/list]', 0, 1)]);
});

describe('блок кода', () => {
  seq('md codeBlock: применить → снять (пустой)', 'markdown', '', 0, 0, 'codeBlock',
      [caret('```\n\n```', 4), caret('', 0)]);
  seq('md codeBlock: применить → снять (выделение)', 'markdown', 'x', 0, 1, 'codeBlock',
      [sel('```\nx\n```', 0, 9), sel('x', 0, 1)]);
  seq('md codeBlock: курсор внутри', 'markdown', 'intro\n\n```\ncode\n```\n\noutro', 13, 13, 'codeBlock',
      sel('intro\n\ncode\n\noutro', 7, 11));
  seq('md codeBlock: с языком', 'markdown', '```bash\ncode\n```', 10, 10, 'codeBlock', sel('code', 0, 4));
  seq('md codeBlock: курсор на закрывающем ограждении', 'markdown', '```\ncode\n```', 10, 10, 'codeBlock',
      sel('code', 0, 4));
  seq('md codeBlock: второй блок', 'markdown', '```\na\n```\nx\n```\nb\n```', 15, 15, 'codeBlock',
      sel('```\na\n```\nx\nb', 12, 13));
  seq('md codeBlock: строка с языком внутри блока не закрывает его', 'markdown', '```\n```bash\nx\n```', 6, 6,
      'codeBlock', sel('```bash\nx', 0, 9));
  seq('md codeBlock: курсор вне блока — применяется', 'markdown', 'a\n```\nb\n```', 0, 0, 'codeBlock',
      caret('```\n\n```\n\na\n```\nb\n```', 4));
  seq('lor codeBlock: применить → снять', 'lorcode', 'x', 0, 1, 'codeBlock',
      [sel('[code]\nx\n[/code]', 0, 16), sel('x', 0, 1)]);
  seq('lor codeBlock: с языком, курсор внутри', 'lorcode', '[code=java]\nx\n[/code]', 13, 13, 'codeBlock',
      sel('x', 0, 1));
});

describe('курсор вплотную к чужому маркеру', () => {
  seq('md bold: курсор перед закрывающим маркером — снимается пара', 'markdown', '**Linux**.', 7, 7, 'bold',
      sel('Linux.', 0, 5));
  seq('md bold: курсор перед закрывающим внутри слова', 'markdown', 'a**b**c', 4, 4, 'bold', sel('abc', 1, 2));
  seq('md italic: курсор перед закрывающим внутри слова', 'markdown', 'a*b*c', 3, 3, 'italic', sel('abc', 1, 2));
  seq('md strike: курсор внутри зачёркивания в жирном', 'markdown', '**~~x~~** y', 5, 5, 'strike',
      sel('**x** y', 2, 3));
  seq('md bold: курсор перед чужим открывающим — вставляется пара (чужой цел)', 'markdown', 'Use **bold** here', 4,
      4, 'bold', caret('Use ******bold** here', 6));
  seq('md italic: курсор перед чужим закрывающим', 'markdown', 'a *b* c', 4, 4, 'italic', sel('a b c', 2, 3));
  seq('md italic: курсор после чужого закрывающего — вставляется пара', 'markdown', 'a *b* c', 5, 5, 'italic',
      caret('a *b*** c', 6));
  seq('lor bold: курсор перед чужим открывающим тегом — вставляется пара', 'lorcode', 'a [b]b[/b] c', 2, 2, 'bold',
      caret('a [b][/b][b]b[/b] c', 5));
  seq('lor bold: курсор перед чужим закрывающим тегом — снимается пара', 'lorcode', 'a [b]b[/b] c', 6, 6, 'bold',
      sel('a b c', 2, 3));
  seq('lor bold: курсор внутри открывающего тега — ничего не происходит', 'lorcode', 'a [b]b[/b] c', 4, 4, 'bold',
      caret('a [b]b[/b] c', 4));
  seq('lor user: курсор перед чужим открывающим тегом — вставляется плейсхолдер', 'lorcode',
      'hi [user]maxcom[/user]', 3, 3, 'user', sel('hi [user]ник[/user][user]maxcom[/user]', 9, 12));
});

describe('жирный + курсив', () => {
  seq('md bold: ***выделение*** снимает две звезды', 'markdown', '***hello*** world', 3, 8, 'bold',
      sel('*hello* world', 1, 6));
  seq('md italic: ***выделение*** снимает одну звезду', 'markdown', '***hello*** world', 3, 8, 'italic',
      sel('**hello** world', 2, 7));
  seq('md bold: курсор внутри ***текста***', 'markdown', '***hello*** world', 6, 6, 'bold', sel('*hello* world', 1, 6));
  seq('md italic: курсор внутри ***текста***', 'markdown', '***hello*** world', 6, 6, 'italic',
      sel('**hello** world', 2, 7));
  seq('md bold: выделение вместе с ***маркерами***', 'markdown', '***hello*** world', 0, 11, 'bold',
      sel('*hello* world', 1, 6));
  seq('md italic: **** четыре звезды — курсива нет, применяется', 'markdown', '****text****', 4, 8, 'italic',
      sel('*****text*****', 5, 9));
  seq('md bold: **** четыре звезды — снимается один уровень', 'markdown', '****text****', 4, 8, 'bold',
      sel('**text**', 2, 6));
  seq('md bold: *только курсив* — применяется (получается *** )', 'markdown', '*text*', 1, 5, 'bold',
      sel('***text***', 3, 7));
  seq('md code: `код` курсор внутри', 'markdown', 'a `code` b', 4, 4, 'code', sel('a code b', 2, 6));
  seq('md code: ``двойные`` — не пара для одинарного', 'markdown', 'a ``code`` b', 5, 5, 'code',
      caret('a ``c``ode`` b', 6));
});

describe('упоминание: курсор внутри', () => {
  seq('md user: курсор сразу после @ перед плейсхолдером — снимается всё', 'markdown', 'hi @ник', 4, 4, 'user',
      caret('hi ', 3));
  seq('md user: курсор в середине ника', 'markdown', 'hi @maxcom', 7, 7, 'user', sel('hi maxcom', 3, 9));
  seq('md user: курсор в конце ника', 'markdown', 'hi @maxcom', 10, 10, 'user', sel('hi maxcom', 3, 9));
  seq('md user: выделена часть ника — применяется', 'markdown', 'hi @maxcom', 4, 7, 'user', sel('hi @ @max com', 6, 9));
  seq('md user: курсор перед @ — применяется', 'markdown', 'hi @maxcom', 3, 3, 'user', sel('hi @ник@maxcom', 4, 7));
});

describe('списки LORCODE: границы и вложенность', () => {
  seq('lor list: курсор на строке [/list] — снимается, а не оборачивается', 'lorcode', '[list]\n[*] a\n[/list]', 14,
      14, 'list', sel('a', 0, 1));
  seq('lor list: вложенный в список — вливается в родительский', 'lorcode',
      '[list]\n[*] a\n[list]\n[*] b\n[/list]\n[/list]', 22, 22, 'list', sel('[list]\n[*] a\n[*] b\n[/list]', 13, 18));
  seq('lor list: внешний вокруг вложенного — маркеры внешнего снимаются', 'lorcode',
      '[list]\n[*] a\n[list]\n[*] b\n[/list]\n[/list]', 7, 7, 'list', sel('a\n[list]\n[*] b\n[/list]', 0, 22));
});

describe('край выделения разрезает пару; слипшаяся вставленная пара', () => {
  seq('md bold: выделение разрезает открывающую пару — снимается вся пара', 'markdown', '**hello** world', 1, 7,
      'bold', sel('hello world', 0, 5));
  seq('md bold: выделение разрезает закрывающую пару — снимается вся пара', 'markdown', '**hello** world', 2, 8,
      'bold', sel('hello world', 0, 5));
  seq('md italic: пустая пара слиплась с чужой звездой справа — снимается повторным нажатием', 'markdown', 'a *i* b',
      5, 5, 'italic', [caret('a *i*** b', 6), caret('a *i* b', 5)]);
  seq('md italic: пустая пара слиплась с чужой звездой слева — снимается повторным нажатием', 'markdown', 'a *i* b',
      2, 2, 'italic', [caret('a ***i* b', 3), caret('a *i* b', 2)]);
  seq('md code: пустая пара слиплась с чужими апострофами — снимается повторным нажатием', 'markdown', 'a `c` b', 5,
      5, 'code', [caret('a `c``` b', 6), caret('a `c` b', 5)]);
  seq('md bold: пустая пара рядом с чужим курсивом — снимается повторным нажатием', 'markdown', 'a *i* b', 5, 5,
      'bold', [caret('a *i***** b', 7), caret('a *i* b', 5)]);
  seq('md: другая команда в слипшейся паре ничего не делает', 'markdown', 'a *i* b', 5, 5, ['italic', 'bold'],
      [caret('a *i*** b', 6), caret('a *i*** b', 6)]);
});

describe('снятие выделяет сам текст', () => {
  seq('md bold: ***выделение с маркерами*** — выделен текст без оставшихся звезд', 'markdown', '***hello*** world',
      0, 11, 'bold', sel('*hello* world', 1, 6));
  seq('md bold: пара целиком внутри выделения', 'markdown', 'x **hello** y', 2, 11, 'bold', sel('x hello y', 2, 7));
});

describe('курсор внутри чужого маркера: ничего не происходит', () => {
  seq('md italic: курсор внутри открывающих ***', 'markdown', '***hello*** world', 1, 1, 'italic',
      caret('***hello*** world', 1));
  seq('md bold: курсор внутри закрывающих ***', 'markdown', '***hello*** world', 10, 10, 'bold',
      caret('***hello*** world', 10));
  seq('md italic: слипшаяся пара без памяти о вставке', 'markdown', 'a *i*** b', 6, 6, 'italic', caret('a *i*** b', 6));
  seq('md code: курсор внутри двойных апострофов', 'markdown', 'a ``c`` b', 3, 3, 'code', caret('a ``c`` b', 3));
  seq('lor bold: курсор внутри закрывающего тега', 'lorcode', 'a [b]x[/b] c', 8, 8, 'bold', caret('a [b]x[/b] c', 8));
  seq('lor bold: курсор сразу после открывающего тега — пара снимается', 'lorcode', 'a [b]x[/b] c', 5, 5, 'bold',
      sel('a x c', 2, 3));
  seq('lor bold: курсор в начале строки перед текстом с «[» — вставляется пара', 'lorcode', '[b]x[/b]', 0, 0, 'bold',
      caret('[b][/b][b]x[/b]', 3));
});

describe('побочные эффекты бездействия и памяти о паре', () => {
  seq('md: бездействие другой команды не сбрасывает память о вставленной паре', 'markdown', 'a *i* b', 5, 5,
      ['italic', 'bold', 'italic'], [caret('a *i*** b', 6), caret('a *i*** b', 6), caret('a *i* b', 5)]);
  seq('lor bold: [i] синтаксически тег курсива — бездействие', 'lorcode', 'int arr[i] = 0;', 8, 8, 'bold',
      caret('int arr[i] = 0;', 8));
  seq('lor bold: курсор перед «]» тега [i] — бездействие', 'lorcode', 'int arr[i] = 0;', 9, 9, 'bold',
      caret('int arr[i] = 0;', 9));
  seq('lor bold: курсор перед «[» — вставляется пара', 'lorcode', 'int arr[i] = 0;', 7, 7, 'bold',
      caret('int arr[b][/b][i] = 0;', 10));
  seq('lor italic: внутри сноски [1] — вставляется пара', 'lorcode', 'см. [1] и [2]', 5, 5, 'italic',
      caret('см. [[i][/i]1] и [2]', 8));
  seq('lor bold: внутри [count] — не тег, вставляется пара', 'lorcode', 'переменная [count]', 14, 14, 'bold',
      caret('переменная [co[b][/b]unt]', 17));
  seq('lor bold: внутри настоящего тега с параметром — бездействие', 'lorcode', 'a [url=http://x]t[/url] b', 14, 14,
      'bold', caret('a [url=http://x]t[/url] b', 14));
  seq('lor bold: внутри закрывающего тега — бездействие', 'lorcode', 'a [b]x[/b] c', 8, 8, 'bold',
      caret('a [b]x[/b] c', 8));
  seq('lor bold: внутри маркера элемента [*] — бездействие', 'lorcode', '[list]\n[*] a\n[/list]', 8, 8, 'bold',
      caret('[list]\n[*] a\n[/list]', 8));
  seq('lor bold: незакрытая «[» на строке выше не мешает', 'lorcode', 'x [\ny z] w', 5, 5, 'bold',
      caret('x [\ny[b][/b] z] w', 8));
  seq('lor bold: скобка закрыта на строке выше — не мешает', 'lorcode', 'x [b]\nz ] w', 6, 6, 'bold',
      caret('x [b]\n[b][/b]z ] w', 9));
  seq('lor bold: курсор в середине тега на той же строке — бездействие', 'lorcode', 'x [b]\nz ] w', 3, 3, 'bold',
      caret('x [b]\nz ] w', 3));
  seq('lor bold: тег не может занимать две строки — вставляется пара', 'lorcode', 'x [b\ny] w', 5, 5, 'bold',
      caret('x [b\n[b][/b]y] w', 8));
  seq('md bold: набранная вручную пара, курсор внутри — бездействие (снять можно выделив)', 'markdown',
      'hello**** world', 7, 7, 'bold', caret('hello**** world', 7));
  seq('md bold: набранная вручную пара, курсор внутри, после вставки кнопкой — снимается памятью', 'markdown',
      'hello world', 5, 5, ['bold', 'bold'], [caret('hello**** world', 7), caret('hello world', 5)]);
  seq('md bold: выделена пустая пара звёзд — снимается', 'markdown', 'hello**** world', 5, 9, 'bold',
      caret('hello world', 5));
  seq('md italic: курсор внутри чужой пары звёзд без памяти о вставке — бездействие', 'markdown', 'hello** world', 6,
      6, 'italic', caret('hello** world', 6));
  seq('md italic: курсор сразу после чужой пары — вставляется пара', 'markdown', 'hello** world', 7, 7, 'italic',
      caret('hello**** world', 8));
  seq('md italic: две набранные вручную звезды, выделенные целиком, снимаются как пустая пара', 'markdown',
      'hello** world', 5, 7, 'italic', caret('hello world', 5));
  seq('md italic: пустая пара курсива внутри слова', 'markdown', 'a**b', 1, 3, 'italic', caret('ab', 1));
  seq('md code: пустая пара апострофов внутри слова', 'markdown', 'a``b', 1, 3, 'code', caret('ab', 1));
  seq('md bold: шесть звезд выделены — снимается один уровень, курсор на месте пары', 'markdown', 'a****** b', 1, 7,
      'bold', caret('a** b', 1));
  seq('md bold: двух выделенных звезд для жирного мало — применяется', 'markdown', 'a**b', 1, 3, 'bold',
      sel('a******b', 3, 5));
  seq('md italic: курсор внутри той же пары — по-прежнему бездействие', 'markdown', 'a**b', 2, 2, 'italic',
      caret('a**b', 2));
  seq('md italic: курсор между закрывающими звездами жирного не ломает его', 'markdown', '**text** world', 7, 7,
      'italic', caret('**text** world', 7));
  seq('md bold: то же самое для жирного', 'markdown', 'a **b** c', 6, 6, 'bold', caret('a **b** c', 6));
  seq('md bold: курсор делит чужую последовательность — бездействие', 'markdown', 'a **b**** c', 7, 7, 'bold',
      caret('a **b**** c', 7));
});

describe('выделенный ограничитель чужой разметки — не пустая пара', () => {
  seq('md italic: выделен открывающий маркер жирного — бездействие, жирный цел', 'markdown', '**text** world', 0, 2,
      'italic', sel('**text** world', 0, 2));
  seq('md italic: выделен закрывающий маркер жирного — бездействие', 'markdown', 'x **text** y', 8, 10, 'italic',
      sel('x **text** y', 8, 10));
  seq('md strike: выделен открывающий маркер зачёркивания — бездействие', 'markdown', 'a~~s~~ b', 1, 3, 'strike',
      sel('a~~s~~ b', 1, 3));
  seq('md code: выделен апостроф при второй паре на строке — бездействие', 'markdown', 'a `x` `` b', 6, 8, 'code',
      sel('a `x` `` b', 6, 8));
  seq('md italic: пустая пара на строке с другой парой курсива — бездействие (не отличить)', 'markdown',
      'a *x* ** b', 6, 8, 'italic', sel('a *x* ** b', 6, 8));
  seq('md italic: пара на другой строке не мешает снять пустую пару', 'markdown', '*x*\na**b', 5, 7, 'italic',
      caret('*x*\nab', 5));
  seq('md italic: тройная звезда без пары снимается по одной с краёв', 'markdown', 'a***b', 1, 4, 'italic',
      caret('a*b', 1));
  seq('md bold: выделен ограничитель жирного при чужом курсиве на строке — бездействие', 'markdown', 'a *x* **b** c',
      6, 8, 'bold', sel('a *x* **b** c', 6, 8));
  seq('md bold: две звезды без пары — для пустой пары жирного мало, применяется', 'markdown', 'a**b', 1, 3, 'bold',
      sel('a******b', 3, 5));
  seq('md italic: повторное нажатие на снятой паре применяет её заново', 'markdown', 'a**b', 1, 3,
      ['italic', 'italic'], [caret('ab', 1), sel('a**b', 2, 2)]);
});
