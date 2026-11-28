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

// Прокрутка к inline-форме ответа (add-form.js): расчёт по видимой части окна, а не по innerHeight —
// экранная клавиатура на планшете занимает почти половину окна — и пересчёт, пока она открывается

'use strict';

const {describe, test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const addFormSource = fs.readFileSync(path.join(__dirname, '../../main/webapp/js/add-form.js'), 'utf8');

// Как и в markup-editor-loading.test.js, add-form.js выполняется в функции, которой window, $script и $
// переданы параметрами
const addForm = new Function('window', '$script', '$', addFormSource
    + '\nreturn {visibleViewportRect, inlineFormScrollTarget, inlineFormScrollTop, inlineFormBeforeKeyboardTarget,'
    + ' keyboardShareOf, knownKeyboardShare, rememberKeyboardShare, isForeignScroll, watchViewportResize,'
    + ' watchPageScroll};')(
  {}, {ready() {}}, () => {});
const {
  visibleViewportRect, inlineFormScrollTarget, inlineFormScrollTop, inlineFormBeforeKeyboardTarget,
  keyboardShareOf, knownKeyboardShare, rememberKeyboardShare, isForeignScroll, watchViewportResize,
  watchPageScroll
} = addForm;

// Планшет в горизонтальной ориентации: окно 800px, с экранной клавиатурой видно верхние 450px
const WINDOW_HEIGHT = 800;
const VISIBLE_WITH_KEYBOARD = 450;

// верх и низ видимой части окна в координатах документа
const visibleRect = (top, height) => ({top, bottom: top + height});

// окно и его visualViewport — оба EventTarget, как в браузере
function fakeWindow() {
  const win = new EventTarget();
  win.visualViewport = Object.assign(new EventTarget(), {pageTop: 0, height: WINDOW_HEIGHT});
  return win;
}

const resize = (target) => target.dispatchEvent(new Event('resize'));
const scroll = (target) => target.dispatchEvent(new Event('scroll'));

// форма ответа: над полем вкладки с тулбаром, само поле в 5 строк; под ним подсказки, капча и кнопки
function replyForm(top, height = 400) {
  const msgTop = top + 50;
  return {top, height, msgTop, msgBottom: msgTop + 112};
}

describe('видимая часть окна', () => {
  test('без visualViewport — окно целиком, от прокрутки', () => {
    const win = {pageYOffset: 100, innerHeight: WINDOW_HEIGHT};

    assert.deepEqual(visibleViewportRect(win), visibleRect(100, WINDOW_HEIGHT));
  });

  test('с visualViewport — часть над экранной клавиатурой: innerHeight её не замечает', () => {
    const win = {
      pageYOffset: 400,
      innerHeight: WINDOW_HEIGHT,
      visualViewport: {pageTop: 400, height: VISIBLE_WITH_KEYBOARD}
    };

    assert.deepEqual(visibleViewportRect(win), visibleRect(400, VISIBLE_WITH_KEYBOARD));
  });

  test('со сдвигом видимой части вниз относительно прокрутки (offsetTop)', () => {
    const win = {pageYOffset: 400, innerHeight: WINDOW_HEIGHT, visualViewport: {pageTop: 700, height: 450}};

    assert.deepEqual(visibleViewportRect(win), visibleRect(700, 450));
  });
});

describe('куда прокрутить форму ответа', () => {
  test('форма видна целиком — прокручивать не нужно', () => {
    assert.equal(inlineFormScrollTarget(replyForm(300), visibleRect(100, WINDOW_HEIGHT)), null);
  });

  test('форма помещается, но уходит за низ окна — её низ на 32px выше видимого низа', () => {
    assert.equal(inlineFormScrollTarget(replyForm(600), visibleRect(100, WINDOW_HEIGHT)), 232);
  });

  test('форма видна, но срезан верх — прижимается к видимому низу и видна целиком', () => {
    assert.equal(inlineFormScrollTarget(replyForm(380), visibleRect(400, VISIBLE_WITH_KEYBOARD)), 362);
  });

  test('планшет с клавиатурой: форма помещается над ней — её низ на 32px выше клавиатуры', () => {
    // по окну форма и так видна: её низ на 100px выше низа окна, то есть на 250px ниже верха клавиатуры
    assert.equal(inlineFormScrollTarget(replyForm(500), visibleRect(400, VISIBLE_WITH_KEYBOARD)), 482);
  });

  test('форма выше видимой части — прокручивается назад к видимому низу', () => {
    // после прокрутки видно [82..532] — форма целиком
    assert.equal(inlineFormScrollTarget(replyForm(100), visibleRect(400, VISIBLE_WITH_KEYBOARD)), 82);
  });

  test('форма помещается только без отступа — видна целиком, отступ снизу уступает', () => {
    // форма без капчи у залогиненного: 315px при видимых 330px — отступ в 32px уже не влезает
    const shortForm = replyForm(1000, 315);

    assert.equal(inlineFormScrollTarget(shortForm, visibleRect(400, 330)), 1000);
  });

  test('форма не помещается: верх формы на 16px ниже видимого верха, поле видно целиком', () => {
    assert.equal(inlineFormScrollTarget(replyForm(1000, 500), visibleRect(400, VISIBLE_WITH_KEYBOARD)), 984);
  });

  test('форма не помещается и поле срезалось бы снизу — поле целиком над клавиатурой', () => {
    // телефон в горизонтальной ориентации: видно всего 180px
    assert.equal(inlineFormScrollTarget(replyForm(1100, 500), visibleRect(1000, 180)), 1098);
  });

  test('форма не помещается и поле срезано сверху — поле целиком в видимой части', () => {
    assert.equal(inlineFormScrollTarget(replyForm(900, 500), visibleRect(1000, 180)), 898);
  });

  test('поле само выше видимой части — его верх у видимого верха', () => {
    const tallField = {...replyForm(1100, 500), msgBottom: 1350};

    assert.equal(inlineFormScrollTarget(tallField, visibleRect(1000, 100)), 1150);
  });

  test('форма не помещается, но поле и так видно — прокручивать не нужно', () => {
    assert.equal(inlineFormScrollTarget(replyForm(1000, 500), visibleRect(1000, VISIBLE_WITH_KEYBOARD)), null);
  });
});

describe('от верха видимой части к прокрутке', () => {
  test('видимая часть не сдвинута относительно прокрутки — прокрутка равна её верху', () => {
    assert.equal(inlineFormScrollTop(232, visibleRect(100, WINDOW_HEIGHT), 100), 232);
  });

  test('видимая часть сдвинута вниз (visualViewport.offsetTop) — прокрутка меньше на сдвиг', () => {
    // pageTop = 700 при прокрутке 400: сдвиг 300px. После прокрутки до 600 видимый верх — 900
    assert.equal(inlineFormScrollTop(900, visibleRect(700, 450), 400), 600);
  });

  test('цель выше начала страницы — прокрутка не уходит в минус', () => {
    // сдвиг 300px не даёт поставить видимый верх на 100px: выше начала страницы прокрутки нет
    assert.equal(inlineFormScrollTop(100, visibleRect(700, 450), 400), 0);
  });

  test('вместе с целью прокрутки: и при сдвиге низ формы на 32px выше видимого низа', () => {
    // планшет с клавиатурой: видно [700..1150], форма 1000..1400
    const visible = visibleRect(700, VISIBLE_WITH_KEYBOARD);
    const visibleTop = inlineFormScrollTarget(replyForm(1000), visible);

    assert.equal(visibleTop, 982);
    // после прокрутки видимый верх = 682 + 300, видимый низ = 982 + 450 = низ формы + 32
    assert.equal(inlineFormScrollTop(visibleTop, visible, 400), 682);
  });
});

describe('до открытия клавиатуры', () => {
  // клавиатура займёт половину окна: её верх — в 400px от видимого верха
  const half = 0.5;

  test('полю достаётся места над клавиатурой — форму не трогаем', () => {
    // поле 250..362, до верха клавиатуры остаётся ещё 38px
    assert.equal(inlineFormBeforeKeyboardTarget(replyForm(200), visibleRect(0, WINDOW_HEIGHT), half), null);
  });

  test('полю не хватает места — верх формы к видимому верху', () => {
    // поле 350..462: низ поля на 62px ниже верха клавиатуры
    assert.equal(inlineFormBeforeKeyboardTarget(replyForm(300), visibleRect(0, WINDOW_HEIGHT), half), 300);
  });

  test('та же форма там, где клавиатура занимает треть окна, — места хватает', () => {
    assert.equal(inlineFormBeforeKeyboardTarget(replyForm(300), visibleRect(0, WINDOW_HEIGHT), 1 / 3), null);
  });

  test('форма уже верхом у видимого верха — прокручивать не нужно', () => {
    // окно в 200px: верх клавиатуры на 500px, поле 450..562 — под ним
    assert.equal(inlineFormBeforeKeyboardTarget(replyForm(400, 500), visibleRect(400, 200), half), null);
  });
});

describe('доля окна под клавиатурой', () => {
  test('измеряется по тому, насколько сжалась видимая часть после открытия формы', () => {
    assert.equal(keyboardShareOf(WINDOW_HEIGHT, VISIBLE_WITH_KEYBOARD), 0.4375);
  });

  test('окно не сжималось или выросло — измерять нечего', () => {
    assert.equal(keyboardShareOf(WINDOW_HEIGHT, WINDOW_HEIGHT), null);
    assert.equal(keyboardShareOf(WINDOW_HEIGHT, WINDOW_HEIGHT + 50), null);
  });

  test('клавиатуру ещё не видели — половина окна', () => {
    assert.equal(knownKeyboardShare(null), 0.5);
    assert.equal(knownKeyboardShare({getItem: () => null}), 0.5);
    assert.equal(knownKeyboardShare({getItem: () => 'нет'}), 0.5);
    assert.equal(knownKeyboardShare({getItem: () => '1.5'}), 0.5);
  });

  test('измеренная при прошлом открытии — берётся из хранилища', () => {
    assert.equal(knownKeyboardShare({getItem: () => '0.2987'}), 0.2987);
  });

  test('запоминается', () => {
    const values = {};
    const storage = {setItem: (key, value) => { values[key] = value; }};
    rememberKeyboardShare(storage, 1 - 826 / 1180);

    assert.deepEqual(values, {'lor.keyboardShare': '0.3'}); // без 0.30000000000000004
  });

  test('хранилище недоступно — не ошибка', () => {
    assert.doesNotThrow(() => rememberKeyboardShare(null, 0.3));
    assert.doesNotThrow(() => rememberKeyboardShare({setItem() { throw new Error('QuotaExceededError'); }}, 0.3));
  });
});

describe('чужая прокрутка', () => {
  test('в окне то, что только что записала наша анимация, — прокрутка наша', () => {
    assert.equal(isForeignScroll(true, 318.4, 318.4), false);
    assert.equal(isForeignScroll(true, 318.4, 319), false); // браузер округлил
  });

  test('в окне другое — страницу прокрутил кто-то ещё', () => {
    assert.equal(isForeignScroll(true, 318.4, 290), true);
  });

  test('анимации нет — различать нечего', () => {
    assert.equal(isForeignScroll(false, 0, 5000), false);
  });
});

describe('наблюдение за окном, пока открывается клавиатура', () => {
  test('изменения видимой части пересчитывают прокрутку, пока не стихнут', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let changes = 0;
    watchViewportResize(win, () => changes++);

    resize(win.visualViewport);
    assert.equal(changes, 1);

    t.mock.timers.tick(100);
    resize(win.visualViewport);
    assert.equal(changes, 2, 'клавиатура ещё открывается — пересчёт продолжается');

    t.mock.timers.tick(250);
    resize(win.visualViewport);
    assert.equal(changes, 2, 'изменения стихли — наблюдение прекращено');
  });

  test('клавиатуры нет: через три секунды окно больше не трогают', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let changes = 0;
    watchViewportResize(win, () => changes++);

    t.mock.timers.tick(2999);
    resize(win.visualViewport);
    assert.equal(changes, 1);

    t.mock.timers.tick(1);
    resize(win.visualViewport);
    assert.equal(changes, 1, 'окно изменилось слишком поздно — это уже не клавиатура');
  });

  test('наблюдение можно прекратить раньше — при закрытии формы', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let changes = 0;
    const stop = watchViewportResize(win, () => changes++);

    stop();
    stop(); // повторное прекращение ничего не ломает
    resize(win.visualViewport);
    resize(win);

    assert.equal(changes, 0);
  });

  test('клавиатура сжимает само окно: window.resize тоже замечают', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let changes = 0;
    watchViewportResize(win, () => changes++);

    resize(win);
    assert.equal(changes, 1);
  });

  test('без visualViewport наблюдение работает и завершается без ошибок', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = new EventTarget();
    let changes = 0;
    watchViewportResize(win, () => changes++);

    resize(win);
    t.mock.timers.tick(3000);

    assert.equal(changes, 1);
  });
});

describe('сопровождение прокрутки, пока открывается клавиатура', () => {
  test('пересчёт приходит, когда прокрутка закончилась — один раз на всю прокрутку', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let recalculations = 0;
    watchPageScroll(win, () => recalculations++);

    scroll(win);
    t.mock.timers.tick(50);
    scroll(win.visualViewport); // сдвиг видимой части — тоже прокрутка
    t.mock.timers.tick(99);
    assert.equal(recalculations, 0, 'прокрутка ещё идёт');

    t.mock.timers.tick(1);
    assert.equal(recalculations, 1);
  });

  test('о каждом событии прокрутки сообщается сразу, пересчёт — после паузы', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let scrollEvents = 0;
    let recalculations = 0;
    watchPageScroll(win, () => recalculations++, () => scrollEvents++);

    scroll(win);
    scroll(win.visualViewport);
    assert.equal(scrollEvents, 2);
    assert.equal(recalculations, 0);

    t.mock.timers.tick(100);
    assert.equal(recalculations, 1);
  });

  test('прокрутка пальцем прекращает сопровождение — её нельзя перебивать', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let recalculations = 0;
    watchPageScroll(win, () => recalculations++);

    win.dispatchEvent(new Event('touchmove'));
    scroll(win);
    t.mock.timers.tick(1000);

    assert.equal(recalculations, 0);
  });

  test('прокрутка колесом — тоже', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let recalculations = 0;
    watchPageScroll(win, () => recalculations++);

    win.dispatchEvent(new Event('wheel'));
    scroll(win);
    t.mock.timers.tick(1000);

    assert.equal(recalculations, 0);
  });

  test('через три секунды сопровождение прекращается само', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let recalculations = 0;
    watchPageScroll(win, () => recalculations++);

    t.mock.timers.tick(3000);
    scroll(win);
    t.mock.timers.tick(1000);

    assert.equal(recalculations, 0);
  });

  test('наблюдение можно прекратить раньше — при закрытии формы', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = fakeWindow();
    let recalculations = 0;
    const stop = watchPageScroll(win, () => recalculations++);

    stop();
    stop(); // повторное прекращение ничего не ломает
    scroll(win);
    t.mock.timers.tick(1000);

    assert.equal(recalculations, 0);
  });

  test('без visualViewport сопровождение работает', (t) => {
    t.mock.timers.enable({apis: ['setTimeout']});
    const win = new EventTarget();
    let recalculations = 0;
    watchPageScroll(win, () => recalculations++);

    scroll(win);
    t.mock.timers.tick(100);

    assert.equal(recalculations, 1);
  });
});
