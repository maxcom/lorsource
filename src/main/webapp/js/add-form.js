
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


/*
 * Тулбар кнопок разметки. Создаётся только скриптом и вставляется сразу после вкладок:
 * без JavaScript кнопок нет в DOM.
 *
 * MarkupEditor лежит в markup-editor.js и к этому моменту загружен: эту функцию вызывает только
 * initPreviewTabs, который ждёт его загрузки. Если тот всё же не загрузился (сетевая ошибка),
 * тулбара не будет, а вкладки предпросмотра останутся рабочими.
 */
function initMarkupToolbar(nav, textarea, formatMode) {
  if (typeof MarkupEditor === 'undefined') return null;

  const dialect = MarkupEditor.dialect(formatMode);
  if (!dialect) return null;

  const editor = MarkupEditor.createEditor(textarea, dialect);

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

/*
 * Вкладки предпросмотра и тулбар разметки.
 *
 * Тулбару нужен MarkupEditor из markup-editor.js, а страница грузит оба файла параллельно, и каким
 * из них выполниться раньше, не определено. Дождаться второго файла через колбэк DOM-ready нельзя:
 * тот срабатывает через пару таймеров после add-form.js (а если страница ещё разбирается — в момент
 * готовности страницы) и не ждёт остальных скриптов. Поэтому страницы регистрируют оба файла в $script
 * под именем 'markup-editor' (оно отмечается готовым, когда загрузились оба), и инициализация ждёт
 * этой готовности. Вызывающий код после вызова сразу помечает форму инициализированной: повторный
 * запуск до выполнения отложенной инициализации не нужен.
 */
function initPreviewTabs(formElement) {
  $script.ready('markup-editor', function () {
    setupPreviewTabs(formElement);
  });
}

function setupPreviewTabs(formElement) {
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
