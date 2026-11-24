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

const UNSAVED_WARNING = "Вы что-то напечатали в форме. Все введенные данные будут потеряны при закрытии страницы.";
const SWITCH_REPLY_WARNING = "Вы уже вводите комментарий. При ответе на другое сообщение набранный текст будет потерян. Переключиться?";
const PREVIEW_REQUEST_ERROR = "Не удалось выполнить запрос, попробуйте повторить еще раз.";

function getCsrf() {
  const cookies = Object.fromEntries(
    document.cookie.split('; ').map(c => {
      const [key, ...val] = c.split('=');
      return [key, val.join('=')];
    })
  );
  return (cookies["CSRF_TOKEN"] || '').replace(/(^")|("$)/g, "");
}

window.setupFormWithSpinner = function(options) {
  const startSpinner = ($form) => {
    const spinner = $("<i class='icon-spin spinner' style='margin-left: 0.5em'>");
    $form.find(".form-actions .btn").last().after(spinner);
  };

  $script.ready("plugins", function() {
    $(function() {
      const $form = $(options.formSelector);
      const $textarea = $(options.textareaSelector);
      let submitted = false;

      initPreviewTabs($form[0]);

      const warnOnUnload = (e) => {
        if ($textarea.val() !== $textarea[0].defaultValue && !submitted) {
          e.preventDefault();
          e.returnValue = UNSAVED_WARNING;
          return e.returnValue;
        }
      };

      window.addEventListener('beforeunload', warnOnUnload);

      let clickedButton = null;
      $form.find(".form-actions button[type=submit]").on('click', function() {
        clickedButton = this;
      });

      const validateOpts = {
        ...options.validateOptions,
        submitHandler: (form) => {
          if (submitted) { return; }
          submitted = true;
          window.removeEventListener('beforeunload', warnOnUnload);

          if (clickedButton && clickedButton.name) {
            $(form).append($('<input>').attr({
              type: 'hidden', name: clickedButton.name, value: $(clickedButton).val() || clickedButton.name
            }));
          }

          startSpinner($form);
          $form.find(".form-actions button").prop("disabled", true);
          form.submit();
        }
      };

      $form.validate(validateOpts);
    });
  });
};

/*
 * Иконки тулбара — Primer Octicons 19.42.0 (MIT License, Copyright (c) GitHub Inc.)
 */
const TOOLBAR_ICONS = {
  bold: '<path d="M4 2h4.5a3.501 3.501 0 0 1 2.852 5.53A3.499 3.499 0 0 1 9.5 14H4a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1Zm1 7v3h4.5a1.5 1.5 0 0 0 0-3Zm3.5-2a1.5 1.5 0 0 0 0-3H5v3Z"/>',
  italic: '<path d="M6 2.75A.75.75 0 0 1 6.75 2h6.5a.75.75 0 0 1 0 1.5h-2.505l-3.858 9H9.25a.75.75 0 0 1 0 1.5h-6.5a.75.75 0 0 1 0-1.5h2.505l3.858-9H6.75A.75.75 0 0 1 6 2.75Z"/>',
  strike: '<path d="M11.055 8.5c.524.536.815 1.257.811 2.007a3.133 3.133 0 0 1-1.12 2.408C9.948 13.597 8.748 14 7.096 14c-1.706 0-3.104-.607-3.902-1.377a.751.751 0 0 1 1.042-1.079c.48.463 1.487.956 2.86.956 1.422 0 2.232-.346 2.676-.726.435-.372.594-.839.594-1.267 0-.472-.208-.857-.647-1.197-.448-.346-1.116-.623-1.951-.81H1.75a.75.75 0 0 1 0-1.5h12.5a.75.75 0 0 1 0 1.5ZM7.581 3.25c-2.036 0-2.778 1.082-2.778 1.786 0 .055.002.107.006.157a.75.75 0 0 1-1.496.114 3.506 3.506 0 0 1-.01-.271c0-1.832 1.75-3.286 4.278-3.286 1.418 0 2.721.58 3.514 1.093a.75.75 0 1 1-.814 1.26c-.64-.414-1.662-.853-2.7-.853Z"/>',
  code: '<path d="m11.28 3.22 4.25 4.25a.75.75 0 0 1 0 1.06l-4.25 4.25a.749.749 0 0 1-1.275-.326.749.749 0 0 1 .215-.734L13.94 8l-3.72-3.72a.749.749 0 0 1 .326-1.275.749.749 0 0 1 .734.215Zm-6.56 0a.751.751 0 0 1 1.042.018.751.751 0 0 1 .018 1.042L2.06 8l3.72 3.72a.749.749 0 0 1-.326 1.275.749.749 0 0 1-.734-.215L.47 8.53a.75.75 0 0 1 0-1.06Z"/>',
  codeBlock: '<path d="M5.97 5.72a.75.75 0 1 1 1.06 1.06L5.81 8l1.22 1.22a.75.75 0 1 1-1.06 1.06L4.22 8.53a.75.75 0 0 1 0-1.06zm3 0a.75.75 0 0 1 1.06 0l1.75 1.75a.75.75 0 0 1 0 1.06l-1.75 1.75a.75.75 0 1 1-1.06-1.06L10.19 8 8.97 6.78a.75.75 0 0 1 0-1.06"/><path fill-rule="evenodd" d="M13.25 1c.966 0 1.75.784 1.75 1.75v10.5A1.75 1.75 0 0 1 13.25 15H2.75A1.75 1.75 0 0 1 1 13.25V2.75C1 1.784 1.784 1 2.75 1zM2.75 2.5a.25.25 0 0 0-.25.25v10.5c0 .138.112.25.25.25h10.5a.25.25 0 0 0 .25-.25V2.75a.25.25 0 0 0-.25-.25z"/>',
  link: '<path d="m7.775 3.275 1.25-1.25a3.5 3.5 0 1 1 4.95 4.95l-2.5 2.5a3.5 3.5 0 0 1-4.95 0 .751.751 0 0 1 .018-1.042.751.751 0 0 1 1.042-.018 1.998 1.998 0 0 0 2.83 0l2.5-2.5a2.002 2.002 0 0 0-2.83-2.83l-1.25 1.25a.751.751 0 0 1-1.042-.018.751.751 0 0 1-.018-1.042Zm-4.69 9.64a1.998 1.998 0 0 0 2.83 0l1.25-1.25a.751.751 0 0 1 1.042.018.751.751 0 0 1 .018 1.042l-1.25 1.25a3.5 3.5 0 1 1-4.95-4.95l2.5-2.5a3.5 3.5 0 0 1 4.95 0 .751.751 0 0 1 .018 1.042.751.751 0 0 1-1.042.018 1.998 1.998 0 0 0-2.83 0l-2.5 2.5a1.998 1.998 0 0 0 0 2.83Z"/>',
  quote: '<path d="M1.75 2.5h10.5a.75.75 0 0 1 0 1.5H1.75a.75.75 0 0 1 0-1.5Zm4 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5Zm0 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5ZM2.5 7.75v6a.75.75 0 0 1-1.5 0v-6a.75.75 0 0 1 1.5 0Z"/>',
  user: '<path d="M8 .5a7.499 7.499 0 0 1 7.499 7.462l.002.038v1.164a2.612 2.612 0 0 1-4.783 1.454A3.763 3.763 0 0 1 8 11.776 3.776 3.776 0 1 1 11.776 8v1.164a1.112 1.112 0 0 0 2.225 0L14 8a6 6 0 1 0-3.311 5.365.75.75 0 0 1 .673 1.341A7.5 7.5 0 1 1 8 .5Zm0 5.225a2.275 2.275 0 1 0 0 4.552 2.275 2.275 0 0 0 0-4.552Z"/>',
  list: '<path d="M5.75 2.5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5Zm0 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5Zm0 5h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1 0-1.5ZM2 14a1 1 0 1 1 0-2 1 1 0 0 1 0 2Zm1-6a1 1 0 1 1-2 0 1 1 0 0 1 2 0ZM2 4a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z"/>',
  orderedList: '<path d="M5 3.25a.75.75 0 0 1 .75-.75h8.5a.75.75 0 0 1 0 1.5h-8.5A.75.75 0 0 1 5 3.25Zm0 5a.75.75 0 0 1 .75-.75h8.5a.75.75 0 0 1 0 1.5h-8.5A.75.75 0 0 1 5 8.25Zm0 5a.75.75 0 0 1 .75-.75h8.5a.75.75 0 0 1 0 1.5h-8.5a.75.75 0 0 1-.75-.75ZM.924 10.32a.5.5 0 0 1-.851-.525l.001-.001.001-.002.002-.004.007-.011c.097-.144.215-.273.348-.384.228-.19.588-.392 1.068-.392.468 0 .858.181 1.126.484.259.294.377.673.377 1.038 0 .987-.686 1.495-1.156 1.845l-.047.035c-.303.225-.522.4-.654.597h1.357a.5.5 0 0 1 0 1H.5a.5.5 0 0 1-.5-.5c0-1.005.692-1.52 1.167-1.875l.035-.025c.531-.396.8-.625.8-1.078a.57.57 0 0 0-.128-.376C1.806 10.068 1.695 10 1.5 10a.658.658 0 0 0-.429.163.835.835 0 0 0-.144.153ZM2.003 2.5V6h.503a.5.5 0 0 1 0 1H.5a.5.5 0 0 1 0-1h.503V3.308l-.28.14a.5.5 0 0 1-.446-.895l1.003-.5a.5.5 0 0 1 .723.447Z"/>'
};

const TOOLBAR_BUTTONS = [
  {command: 'bold', label: 'Жирный', hotkey: 'Ctrl+B'},
  {command: 'italic', label: 'Курсив', hotkey: 'Ctrl+I'},
  {command: 'strike', label: 'Зачёркнутый'},
  {command: 'code', label: 'Код'},
  {command: 'codeBlock', label: 'Блок кода'},
  {command: 'link', label: 'Ссылка'},
  {command: 'quote', label: 'Цитата'},
  {command: 'user', label: 'Упоминание пользователя'},
  {command: 'list', label: 'Список'},
  {command: 'orderedList', label: 'Нумерованный список'}
];

/* физический код клавиши: работает независимо от раскладки */
const TOOLBAR_HOTKEYS = {
  KeyB: 'bold',
  KeyI: 'italic'
};

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

/*
 * Тулбар кнопок разметки. Создаётся только скриптом и вставляется сразу после вкладок:
 * без JavaScript кнопок нет в DOM.
 */
function initMarkupToolbar(nav, textarea, formatMode) {
  // «User line break» (ntobr) отличается от LORCODE только обработкой переводов строк
  const dialect = formatMode === 'ntobr' ? TOOLBAR_DIALECTS.lorcode : TOOLBAR_DIALECTS[formatMode];
  if (!dialect) return null;

  const editor = createMarkupEditor(textarea, dialect);

  const toolbar = document.createElement('div');
  toolbar.className = 'markup-toolbar';
  toolbar.setAttribute('role', 'toolbar');
  toolbar.setAttribute('aria-label', 'Разметка');

  TOOLBAR_BUTTONS.forEach(({command, label, hotkey}, index) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'markup-toolbar__btn';
    button.dataset.command = command;
    button.title = hotkey ? `${label} (${hotkey})` : label;
    button.setAttribute('aria-label', button.title);
    button.tabIndex = index === 0 ? 0 : -1;
    button.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16"'
      + ' aria-hidden="true">' + TOOLBAR_ICONS[command] + '</svg>';
    toolbar.appendChild(button);
  });

  // клик по кнопке не должен уводить фокус из textarea: иначе выделение потеряется
  toolbar.addEventListener('mousedown', (e) => e.preventDefault());

  toolbar.addEventListener('click', (e) => {
    const button = e.target.closest('.markup-toolbar__btn');
    if (button) {
      editor.run(button.dataset.command);
    }
  });

  // перемещение между кнопками стрелками (role="toolbar"); стрелки с модификаторами не трогаем —
  // Ctrl+стрелки на сайте заняты навигацией по страницам
  toolbar.addEventListener('keydown', (e) => {
    if (e.ctrlKey || e.metaKey || e.altKey || e.shiftKey) return;

    const buttons = Array.from(toolbar.children);
    const current = buttons.indexOf(document.activeElement);
    if (current === -1) return;

    let next = null;
    if (e.key === 'ArrowRight') {
      next = (current + 1) % buttons.length;
    } else if (e.key === 'ArrowLeft') {
      next = (current - 1 + buttons.length) % buttons.length;
    } else if (e.key === 'Home') {
      next = 0;
    } else if (e.key === 'End') {
      next = buttons.length - 1;
    }

    if (next !== null) {
      e.preventDefault();
      buttons[current].tabIndex = -1;
      buttons[next].tabIndex = 0;
      buttons[next].focus();
    }
  });

  textarea.addEventListener('keydown', (e) => {
    if (!(e.ctrlKey || e.metaKey) || e.altKey || e.shiftKey) return;

    const command = TOOLBAR_HOTKEYS[e.code];
    if (command) {
      e.preventDefault();
      editor.run(command);
    }
  });

  nav.after(toolbar);

  /*
   * «Отдельный ряд под вкладками» определяем по фактической раскладке, а не по ширине окна:
   * узкая форма ответа переносит тулбар и на широком экране. Перед замером модификатор снимаем —
   * он сам растягивает ряд на всю ширину, иначе обратного перехода в ряд вкладок не будет.
   */
  const syncLayout = () => {
    toolbar.classList.remove('markup-toolbar--stacked');

    const navRect = nav.getBoundingClientRect();
    const toolbarRect = toolbar.getBoundingClientRect();
    const stacked = (toolbarRect.top + toolbarRect.bottom) / 2 > navRect.bottom;

    toolbar.classList.toggle('markup-toolbar--stacked', stacked);
  };

  syncLayout();

  if (typeof ResizeObserver !== 'undefined') {
    const layoutObserver = new ResizeObserver(syncLayout);
    layoutObserver.observe(nav);
    layoutObserver.observe(toolbar);
  } else {
    window.addEventListener('resize', syncLayout);
  }

  return toolbar;
}

function initPreviewTabs(formElement) {
  const formatGroup = formElement.querySelector('[data-format-mode]');
  if (!formatGroup) return;

  const formatMode = formatGroup.dataset.formatMode;
  const textarea = formatGroup.querySelector('textarea');
  if (!textarea) return;
  if (textarea.readOnly) return;

  const nav = formatGroup.querySelector('.markup-tabs__nav');
  if (!nav) return;

  const panelsContainer = formatGroup.querySelector('.markup-tabs__content');
  if (!panelsContainer) return;

  const editorTab = nav.querySelector('[data-tab="editor"]');
  if (!editorTab) return;

  const editorPanel = panelsContainer.querySelector('[data-panel="editor"]');
  if (!editorPanel) return;

  const previewTab = document.createElement('li');
  previewTab.className = 'markup-tabs__tab';
  previewTab.dataset.tab = 'preview';
  previewTab.textContent = 'Предпросмотр';
  nav.appendChild(previewTab);

  const previewPanel = document.createElement('div');
  previewPanel.className = 'markup-tabs__panel';
  previewPanel.dataset.panel = 'preview';
  const previewContent = document.createElement('div');
  previewContent.className = 'markup-preview';
  previewPanel.appendChild(previewContent);
  panelsContainer.appendChild(previewPanel);

  const uid = 'mt-' + (window.crypto?.randomUUID?.() ?? Math.random().toString(36).slice(2));
  editorTab.id = editorTab.id || (uid + '-editor-tab');
  editorPanel.id = editorPanel.id || (uid + '-editor-panel');
  previewTab.id = uid + '-preview-tab';
  previewPanel.id = uid + '-preview-panel';

  nav.setAttribute('role', 'tablist');

  editorTab.setAttribute('role', 'tab');
  editorTab.setAttribute('aria-controls', editorPanel.id);
  editorPanel.setAttribute('role', 'tabpanel');
  editorPanel.setAttribute('aria-labelledby', editorTab.id);

  previewTab.setAttribute('role', 'tab');
  previewTab.setAttribute('aria-controls', previewPanel.id);
  previewPanel.setAttribute('role', 'tabpanel');
  previewPanel.setAttribute('aria-labelledby', previewTab.id);

  const previewButton = formElement.querySelector('button[name=preview]');
  if (previewButton) {
    previewButton.classList.add('preview-button-js-hidden');
  }

  const toolbar = initMarkupToolbar(nav, textarea, formatMode);

  let textareaHeight = 0;

  const tabs = () => Array.from(nav.querySelectorAll('.markup-tabs__tab'));

  const syncAria = (activeTab) => {
    tabs().forEach(t => {
      const isActive = t === activeTab;
      t.setAttribute('aria-selected', isActive ? 'true' : 'false');
      t.setAttribute('tabindex', isActive ? '0' : '-1');
    });
  };

  syncAria(editorTab);

  const switchTab = (tabName, options = {}) => {
    textareaHeight = textarea.offsetHeight;

    nav.querySelectorAll('.markup-tabs__tab').forEach(t => t.classList.remove('active'));
    panelsContainer.querySelectorAll('.markup-tabs__panel').forEach(p => p.classList.remove('active'));
    if (toolbar) {
      toolbar.hidden = tabName !== 'editor';
    }

    if (tabName === 'editor') {
      editorTab.classList.add('active');
      editorPanel.classList.add('active');
      previewContent.style.minHeight = '';
      syncAria(editorTab);
      if (options.focus !== false) {
        textarea.focus();
      }
    } else if (tabName === 'preview') {
      previewTab.classList.add('active');
      previewPanel.classList.add('active');
      previewContent.style.minHeight = Math.max(textareaHeight, 50) + 'px';
      syncAria(previewTab);
      loadPreview();
      if (options.focusTab) {
        previewTab.focus();
      }
    }
  };

  const resetPreviewContent = () => {
    previewContent.classList.remove('markup-preview--error');
    previewContent.textContent = '';
  };

  const showPreviewError = (message, retryable) => {
    resetPreviewContent();
    previewContent.classList.add('markup-preview--error');

    const errorText = document.createElement('div');
    errorText.className = 'markup-preview__error-text';
    errorText.setAttribute('role', 'alert');
    errorText.textContent = message;
    previewContent.appendChild(errorText);

    if (retryable) {
      const refreshButton = document.createElement('button');
      refreshButton.type = 'button';
      refreshButton.className = 'btn btn-default';
      refreshButton.textContent = 'Обновить';
      refreshButton.addEventListener('click', () => loadPreview());
      previewContent.appendChild(refreshButton);
      refreshButton.focus();
    }
  };

  const loadPreview = () => {
    const text = textarea.value;
    if (!text.trim()) {
      resetPreviewContent();
      return;
    }

    resetPreviewContent();
    previewContent.textContent = 'Загрузка...';

    const formData = new URLSearchParams();
    formData.append('text', text);
    formData.append('markup', formatMode);
    const csrf = getCsrf();
    if (csrf) {
      formData.append('csrf', csrf);
    }

    fetch('/markup/preview', {
      method: 'POST',
      headers: {'Content-Type': 'application/x-www-form-urlencoded'},
      body: formData.toString()
    })
    .then(response => {
      if (!response.ok) {
        showPreviewError(PREVIEW_REQUEST_ERROR, true);
        return;
      }
      return response.json();
    })
    .then(data => {
      if (!data) {
        return;
      }
      if (data.error) {
        showPreviewError(data.error, false);
      } else {
        resetPreviewContent();
        previewContent.innerHTML = data.html;
        previewContent.querySelectorAll('a').forEach(a => a.setAttribute('target', '_blank'));
        $script.ready('hljs', function() {
          previewContent.querySelectorAll('pre code').forEach(block => {
            hljs.highlightBlock(block);
          });
        });
      }
    })
    .catch((_error) => {
      showPreviewError(PREVIEW_REQUEST_ERROR, true);
    });
  };

  nav.addEventListener('click', (e) => {
    const tab = e.target.closest('.markup-tabs__tab');
    if (!tab || tab.classList.contains('active')) return;
    e.preventDefault();
    switchTab(tab.dataset.tab);
  });

  nav.addEventListener('keydown', (e) => {
    const list = tabs();
    if (!list.length) return;
    const cur = list.findIndex(t => t === document.activeElement);
    if (cur === -1) return;

    if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      e.preventDefault();
      const delta = e.key === 'ArrowRight' ? 1 : -1;
      const next = list[(cur + delta + list.length) % list.length];
      switchTab(next.dataset.tab, {focusTab: next === previewTab});
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      const tab = e.target.closest('.markup-tabs__tab');
      if (tab && !tab.classList.contains('active')) {
        switchTab(tab.dataset.tab, {focusTab: tab === previewTab});
      }
    }
  });

  formElement._switchTab = switchTab;
}

$script.ready('jquery', function() {
  'use strict';

  if (window._formWithSpinnerActive) {
    return;
  }

  $(function() {
    const commentForm = $("#commentForm");
    if (!commentForm.length) {
      return;
    }

    const startSpinner = ($form) => {
      const spinner = $("<i class='icon-spin spinner' style='margin-left: 0.5em'>");
      $form.find(".form-actions .btn").last().after(spinner);
    };

    const stopSpinner = ($form) => {
      $form.find(".spinner").remove();
    };

    const clearErrors = ($form) => {
      $form.find("div[error]").remove();
    };

    const commentFormContainer = commentForm.parent();
    const isInline = commentFormContainer.is(':hidden');
    const INLINE_FORM_CLASS = 'comment-form-inline';
    const INLINE_FORM_VISIBLE_CLASS = 'comment-form-inline-visible';

    const csrf = getCsrf();

    let captchaLoaded = false;
    let captchaRendered = false;

    const showCaptchaLoading = (lazyCaptcha) => {
      lazyCaptcha.classList.add('captcha-state');
      lazyCaptcha.innerHTML = '<div class="captcha-loading"><i class="icon-spin spinner"></i>Капча загружается…</div>';
    };

    const showCaptchaError = (lazyCaptcha) => {
      lazyCaptcha.classList.remove('captcha-state');
      lazyCaptcha.innerHTML = '<div class="captcha-error">Не удалось загрузить капчу (сетевая ошибка). <a href="#">Повторить</a></div>';
      $('a', lazyCaptcha).on('click', (e) => {
        e.preventDefault();
        loadCaptcha();
      });
    };

    const loadCaptcha = () => {
      const lazyCaptcha = commentFormContainer.find('[data-lazy-captcha]')[0];
      if (!lazyCaptcha || captchaLoaded) {
        return;
      }

      captchaLoaded = true;
      const sitekey = lazyCaptcha.getAttribute('data-sitekey');
      showCaptchaLoading(lazyCaptcha);

      const renderCaptcha = () => {
        if (!captchaRendered) {
          captchaRendered = true;
          let timer = null;
          let done = false;
          const hideLoading = () => {
            if (done) {
              return;
            }
            done = true;
            clearTimeout(timer);
            window.removeEventListener('message', onCaptchaMessage);
            $('.captcha-loading', lazyCaptcha).remove();
            lazyCaptcha.classList.remove('captcha-state');
          };
          // hCaptcha сообщает о готовности чекбокса через postMessage (label: checkbox-loaded);
          // событие load у iframe стреляет заметно раньше реальной отрисовки
          const onCaptchaMessage = (e) => {
            if (typeof e.data !== 'string') {
              return;
            }
            try {
              const msg = JSON.parse(e.data);
              if (msg && msg.source === 'hcaptcha' && msg.label === 'checkbox-loaded') {
                hideLoading();
              }
            } catch (_err) {
              // сообщение не от hCaptcha
            }
          };
          window.addEventListener('message', onCaptchaMessage);
          // страховка: если событие готовности так и не придёт
          timer = setTimeout(hideLoading, 20000);
          try {
            hcaptcha.render(lazyCaptcha, {sitekey: sitekey});
          } catch (_err) {
            hideLoading(); // hCaptcha покажет собственный блок ошибки
          }
          lazyCaptcha.removeAttribute('data-lazy-captcha');
        }
      };

      if (typeof hcaptcha !== 'undefined') {
        renderCaptcha();
        return;
      }

      const callbackName = 'onCaptchaLoaded';
      window[callbackName] = () => {
        renderCaptcha();
        delete window[callbackName];
      };
      const script = document.createElement('script');
      script.src = 'https://js.hcaptcha.com/1/api.js?render=explicit&onload=' + callbackName;
      script.async = true;
      script.onerror = () => {
        delete window[callbackName];
        captchaLoaded = false;
        showCaptchaError(lazyCaptcha);
      };
      document.head.appendChild(script);
    };

    const resetCaptcha = () => {
      if (captchaRendered && typeof hcaptcha !== 'undefined') {
        hcaptcha.reset?.();
      }
    };

    const updateCsrf = () => {
      if (csrf) {
        $("input[name='csrf']", commentForm).val(csrf);
      }
    };

    let closeInlineForm = () => {};

    if (isInline) {
      let inlineFormTransitionListener = null;
      let inlineFormAnimationFrame = null;

      const prepareInlineFormContainer = () => {
        const container = commentFormContainer[0];
        if (!container) {
          return;
        }

        commentFormContainer.addClass(INLINE_FORM_CLASS);
        container.hidden = true;
        container.style.removeProperty('display');
        container.style.maxHeight = '0px';
      };

      const removeInlineTransitionListener = () => {
        const container = commentFormContainer[0];
        if (!container || !inlineFormTransitionListener) {
          return;
        }

        container.removeEventListener('transitionend', inlineFormTransitionListener);
        inlineFormTransitionListener = null;
      };

      const cancelInlineAnimationFrame = () => {
        if (inlineFormAnimationFrame === null) {
          return;
        }

        cancelAnimationFrame(inlineFormAnimationFrame);
        inlineFormAnimationFrame = null;
      };

      const isInlineFormVisible = () => commentFormContainer.hasClass(INLINE_FORM_VISIBLE_CLASS);

      const openInlineForm = () => {
        const container = commentFormContainer[0];
        if (!container) {
          return;
        }

        removeInlineTransitionListener();
        cancelInlineAnimationFrame();
        container.hidden = false;
        const targetHeight = container.scrollHeight;
        commentFormContainer.addClass(INLINE_FORM_VISIBLE_CLASS);

        inlineFormTransitionListener = (event) => {
          if (event.target !== container || event.propertyName !== 'max-height' || !isInlineFormVisible()) {
            return;
          }

          container.style.maxHeight = 'none';
          removeInlineTransitionListener();
        };

        container.addEventListener('transitionend', inlineFormTransitionListener);
        inlineFormAnimationFrame = requestAnimationFrame(() => {
          inlineFormAnimationFrame = null;
          container.style.maxHeight = targetHeight + 'px';
        });

        return targetHeight;
      };

      closeInlineForm = ({immediate = false} = {}) => {
        const container = commentFormContainer[0];
        if (!container) {
          return;
        }

        removeInlineTransitionListener();
        cancelInlineAnimationFrame();

        if (immediate) {
          commentFormContainer.removeClass(INLINE_FORM_VISIBLE_CLASS);
          container.hidden = true;
          container.style.maxHeight = '0px';
          return;
        }

        inlineFormTransitionListener = (event) => {
          if (event.target !== container || event.propertyName !== 'max-height' || isInlineFormVisible()) {
            return;
          }

          container.hidden = true;
          container.style.maxHeight = '0px';
          removeInlineTransitionListener();
        };

        container.addEventListener('transitionend', inlineFormTransitionListener);
        container.style.maxHeight = container.scrollHeight + 'px';
        inlineFormAnimationFrame = requestAnimationFrame(() => {
          inlineFormAnimationFrame = null;
          commentFormContainer.removeClass(INLINE_FORM_VISIBLE_CLASS);
          container.style.maxHeight = '0px';
        });
      };

      prepareInlineFormContainer();

      const updateAuthorReadonlyNote = (authorReadonly) => {
        $('#author-readonly-note').text(
          authorReadonly
            ? "Внимание! Вы отвечаете на комментарий, автора которого не может создавать новые комментарии в этом топике."
            : ""
        );
      };

      const REPLY_TYPE = 1;
      const TOPIC_TYPE = 0;

      const extractQuoteFromRange = (range, bodyEl) => {
        if (!range.intersectsNode(bodyEl)) return null;

        const bodyRange = document.createRange();
        bodyRange.selectNodeContents(bodyEl);

        const clippedRange = range.cloneRange();
        if (clippedRange.compareBoundaryPoints(Range.START_TO_START, bodyRange) < 0) {
          clippedRange.setStart(bodyRange.startContainer, bodyRange.startOffset);
        }
        if (clippedRange.compareBoundaryPoints(Range.END_TO_END, bodyRange) > 0) {
          clippedRange.setEnd(bodyRange.endContainer, bodyRange.endOffset);
        }

        const text = clippedRange.toString().replace(/\r\n?/g, '\n').replace(/\n+/g, '\n\n').trim();
        if (!text) return null;
        return text.split('\n').map(line => '> ' + line).join('\n');
      };

      const getQuoteText = (element) => {
        if (!element) return null;
        const bodyEl = element.querySelector('.msg-text') || element;
        const selection = window.getSelection();
        if (!selection || !selection.rangeCount) return null;
        const quote = extractQuoteFromRange(selection.getRangeAt(0), bodyEl);
        return quote;
      };

      const bindReplyLink = (link, getElement, onClick) => {
        let pendingQuoteText = null;

        link.on('pointerdown', () => {
          pendingQuoteText = getQuoteText(getElement());
        });

        link.on('click', (e) => {
          e.preventDefault();
          const element = getElement();
          const quoteText = getQuoteText(element) || (e.originalEvent?.detail ? pendingQuoteText : null);
          pendingQuoteText = null;
          onClick(quoteText);
        });
      };

      const moveAndShowForm = (selector, replyToValue, quoteText) => {
        const replyTo = $("input[name='replyto']", commentFormContainer);
        if (replyTo.val() !== String(replyToValue)) {
          commentForm.find("#msg").val('');
          clearErrors(commentForm);
          resetCaptcha();
          commentForm[0]._switchTab?.('editor', {focus: false});
          closeInlineForm({immediate: true});
        }

        if (!isInlineFormVisible()) {
          const reply = $('div.reply', $('div.msg_body', $(selector)));
          reply.after(commentFormContainer);
          replyTo.val(replyToValue);

          if (!commentForm[0].dataset.previewTabsInitialized) {
            initPreviewTabs(commentForm[0]);
            commentForm[0].dataset.previewTabsInitialized = 'true';
          }

          let quoteCursorPos = null;
          if (quoteText) {
            const textarea = document.getElementById('msg');
            if (textarea) {
              const currentValue = textarea.value;
              let newValue;
              if (currentValue.trim()) {
                newValue = currentValue.trimEnd() + '\n\n' + quoteText + '\n\n';
              } else {
                newValue = quoteText + '\n\n';
              }
              textarea.value = newValue;
              quoteCursorPos = newValue.length;
            }
          }

          loadCaptcha();
          const formHeight = openInlineForm();

          const msgEl = document.getElementById('msg');
          if (msgEl) {
            try {
              msgEl.focus({preventScroll: true});
            } catch (_error) {
              msgEl.focus();
            }

            if (quoteCursorPos !== null) {
              msgEl.setSelectionRange(quoteCursorPos, quoteCursorPos);
            }
          }

          const formTop = commentFormContainer.offset().top;
          const viewportHeight = window.innerHeight;

          const formBottom = formTop + formHeight;
          const currentScrollTop = $(window).scrollTop();
          const currentViewportBottom = currentScrollTop + viewportHeight;

          let needsScroll;

          if (formHeight <= viewportHeight) {
            needsScroll = formTop < currentScrollTop || formBottom + 32 > currentViewportBottom;
          } else {
            const msgTop = $("#msg").offset().top;
            needsScroll = msgTop < currentScrollTop || msgTop > currentViewportBottom;
          }

          if (needsScroll) {
            let targetScrollTop;
            if (formHeight <= viewportHeight) {
              targetScrollTop = formBottom - viewportHeight + 32;
            } else {
              targetScrollTop = formTop - 16;
            }
            targetScrollTop = Math.max(0, targetScrollTop);

            $('html,body').animate({scrollTop: targetScrollTop}, 300);
          }
        } else {
          closeInlineForm();
        }
      };

      const toggleCommentForm = (type, id, authorReadonly, quoteText) => {
        const replyToValue = type === REPLY_TYPE ? id : 0;

        const wouldLoseDraft =
          isInlineFormVisible() &&
          $("input[name='replyto']", commentFormContainer).val() !== String(replyToValue) &&
          $("#msg").val() !== '';

        if (wouldLoseDraft && !window.confirm(SWITCH_REPLY_WARNING)) {
          return;
        }

        updateCsrf();
        updateAuthorReadonlyNote(authorReadonly);

        if (type === REPLY_TYPE) {
          moveAndShowForm(`#comment-${id}`, id, quoteText);
        } else if (type === TOPIC_TYPE) {
          const topicId = $("input[name='topic']", commentFormContainer).val();
          moveAndShowForm(`#topic-${topicId}`, 0, quoteText);
        }
      };

      $('div.reply').each((_i, container) => {
        const topicLink = $('a[href^="comment-message.jsp"]', container);
        bindReplyLink(
          topicLink,
          () => {
            const topicId = $("input[name='topic']", commentFormContainer).val();
            return document.getElementById('topic-' + topicId);
          },
          (quoteText) => {
            toggleCommentForm(TOPIC_TYPE, 0, false, quoteText);
          }
        );

        const lnk = $('a[href^="add_comment.jsp"]', container);
        if (lnk.length > 0) {
          const ids = lnk.attr('href').match(/\d+/g);
          const commentId = ids[1];
          bindReplyLink(lnk, () => document.getElementById('comment-' + commentId), (quoteText) => {
            toggleCommentForm(REPLY_TYPE, commentId, lnk.attr('data-author-readonly') === "true", quoteText);
          });
        }
      });
    } else {
      loadCaptcha();
      updateCsrf();
      if (!commentForm[0].dataset.previewTabsInitialized) {
        initPreviewTabs(commentForm[0]);
        commentForm[0].dataset.previewTabsInitialized = 'true';
      }
    }

    const warnOnUnloadComment = (e) => {
      const hasContent = $("#msg").val() !== '';
      const isVisible = isInline ? commentFormContainer.hasClass(INLINE_FORM_VISIBLE_CLASS) : true;
      if (hasContent && isVisible) {
        e.preventDefault();
        e.returnValue = UNSAVED_WARNING;
        return e.returnValue;
      }
    };

    window.addEventListener('beforeunload', warnOnUnloadComment);

    if (isInline) {
      commentForm.on("reset", (e) => {
        e.preventDefault();
        clearErrors(commentForm);
        commentForm[0]._switchTab?.('editor', {focus: false});
        closeInlineForm();
      });
    }

    let submitInProcess = false;

    commentForm.on("submit", (e) => {
      e.preventDefault();

      if (submitInProcess) {
        return;
      }

      submitInProcess = true;
      window._commentSubmitting = true;
      window.removeEventListener('beforeunload', warnOnUnloadComment);

      const form = commentForm.serialize();

      startSpinner(commentForm);
      clearErrors(commentForm);

      $.ajax({
        method: "POST",
        url: "/add_comment_ajax",
        data: form,
        timeout: 30000
      }).always(() => {
        submitInProcess = false;
        stopSpinner(commentForm);
      }).fail((jqXHR, textStatus, errorThrown) => {
        window._commentSubmitting = false;
        window.addEventListener('beforeunload', warnOnUnloadComment);
        commentForm.prepend(
          $('<div class="error" error>').text('Не удалось выполнить запрос, попробуйте повторить еще раз. ' + errorThrown)
        );
        resetCaptcha();
      }).done((data) => {
        if (data.url) {
          window.location.href = data.url;
        } else {
          window._commentSubmitting = false;
          window.addEventListener('beforeunload', warnOnUnloadComment);
          if (data.errors) {
            const errorDiv = $('<div class="error" error>');
            data.errors.forEach((v) => {
              errorDiv.append($('<span>').text(v));
              errorDiv.append($('<br>'));
            });
            commentForm.prepend(errorDiv);
          }
          resetCaptcha();
        }
      });
    });
  });
});
