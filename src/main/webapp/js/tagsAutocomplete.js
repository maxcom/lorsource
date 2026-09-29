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

(function() {
    var idCounter = 0;

    function ensureId(el) {
        if (!el.id) {
            el.id = "tagsAC_" + (++idCounter);
        }
        return el.id;
    }

    function fetchTags(query) {
        return fetch("/tags/?term=" + encodeURIComponent(query))
            .then(function(r) { return r.json(); })
            .catch(function() { return []; });
    }

    function initMulti(input) {
        new window.autoComplete({
            selector: "#" + ensureId(input),
            threshold: 2,
            data: {
                src: fetchTags,
                cache: false
            },
            query: function(query) {
                var parts = query.split(/,\s*/);
                return parts[parts.length - 1].trim();
            },
            resultsList: {
                class: "autoComplete_list",
                maxResults: 10,
                tabSelect: true
            },
            resultItem: {
                class: "autoComplete_result",
                highlight: true
            },
            events: {
                input: {
                    selection: function(event) {
                        var val = event.detail.selection.value;
                        var parts = input.value.split(/,\s*/);
                        parts.pop();
                        parts.push(val);
                        input.value = parts.join(", ") + ", ";
                    }
                }
            }
        });
    }

    function initSingle(input) {
        new window.autoComplete({
            selector: "#" + ensureId(input),
            threshold: 2,
            data: {
                src: fetchTags,
                cache: false
            },
            resultsList: {
                class: "autoComplete_list",
                maxResults: 10,
                tabSelect: true
            },
            resultItem: {
                class: "autoComplete_result",
                highlight: true
            },
            events: {
                input: {
                    selection: function(event) {
                        input.value = event.detail.selection.value;
                    }
                }
            }
        });
    }

    $(function() {
        document.querySelectorAll("[data-tags-autocomplete]").forEach(initMulti);
        document.querySelectorAll("[data-tags-autocomplete-single]").forEach(initSingle);
    });
})();
