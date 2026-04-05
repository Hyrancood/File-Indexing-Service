# File Indexing Service

Простой сервис индексирования текстовых файлов по словам.

Проект реализован как Maven-библиотека с CLI-программой. Он позволяет:
- добавлять каталоги и файлы в индекс,
- искать файлы по слову,
- отслеживать изменения на диске,
- расширять механизм разбиения текста через интерфейс `Tokenizer`.

## Требования

- Java 21
- Maven 3.x

## Сборка

```bash
cd "d:/Programming Projects/Java/File Indexing Service"
mvn clean package
```

После сборки jar будет доступен в `target/`, например:
- `target/file-indexer-1.0-SNAPSHOT.jar`

## Запуск CLI

```bash
java -jar target/file-indexer-1.0-SNAPSHOT.jar
```

### Команды CLI

- `add <path>` — добавить каталог или файл в индекс
- `search <token>` — найти файлы, содержащие токен
- `remove <path>` — удалить путь из индексации
- `list` — показать индексированные файлы
- `tokens` — показать список токенов
- `help` — показать справку
- `exit` / `quit` — выход

## Примеры

Добавить каталог:

```bash
add C:\Users\me\Documents
```

Искать слово:

```bash
search hello
```

## Тесты

Запустить все тесты:

```bash
mvn test
```

## Дизайн и API

### Основные компоненты

- `Indexer` — API для индексации:
  - `addPath(Path path)`
  - `removePath(Path path)`
  - `search(String token)`
  - `stop()`

- `Tokenizer` — API для разбиения текста:
  - `tokenize(String input)`
  - `indexFile(Path file)` (default метод)

- `IndexStore` — потокобезопасный in-memory индекс:
  - связь `token -> набор файлов`
  - `add`, `remove`, `update`, `removeFile`

- `FileWatcher` — следит за файловой системой:
  - регистрирует директории рекурсивно
  - обрабатывает `ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE`

### Расширяемость

Для добавления своего способа разбора текста, необходимо реализовать `com.shapiro.interfaces.Tokenizer` и передать в `TextFileIndexer`.

## Ограничения

- Состояние между запусками не сохраняется.
- Поддержка индексации ограничена текстовыми файлами.

