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

  const previewButton = formElement.querySelector('button[name=preview]');
  if (previewButton) {
    previewButton.classList.add('preview-button-js-hidden');
  }

  let textareaHeight = 0;

  const switchTab = (tabName, options = {}) => {
    textareaHeight = textarea.offsetHeight;

    nav.querySelectorAll('.markup-tabs__tab').forEach(t => t.classList.remove('active'));
    panelsContainer.querySelectorAll('.markup-tabs__panel').forEach(p => p.classList.remove('active'));

    if (tabName === 'editor') {
      editorTab.classList.add('active');
      editorPanel.classList.add('active');
      previewContent.style.minHeight = '';
      if (options.focus !== false) {
        textarea.focus();
      }
    } else if (tabName === 'preview') {
      previewTab.classList.add('active');
      previewPanel.classList.add('active');
      previewContent.style.minHeight = Math.max(textareaHeight, 50) + 'px';
      loadPreview();
    }
  };

  const loadPreview = () => {
    const text = textarea.value;
    if (!text.trim()) {
      previewContent.textContent = '';
      return;
    }

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
    .then(response => response.json())
    .then(data => {
      previewContent.textContent = '';
      if (data.error) {
        previewContent.appendChild(Object.assign(document.createElement('div'), {className: 'error', textContent: data.error}));
      } else {
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
      previewContent.textContent = '';
      previewContent.appendChild(Object.assign(document.createElement('div'), {className: 'error', textContent: 'Не удалось выполнить запрос, попробуйте повторить еще раз.'}));
    });
  };

  nav.addEventListener('click', (e) => {
    const tab = e.target.closest('.markup-tabs__tab');
    if (!tab || tab.classList.contains('active')) return;
    e.preventDefault();
    switchTab(tab.dataset.tab);
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

    const loadCaptcha = () => {
      const lazyCaptcha = commentFormContainer.find('[data-lazy-captcha]')[0];
      if (!lazyCaptcha || captchaLoaded) {
        return;
      }

      captchaLoaded = true;
      const sitekey = lazyCaptcha.getAttribute('data-sitekey');

      const renderCaptcha = () => {
        if (!captchaRendered) {
          hcaptcha.render(lazyCaptcha, {sitekey: sitekey});
          captchaRendered = true;
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
