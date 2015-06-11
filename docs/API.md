# API
Документация по работе с API linux.org.ru

## Список методов

URL для запросов к API: ```https://linux.org.ru/api/метод?параметры```
Обязательные параметры выделены жирным шрифтом, значения по умолчанию - курсивом. В скобках указан требуемый тип аргумента.

*TODO: Добавить список возвращаемых значений.*

**Авторизация**

**Пользователи**

Метод | Описание | Параметры | Авторизация
------|----------|-----------|------------
user | получить информацию о пользователе | **user** (String) | Для получения email, score, ignoredTags, ignoredUsers

**Темы**

Метод | Описание | Параметры | Авторизация
------|----------|-----------|------------
topics | Получить список тем | **section** (String), group, **fromDate** (String), **toDate** (String), limit (Integer), offset (Integer, *0*), tag (String), notalks (Boolean, *false*), tech (Boolean, *false*), commitMode (String, *ALL*), author (String) | Нет
{section}/{group}/{id}/topic | получить свойства темы | Нет | Нет
tracker | получить последние темы | offset (Integer), filter (String, *main*) | Нет

**Сообщения**

Метод | Описание | Параметры | Авторизация
------|----------|-----------|------------
{section}/{group}/{id}/comments | получить комментарии темы | Нет | Нет

**Уведомления**

Метод | Описание | Параметры | Авторизация
------|----------|-----------|-----------------------
notifications-count (plaintext), yandex-tableau (JSON) | количество непрочитанных уведомлений | Нет | Да
notifications-reset | сбросить счетчик уведомлений | Нет | Да
notifications-list | получить список уведомлений | offset (Integer), filter (String) | Да
