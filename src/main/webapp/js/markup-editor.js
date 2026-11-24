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

/*
 * Логика редактора разметки: команды для Markdown и LORCODE, поиск уже расставленной разметки
 * и правка текста в textarea. Файл не обращается к странице при загрузке, поэтому подключается в Node
 * для тестов (src/test/js). Тулбар и вкладки — в add-form.js.
 */
(function (root) {
  'use strict';

  const LinkTextPlaceholder = 'текст ссылки';
  const NickPlaceholder = 'ник';

  /*
   * Описание команд для языка разметки:
   *  - inline: { markers, insert, remove } — обрамление выделения, markers = [открывающий, закрывающий маркер];
   *  - prefix: { make, remove } — префикс строк выделения;
   *  - block: { parts, wrapLine, remove } — обёртка выделения по строкам,
   *    parts = [открывающая конструкция, префикс строки, закрывающая конструкция];
   *    wrapLine: без выделения текущая непустая строка становится элементом, иначе вставляется пустой шаблон.
   * insert/make/parts применяют разметку; remove ищет уже расставленную и возвращает правку,
   * снимающую её, { noop: true } — если нажатие должно ничего не менять, либо null — тогда разметка применяется.
   */
  const TOOLBAR_DIALECTS = {
    markdown: {
      inline: {
        bold: wrapper('**', '**'),
        italic: wrapper('*', '*'),
        strike: wrapper('~~', '~~'),
        code: wrapper('`', '`'),
        link: {
          insert: (selection) => surround('[', ')', selection || LinkTextPlaceholder, {
            middle: '](', placeholder: 'url', selectPlaceholder: Boolean(selection)
          }),
          remove: (ctx) => dropPlaceholder(findMarkdownLink(ctx), ctx, LinkTextPlaceholder)
        },
        user: {
          insert: (selection, before, after) => mention('@', selection, before, after),
          remove: (ctx) => dropPlaceholder(findMention(ctx), ctx, NickPlaceholder)
        }
      },
      prefix: {
        quote: {make: (_index, line) => quotePrefix(line), remove: findQuotedLines},
        list: {make: () => '* ', remove: (ctx) => findMarkdownList(ctx, false)},
        orderedList: {make: (index) => `${index + 1}. `, remove: (ctx) => findMarkdownList(ctx, true)}
      },
      block: {
        codeBlock: {parts: ['```', '', '```'], remove: (ctx) => findFencedBlock(ctx, /^```/, /^```$/)}
      }
    },
    lorcode: {
      inline: {
        bold: wrapper('[b]', '[/b]'),
        italic: wrapper('[i]', '[/i]'),
        strike: wrapper('[s]', '[/s]'),
        code: wrapper('[inline]', '[/inline]'),
        link: {
          insert: (selection) => surround('[url=', '[/url]', selection || LinkTextPlaceholder, {
            middle: ']', placeholder: 'url', placeholderFirst: true, selectPlaceholder: Boolean(selection)
          }),
          remove: (ctx) => dropPlaceholder(findLorcodeLink(ctx), ctx, LinkTextPlaceholder)
        },
        user: wrapper('[user]', '[/user]', {placeholder: NickPlaceholder})
      },
      prefix: {
        quote: {make: (_index, line) => quotePrefix(line), remove: findQuotedLines}
      },
      block: {
        codeBlock: {
          parts: ['[code]', '', '[/code]'],
          remove: (ctx) => findFencedBlock(ctx, /^\[code(=[^\]]*)?\]$/, /^\[\/code\]$/)
        },
        list: {parts: ['[list]', '[*] ', '[/list]'], wrapLine: true, remove: (ctx) => findLorcodeList(ctx, false)},
        orderedList: {
          parts: ['[list="1"]', '[*] ', '[/list]'], wrapLine: true, remove: (ctx) => findLorcodeList(ctx, true)
        }
      }
    }
  };

  /* теги LORCODE из DefaultParserParameters.java: бездействовать имеет смысл только внутри настоящего тега */
  const LorTag = /^\/?(b|i|u|s|em|strong|url|user|br|softbr|p|div|quote|list|pre|code|inline|cut|\*)(=[^\]]*)?$/i;

  function wrapper(before, after, {placeholder = ''} = {}) {
    return {
      markers: [before, after],
      insert: (selection) => surround(before, after, selection || placeholder),
      remove: (ctx) => dropPlaceholder(findWrapper(ctx, before, after), ctx, placeholder)
    };
  }

  /*
   * Собирает обрамление выделения: [before][selection][middle][placeholder][after];
   * при placeholderFirst плейсхолдер ставится перед текстом: [before][placeholder][middle][selection][after].
   * После вставки выделяется плейсхолдер (selectPlaceholder), либо текст выделения, либо плейсхолдер;
   * если выделения и плейсхолдера нет — курсор ставится между before и after.
   */
  function surround(before, after, selection, options = {}) {
    const {middle = '', placeholder = '', placeholderFirst = false, selectPlaceholder = false} = options;

    const text = placeholderFirst
      ? before + placeholder + middle + selection + after
      : before + selection + middle + placeholder + after;
    const contentStart = placeholderFirst
      ? before.length + placeholder.length + middle.length
      : before.length;
    const placeholderStart = placeholderFirst
      ? before.length
      : before.length + selection.length + middle.length;

    let select;
    if (selectPlaceholder && placeholder) {
      select = [placeholderStart, placeholderStart + placeholder.length];
    } else if (selection) {
      select = [contentStart, contentStart + selection.length];
    } else if (placeholder) {
      select = [placeholderStart, placeholderStart + placeholder.length];
    } else {
      select = [contentStart, contentStart];
    }

    return {text, select};
  }

  /* уже процитированная строка становится вложенной цитатой: «> текст» → «>> текст» */
  function quotePrefix(line) {
    return line.startsWith('>') ? '>' : '> ';
  }

  function mention(marker, selection, before, after) {
    const result = surround(marker, '', selection || NickPlaceholder);

    // упоминание распознаётся только в начале текста или после пробела, табуляции, «(»;
    // ник продолжается буквами, цифрами, «_» и «-», поэтому с обеих сторон нужны границы
    if (before && !/[\s(]/.test(before)) {
      result.text = ' ' + result.text;
      result.select = result.select.map((pos) => pos + 1);
    }
    if (after && /[a-z\d_-]/i.test(after)) {
      result.text += ' ';
    }

    return result;
  }

  /* расширяет диапазон до границ строк; завершающий перевод строки в выделение не попадает */
  function expandToLines(value, start, end) {
    const lineStart = start === 0 ? 0 : value.lastIndexOf('\n', start - 1) + 1;

    const endsWithNewline = end > start && value[end - 1] === '\n';
    const newline = endsWithNewline ? end - 1 : value.indexOf('\n', end);
    const lineEnd = newline === -1 ? value.length : newline;

    return {lineStart, lineEnd};
  }

  /* блочная конструкция должна отделяться от текста пустыми строками */
  function isolationAround(value, from, to) {
    const before = value.slice(0, from);
    const after = value.slice(to);

    let lead = '';
    if (before) {
      const blankLineBefore = /\n[ \t]*\n[ \t]*$/.test(before) || /^[ \t]*\n[ \t]*$/.test(before);
      lead = blankLineBefore ? '' : (before.endsWith('\n') ? '\n' : '\n\n');
    }

    let trail = '';
    if (after.trim()) {
      trail = /^\n[ \t]*\n/.test(after) ? '' : (after.startsWith('\n') ? '\n' : '\n\n');
    }

    return {lead, trail};
  }

  /* строки текста с их границами: { from, to }, сам перевод строки в строку не входит */
  function splitLines(value) {
    const lines = [];
    let from = 0;
    for (let newline = value.indexOf('\n'); newline !== -1; newline = value.indexOf('\n', from)) {
      lines.push({from, to: newline});
      from = newline + 1;
    }
    lines.push({from, to: value.length});
    return lines;
  }

  function lineIndexAt(lines, position) {
    return lines.findIndex((line) => position >= line.from && position <= line.to);
  }

  /* строка удаляется вместе с одним соседним переводом строки */
  function removeLine(line, value) {
    if (line.to < value.length) {
      return {from: line.from, to: line.to + 1};
    }
    return {from: Math.max(0, line.from - 1), to: line.to};
  }

  /* позиция парной закрывающей скобки для открывающей в позиции from; limit — граница поиска */
  function matchDelimited(value, from, limit) {
    const close = {'[': ']', '(': ')'}[value[from]];
    let depth = 0;
    for (let i = from; i < limit; i++) {
      if (value[i] === value[from]) {
        depth++;
      } else if (value[i] === close && --depth === 0) {
        return i;
      }
    }
    return -1;
  }

  /* после правки построчных маркеров выделенные строки остаются выделенными, курсор — на месте */
  function lineSelection(ctx) {
    const {start, end, lines, first, last} = ctx;
    return start === end ? [start, start] : [lines[first].from, lines[last].to];
  }

  /*
   * Ищет пару уже расставленных маркеров: выделение включает их краями или лежит между ними,
   * а при пустом выделении курсор стоит между ближайшей парой в пределах строки.
   * Возвращает правку, снимающую маркеры, либо null.
   */
  function findWrapper(ctx, before, after) {
    const symbol = [...before].every((ch) => ch === before[0]) ? before[0] : null;
    return symbol === null ? findTagWrapper(ctx, before, after) : findRunWrapper(ctx, symbol, before.length);
  }

  /* точная пара тегов: [b]…[/b] */
  function findTagWrapper(ctx, before, after) {
    const {value, start, end, lineStart, lineEnd} = ctx;
    const cut = (from, marker) => ({from, to: from + marker.length});

    // края выделения имеют смысл только при непустом выделении: иначе «край» — это текст за курсором
    if (start !== end) {
      // выделение включает теги — целиком или одним краем
      const openCut = value.startsWith(before, start) && start + before.length <= end;
      const closeCut = value.startsWith(after, end - after.length)
        && end - start >= (openCut ? before.length : 0) + after.length;
      if (openCut || closeCut) {
        return {
          edits: [openCut && cut(start, before), closeCut && cut(end - after.length, after)].filter(Boolean),
          select: [start + (openCut ? before.length : 0), end - (closeCut ? after.length : 0)]
        };
      }

      // теги вокруг выделения
      if (value.startsWith(before, start - before.length) && value.startsWith(after, end)) {
        return {edits: [cut(start - before.length, before), cut(end, after)], select: [start, end]};
      }

      return null;
    }

    // курсор внутри тега: снять нечего, а вставка сломает тег
    const bracket = start > lineStart ? value.lastIndexOf('[', start - 1) : -1;
    const bracketEnd = bracket === -1 ? -1 : value.indexOf(']', bracket + 1);
    const insideTag = bracket >= lineStart && bracketEnd >= start && bracketEnd < lineEnd
      && LorTag.test(value.slice(bracket + 1, bracketEnd));
    if (insideTag) return {noop: true};

    // курсор между ближайшей парой
    const closeFrom = value.indexOf(after, end);
    const open = start >= before.length ? value.lastIndexOf(before, start - before.length) : -1;
    if (closeFrom === -1 || closeFrom > lineEnd || open < lineStart) return null;

    return {edits: [cut(open, before), cut(closeFrom, after)], select: [open + before.length, closeFrom]};
  }

  /*
   * Пара маркеров из повторяющегося символа («*», «**», «~~», «`»). Ориентируемся на длину последовательности
   * символов: «***текст***» — жирный и курсив, «**текст**» — только жирный, «*текст*» — только курсив.
   * Снимаются count символов с каждой стороны — те, что прилегают к тексту.
   */
  function findRunWrapper(ctx, symbol, count) {
    const {value, start, end, lineStart, lineEnd} = ctx;
    const isSymbol = (at) => value[at] === symbol;
    const notSpace = (at) => value[at] !== undefined && !/\s/.test(value[at]);

    // длина последовательности символов непосредственно перед позицией и после неё
    const runBefore = (at) => {
      let size = 0;
      while (at - size - 1 >= lineStart && isSymbol(at - size - 1)) size++;
      return size;
    };
    const runAfter = (at) => {
      let size = 0;
      while (at + size < lineEnd && isSymbol(at + size)) size++;
      return size;
    };

    // жирный («**») есть при длине от 2, курсив («*») — при нечётной длине;
    // апостроф должен совпасть по длине: двойные апострофы — другой разделитель кода
    const removable = (size) => {
      if (symbol === '`') return size === count;
      if (symbol === '*') return count === 2 ? size >= 2 : size % 2 === 1;
      return size >= count;
    };

    if (start !== end) {
      // каждой стороне выделения соответствует последовательность символов, которой принадлежит край:
      // целиком снаружи выделения, целиком внутри или разрезанная краем. Снимаем count символов, прилегающих к тексту
      const leftFrom = start - runBefore(start);
      const leftTo = start + runAfter(start);
      const rightFrom = end - runBefore(end);
      const rightTo = end + runAfter(end);

      // выделена только последовательность символов. Если у неё на строке есть пара, это не пустая пара,
      // а ограничитель чужой разметки — его не трогаем. Иначе снимаем пустую пару: по count с краёв
      const onlyRun = leftFrom === start && leftTo === end && rightFrom === start && rightTo === end;
      if (onlyRun) {
        const mark = new RegExp(symbol.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '{' + count + ',}');
        if (mark.test(value.slice(lineStart, start)) || mark.test(value.slice(end, lineEnd))) return {noop: true};

        if (end - start >= 2 * count) {
          return {
            edits: [{from: start, to: start + count}, {from: end - count, to: end}],
            select: [start, end - 2 * count]
          };
        }
      }

      const left = removable(leftTo - leftFrom) && leftTo < end && notSpace(leftTo)
        ? {from: leftTo - count, to: leftTo}
        : null;
      const right = removable(rightTo - rightFrom) && rightFrom > start && notSpace(rightFrom - 1)
        ? {from: rightFrom, to: rightFrom + count}
        : null;

      if (!left && !right) return null;
      if (left && right && left.to > right.from) return null;

      return {edits: [left, right].filter(Boolean), select: [left ? left.to : start, right ? right.from : end]};
    }

    // ближайшая последовательность слева от курсора (её конец не дальше курсора) и справа (начало не раньше)
    let leftEnd = start;
    while (leftEnd > lineStart && !isSymbol(leftEnd - 1)) leftEnd--;
    let rightStart = end;
    while (rightStart < lineEnd && !isSymbol(rightStart)) rightStart++;

    // курсор между символами одной последовательности — внутри чужого маркера: курсор делит её пополам,
    // и половины неотличимы от пустой пары («**текст**» + курсив снял бы закрывающие звезды). Снять нечего,
    // вставка сломает разметку. Собственную вставленную пару снимает removeInsertedPair, набранную вручную — выделение
    if (leftEnd === start && rightStart === end && isSymbol(start - 1) && isSymbol(start)) return {noop: true};

    if (!removable(runBefore(leftEnd)) || !removable(runAfter(rightStart))) return null;

    // по правилам Markdown открывающий маркер не стоит перед пробелом, закрывающий — после
    if (!(notSpace(leftEnd) && notSpace(rightStart - 1))) return null;

    return {
      edits: [{from: leftEnd - count, to: leftEnd}, {from: rightStart, to: rightStart + count}],
      select: [leftEnd, rightStart]
    };
  }

  /*
   * Повторное нажатие снимает только что вставленную пустую пару, если курсор не ушёл с места, —
   * даже когда рядом слиплись чужие маркеры и общий поиск пару не различает.
   */
  function removeInsertedPair({value, start, end}, inserted, command) {
    if (!inserted || inserted.command !== command || inserted.start !== start || inserted.end !== end) return null;
    if (!value.startsWith(inserted.before, start - inserted.before.length) || !value.startsWith(inserted.after, end)) {
      return null;
    }

    return {
      edits: [{from: start - inserted.before.length, to: start}, {from: end, to: end + inserted.after.length}],
      select: [start, end]
    };
  }

  /*
   * Упоминание «@ник» в Markdown: выделение — ник или «@ник» целиком, либо курсор стоит внутри ника
   * (нетронутый плейсхолдер тоже считается). Возвращает правку, снимающую «@», либо null.
   */
  function findMention(ctx) {
    const {value, start, end, lineStart, lineEnd} = ctx;

    // конец слова после «@»
    const wordEnd = (from) => {
      if (value.startsWith(NickPlaceholder, from)) return from + NickPlaceholder.length;
      let to = from;
      while (to < lineEnd && /[a-z\d_-]/i.test(value[to])) to++;
      return to;
    };

    let wordStart = start;
    if (start === end) {
      // курсор внутри слова — идём к его началу
      while (wordStart > lineStart && /[a-z\d_-]/i.test(value[wordStart - 1])) wordStart--;
    }
    const selected = start !== end;
    const from = selected && value[start] === '@' ? start : (selected ? start : wordStart) - 1;
    const wordFrom = from + 1;
    const wordTo = selected ? end : wordEnd(wordFrom);

    const atBoundary = from === 0 || /[\s(]/.test(value[from - 1]);
    if (from < lineStart || value[from] !== '@' || !atBoundary) return null;
    if (wordTo <= wordFrom || wordEnd(wordFrom) !== wordTo) return null;

    return {edits: [{from, to: wordFrom}], select: [wordFrom, wordTo]};
  }

  /*
   * Если внутри снятой разметки остался нетронутый плейсхолдер, снимаем всё вместе с ним:
   * повторное нажатие возвращает текст к виду до первого.
   */
  function dropPlaceholder(removal, {value}, placeholder) {
    if (!removal || !placeholder || value.slice(removal.select[0], removal.select[1]) !== placeholder) {
      return removal;
    }

    const from = Math.min(removal.select[0], ...removal.edits.map((edit) => edit.from));
    const to = Math.max(removal.select[1], ...removal.edits.map((edit) => edit.to));
    return {edits: [{from, to}], select: [from, from]};
  }

  /* ссылка [текст](url) текущей строки, содержащая выделение или курсор целиком */
  function findMarkdownLink(ctx) {
    const {value, start, end, lineStart, lineEnd} = ctx;

    for (let open = value.indexOf('[', lineStart); open !== -1 && open < lineEnd; open = value.indexOf('[', open + 1)) {
      const textEnd = matchDelimited(value, open, lineEnd);
      if (textEnd === -1 || value[textEnd + 1] !== '(') continue;

      const linkEnd = matchDelimited(value, textEnd + 1, lineEnd);
      if (linkEnd === -1 || open > start || end > linkEnd + 1) continue;

      return {edits: [{from: open, to: open + 1}, {from: textEnd, to: linkEnd + 1}], select: [open + 1, textEnd]};
    }

    return null;
  }

  /* ссылка [url=…]текст[/url] или [url]url[/url] текущей строки, содержащая выделение или курсор целиком */
  function findLorcodeLink(ctx) {
    const {value, start, end, lineStart, lineEnd} = ctx;
    const closeTag = '[/url]';
    const opener = /\[url(=[^\]\n]*)?\]/g;
    opener.lastIndex = lineStart;

    let match;
    while ((match = opener.exec(value)) !== null && match.index < lineEnd) {
      const close = value.indexOf(closeTag, opener.lastIndex);
      const closeEnd = close + closeTag.length;
      const inside = match.index <= start && end <= closeEnd;
      if (close === -1 || closeEnd > lineEnd || !inside) continue;

      return {
        edits: [{from: match.index, to: match.index + match[0].length}, {from: close, to: close + closeTag.length}],
        select: [match.index + match[0].length, close]
      };
    }

    return null;
  }

  /* все непустые строки выделения процитированы — снимается один уровень */
  function findQuotedLines(ctx) {
    const {value, lines, first, last} = ctx;
    const edits = [];

    for (let i = first; i <= last; i++) {
      const content = value.slice(lines[i].from, lines[i].to).trimStart();
      const marker = /^> ?/.exec(content);
      if (!content.trim()) continue;
      if (!marker) return null;

      edits.push({from: lines[i].to - content.length, to: lines[i].to - content.length + marker[0].length});
    }

    return edits.length ? {edits, select: lineSelection(ctx)} : null;
  }

  const ListMarker = {
    bullet: /^([ \t]*)[*+-] /,
    ordered: /^([ \t]*)\d+\. /
  };

  /* все непустые строки выделения — элементы списка: того же типа (маркеры снимаются) или другого (заменяются) */
  function findMarkdownList(ctx, ordered) {
    const {value, lines, first, last} = ctx;
    const own = ordered ? ListMarker.ordered : ListMarker.bullet;
    const other = ordered ? ListMarker.bullet : ListMarker.ordered;
    const edits = [];
    let item = 0;

    for (let i = first; i <= last; i++) {
      const text = value.slice(lines[i].from, lines[i].to);
      const ownMarker = own.exec(text);
      const marker = ownMarker || other.exec(text);
      if (!text.trim()) continue;
      if (!marker) return null;

      const edit = {from: lines[i].from + marker[1].length, to: lines[i].from + marker[0].length};
      edits.push(ownMarker ? edit : {...edit, text: ordered ? `${++item}. ` : '* '});
    }

    return edits.length ? {edits, select: lineSelection(ctx)} : null;
  }

  const LorListOpen = /^[ \t]*(\[\*] )?\[list(="[^"]*")?\][ \t]*$/;
  const LorListClose = /^[ \t]*\[\/list\][ \t]*$/;

  /*
   * Идём по строкам от строки from в направлении step и возвращаем строку с незакрытой границей списка:
   * вверх — открывающей «[list]», вниз — закрывающей «[/list]». Вложенные списки пропускаются по глубине.
   */
  function findListBoundary(lineText, from, step) {
    const [inner, wanted] = step < 0 ? [LorListClose, LorListOpen] : [LorListOpen, LorListClose];
    let depth = 0;
    for (let i = from; lineText(i) !== null; i += step) {
      const line = lineText(i);
      if (inner.test(line)) {
        depth++;
      } else if (wanted.test(line)) {
        if (depth === 0) return i;
        depth--;
      }
    }
    return -1;
  }

  /*
   * Выделение или курсор внутри списка: пару «[list]»…«[/list]» ищем вверх и вниз от выделенных строк.
   * Тот же тип — обёртка снимается, другой — меняется открывающий тег.
   */
  function findLorcodeList(ctx, ordered) {
    const {value, lines, first, last, start, end} = ctx;
    const lineText = (i) => (i >= 0 && i < lines.length ? value.slice(lines[i].from, lines[i].to) : null);

    // курсор на строке «[/list]» принадлежит списку, который ею закрывается: его открывающая строка выше
    const open = findListBoundary(lineText, LorListClose.test(lineText(first)) ? first - 1 : first, -1);
    const close = open === -1 ? -1 : findListBoundary(lineText, Math.max(last, open + 1), 1);
    if (open === -1 || close === -1) return null;

    const openText = lineText(open);
    const token = /\[list(="[^"]*")?\]/.exec(openText);
    const openEdit = {from: lines[open].from + token.index, to: lines[open].from + token.index + token[0].length};

    if ((token[1] !== undefined) !== ordered) {
      return {edits: [{...openEdit, text: ordered ? '[list="1"]' : '[list]'}], select: [start, end]};
    }

    // строка «[list]» (или «[*] [list]») и строка «[/list]» убираются целиком
    const edits = [removeLine(lines[open], value), removeLine(lines[close], value)];

    // список внутри другого списка вливается в него — маркеры его элементов остаются;
    // на верхнем уровне маркеры снимаются вместе с обёрткой, остаётся текст
    const nested = findListBoundary(lineText, open - 1, -1) !== -1 && findListBoundary(lineText, close + 1, 1) !== -1;
    if (!nested) {
      let depth = 0;
      for (let i = open + 1; i < close; i++) {
        const line = lineText(i);
        const item = /^([ \t]*)\[\*\] /.exec(line);
        if (depth === 0 && item) {
          edits.push({from: lines[i].from + item[1].length, to: lines[i].from + item[0].length});
        }
        // элементы вложенных списков остаются при своих списках
        if (LorListOpen.test(line)) {
          depth++;
        } else if (LorListClose.test(line)) {
          depth--;
        }
      }
    }

    const bodyFrom = lines[open].to + 1;
    return {edits, select: [bodyFrom, Math.max(bodyFrom, lines[close].from - 1)]};
  }

  /* выделение целиком лежит в блоке, ограждённом строками openRe…closeRe (блок кода) */
  function findFencedBlock(ctx, openRe, closeRe) {
    const {value, lines, first, last} = ctx;
    const text = (i) => value.slice(lines[i].from, lines[i].to).trim();

    for (let i = 0, open = -1; i < lines.length; i++) {
      if (open === -1) {
        if (openRe.test(text(i))) {
          open = i;
        }
      } else if (closeRe.test(text(i))) {
        if (open <= first && last <= i) {
          const bodyFrom = lines[open].to + 1;
          return {
            edits: [removeLine(lines[open], value), removeLine(lines[i], value)],
            select: [bodyFrom, Math.max(bodyFrom, lines[i].from - 1)]
          };
        }
        open = -1;
      }
    }

    return null;
  }

  function createMarkupEditor(textarea, dialect) {
    // insertText вместо присваивания value: правка попадает в историю отмены (Ctrl+Z)
    const replaceRange = (start, end, text) => {
      textarea.focus();
      textarea.setSelectionRange(start, end);

      let inserted = false;
      try {
        inserted = document.execCommand('insertText', false, text);
      } catch (_error) {
        inserted = false;
      }

      if (!inserted) {
        textarea.setRangeText(text, start, end, 'end');
      }
    };

    // последняя вставленная пустая пара маркеров — для снятия повторным нажатием
    let insertedPair = null;

    const applyInline = (spec, command) => {
      const value = textarea.value;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const {text, select} = spec.insert(value.slice(start, end), start > 0 ? value[start - 1] : '', value[end] || '');

      replaceRange(start, end, text);
      textarea.setSelectionRange(start + select[0], start + select[1]);

      const [before, after] = spec.markers || [];
      insertedPair = start === end && before && select[0] === select[1]
        ? {command, start: start + select[0], end: start + select[1], before, after}
        : null;
    };

    const applyLinePrefix = (makePrefix) => {
      const value = textarea.value;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const {lineStart, lineEnd} = expandToLines(value, start, end);
      const {lead, trail} = isolationAround(value, lineStart, lineEnd);
      const block = value.slice(lineStart, lineEnd);

      let text;
      if (start === end) {
        text = makePrefix(0, block) + block;
      } else {
        let item = 0;
        text = block.split('\n').map((line) => line ? makePrefix(item++, line) + line : line).join('\n');
      }

      const blockStart = lineStart + lead.length;
      replaceRange(lineStart, lineEnd, lead + text + trail);

      if (start === end) {
        // курсор остаётся на прежнем месте внутри строки
        const caret = blockStart + makePrefix(0, block).length + (start - lineStart);
        textarea.setSelectionRange(caret, caret);
      } else {
        textarea.setSelectionRange(blockStart, blockStart + text.length);
      }
    };

    const applyBlock = ({parts: [open, itemPrefix, close], wrapLine}) => {
      const value = textarea.value;
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;

      // wrapLine: без выделения текущая непустая строка становится элементом (список),
      // иначе вставляется пустой шаблон (блок кода)
      const caretLine = expandToLines(value, start, end);
      const caretLineText = value.slice(caretLine.lineStart, caretLine.lineEnd);
      const wrapCaretLine = start === end && wrapLine && Boolean(caretLineText.trim());

      let from;
      let to;
      let text;
      let select;

      if (start === end && !wrapCaretLine) {
        from = to = start;
        text = open + '\n' + itemPrefix + '\n' + close;
        const caret = from + open.length + 1 + itemPrefix.length;
        select = [caret, caret];
      } else {
        const {lineStart, lineEnd} = wrapCaretLine ? caretLine : expandToLines(value, start, end);
        from = lineStart;
        to = lineEnd;
        const items = value.slice(from, to).split('\n').map((line) => (line ? itemPrefix + line : line)).join('\n');
        text = open + '\n' + items + '\n' + close;
        if (wrapCaretLine) {
          // курсор остаётся на прежнем месте внутри строки
          const caret = from + open.length + 1 + itemPrefix.length + (start - from);
          select = [caret, caret];
        } else {
          select = [from, from + text.length];
        }
      }

      const {lead, trail} = isolationAround(value, from, to);
      replaceRange(from, to, lead + text + trail);
      textarea.setSelectionRange(select[0] + lead.length, select[1] + lead.length);
    };

    /*
     * Снятие разметки: все правки выполняются одной заменой текста, то есть одним шагом отмены.
     * select задан в координатах исходного текста и пересчитывается на сдвиги от правок.
     */
    const applyEdits = ({value}, removal) => {
      const edits = [...removal.edits].sort((a, b) => a.from - b.from);
      const from = edits[0].from;
      const to = edits[edits.length - 1].to;

      let text = '';
      let position = from;
      for (const edit of edits) {
        text += value.slice(position, edit.from) + (edit.text || '');
        position = edit.to;
      }
      text += value.slice(position, to);

      replaceRange(from, to, text);

      const shift = (pos) => pos - edits.reduce((delta, edit) =>
        edit.to <= pos ? delta + (edit.to - edit.from) - (edit.text || '').length : delta, 0);
      textarea.setSelectionRange(shift(removal.select[0]), shift(removal.select[1]));
    };

    return {
      run(command) {
        const spec = dialect.inline[command] || dialect.prefix[command] || dialect.block[command];
        if (!spec) return;

        const value = textarea.value;
        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        const {lineStart, lineEnd} = expandToLines(value, start, end);
        const lines = splitLines(value);
        const ctx = {
          value, start, end, lineStart, lineEnd, lines,
          first: lineIndexAt(lines, lineStart),
          last: lineIndexAt(lines, Math.max(start, end - 1))
        };

        const removal = removeInsertedPair(ctx, insertedPair, command) || spec.remove(ctx);

        // noop: курсор внутри чужого маркера — и снять нечего, и вставлять нельзя; текст не меняется,
        // поэтому память о вставленной паре сохраняется
        if (removal && removal.noop) return;
        insertedPair = null;

        if (removal) {
          applyEdits(ctx, removal);
        } else if (dialect.inline[command]) {
          applyInline(spec, command);
        } else if (dialect.prefix[command]) {
          applyLinePrefix(spec.make);
        } else {
          applyBlock(spec);
        }
      }
    };
  }

  /* «User line break» (ntobr) отличается от LORCODE только обработкой переводов строк */
  function dialect(formatMode) {
    if (formatMode === 'ntobr') return TOOLBAR_DIALECTS.lorcode;
    return Object.hasOwn(TOOLBAR_DIALECTS, formatMode) ? TOOLBAR_DIALECTS[formatMode] : null;
  }

  root.MarkupEditor = {dialect, createEditor: createMarkupEditor};
})(typeof globalThis !== 'undefined' ? globalThis : this);
