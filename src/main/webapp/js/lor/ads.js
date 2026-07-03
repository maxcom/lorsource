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

function init_interpage_adv(ads) {
  $(function () {
    const img = $('<img>');
    const anchor = $('<a>');
    const ad = ads[Math.floor(Math.random() * ads.length)];

    if (ad.type === 'img') {
      anchor.attr('href', ad.href);
      anchor.attr('target', '_blank');

      img.attr('src', ad.src);
      if ('width' in ad) {
        img.attr('width', ad.width);
      } else {
        img.attr('width', 728);
      }

      if ('height' in ad) {
        img.attr('height', ad.height);
      } else {
        img.attr('height', 90);
      }

      anchor.append(img);
      $('#interpage').append(anchor);
    }

    if (ad.type === 'rimg') {
      anchor.attr('href', ad.href);
      anchor.attr('target', '_blank');

      const interpage = $('#interpage');

      if (interpage.width() > 1024) {
        img.attr('width', 1000);
        img.attr('height', 120);
        img.attr('src', ad.img1000);
      } else if (interpage.width() > 750) {
        img.attr('width', 728);
        img.attr('height', 90);
        img.attr('src', ad.img728);
        img.attr('style', "margin-top: 15px");
      } else {
        img.attr('width', 320);
        img.attr('height', 100);
        img.attr('style', "margin-top: 5px");
        img.attr('src', ad.img320);
      }

      anchor.append(img);
      interpage.append(anchor);
    }
  });
}