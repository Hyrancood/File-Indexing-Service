package com.shapiro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import com.shapiro.interfaces.Indexer;

import com.shapiro.core.IndexStore;
import com.shapiro.implementations.TextFileIndexer;
import com.shapiro.implementations.SimpleTokenizer;

public class TextFileIndexerCLI {
    private static IndexStore indexStore = new IndexStore();
    private static Indexer indexer;

    public static void main(String[] args) {
        try {
            indexer = new TextFileIndexer(indexStore, new SimpleTokenizer(false), 4);
        } catch (IOException e) {
            System.err.println("An error occurred while initializing the indexer: " + e.getMessage());
            e.printStackTrace();
        }
        cli(args);
    }

    public static void cli(String[] args) {
        System.out.println("File Indexing Service CLI");
        java.io.Console console = System.console();
        if (console == null) {
            System.out.println("Консоль недоступна. Запустите программу в терминале.");
            System.out.println("Console is not available. Please run the program in a terminal.");
            return;
        }
 
        printHelp();

        boolean running = true;
        while (running) {
            String line = console.readLine("cmd> ");
            if (line == null || line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String arg = parts.length > 1 ? parts[1].trim() : "";
            
            running = cmd(cmd, arg);          
        }

        indexer.stop();
        System.out.println("Завершено.");
    }

    private static boolean cmd(String cmd, String arg) {
        switch (cmd) {
            case "add":
                if (arg.isEmpty()) {
                    System.out.println("Usage: add <directory-or-file-path>");
                } else {
                    Path path = Paths.get(arg);
                    if (!Files.exists(path)) {
                        System.out.println("Указанный путь не существует: " + arg);
                    } else {
                        indexer.addPath(path);
                        System.out.println("Добавлен путь для индексации: " + path);
                    }
                }
                break;
            case "search":
                if (arg.isEmpty()) {
                    System.out.println("Usage: search <token>");
                } else {
                    Set<Path> results = indexer.search(arg);
                    if (results.isEmpty()) {
                        System.out.println("Токен не найден: " + arg);
                    } else {
                        System.out.println("Файлы, содержащие '" + arg + "':");
                        results.forEach(p -> System.out.println("  " + p.toAbsolutePath()));
                    }
                }
                break;
            case "remove":
                if (arg.isEmpty()) {
                    System.out.println("Usage: remove <directory-or-file-path>");
                } else {
                    Path path = Paths.get(arg);
                    if (!Files.exists(path)) {
                        System.out.println("Указанный путь не существует: " + arg);
                    } else {
                        indexer.removePath(path);
                        System.out.println("Удален путь из индексации: " + path);
                    }
                }
                break;
            case "list":
                System.out.println("Индексированные файлы:");
                indexStore.getAllIndexedFiles().forEach(p -> System.out.println("  " + p.toAbsolutePath()));
                break;
            case "tokens":
                System.out.println("Индексированные токены:");
                indexStore.getAllTokens().forEach(t -> System.out.println("  " + t));
                break;
            case "help":
                printHelp();
                break;
            case "exit":
            case "quit":
                return false;
            default:
                System.out.println("Неизвестная команда: " + cmd + ". Введите help для списка команд.");
        }
        return true;
    }

    private static void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  add <path>     - рекурсивно индексирует каталог или файл");
        System.out.println("  remove <path>  - удаляет путь из индексации");
        System.out.println("  search <token> - ищет файлы по ключевому слову");
        System.out.println("  list           - показывает все индексированные файлы");
        System.out.println("  tokens         - показывает все индексированные токены");
        System.out.println("  help           - показывает это сообщение");
        System.out.println("  exit|quit      - выход");
    }
}