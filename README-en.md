# File Indexing Service

A simple service for indexing text files by words.

The project is implemented as a Maven library with a CLI application. It allows you to:
- add directories and files to the index,
- search files by token,
- watch file system changes,
- extend text tokenization via the `Tokenizer` interface.

## Requirements

- Java 21
- Maven 3.x

## Build

```bash
cd "d:/Programming Projects/Java/File Indexing Service"
mvn clean package
```

After building, the jar will be available in `target/`, for example:
- `target/file-indexer-1.0-SNAPSHOT.jar`

## Run CLI

```bash
java -jar target/file-indexer-1.0-SNAPSHOT.jar
```

### CLI Commands

- `add <path>` — add a directory or file to indexing
- `search <token>` — search for files containing a token
- `remove <path>` — remove a path from indexing
- `list` — list indexed files
- `tokens` — show indexed tokens
- `help` — show help
- `exit` / `quit` — exit

## Examples

Add a directory:

```bash
add C:\Users\me\Documents
```

Search for a word:

```bash
search hello
```

## Tests

Run all tests:

```bash
mvn test
```

## Design and API

### Main components

- `Indexer` — indexing API:
  - `addPath(Path path)`
  - `removePath(Path path)`
  - `search(String token)`
  - `stop()`

- `Tokenizer` — text tokenization API:
  - `tokenize(String input)`
  - `indexFile(Path file)` (default method)

- `IndexStore` — thread-safe in-memory index:
  - mapping `token -> set of files`
  - `add`, `remove`, `update`, `removeFile`

- `FileWatcher` — watches the file system:
  - registers directories recursively
  - handles `ENTRY_CREATE`, `ENTRY_MODIFY`, `ENTRY_DELETE`

### Extensibility

To add a custom text parsing strategy, implement `com.shapiro.interfaces.Tokenizer` and pass it to `TextFileIndexer`.

## Limitations

- State is not persisted between runs.
- Indexing support is limited to text files.
