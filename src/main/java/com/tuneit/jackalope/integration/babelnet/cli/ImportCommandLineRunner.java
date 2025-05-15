package com.tuneit.jackalope.integration.babelnet.cli;

import com.tuneit.jackalope.integration.babelnet.integration.SemanticIntegrationService;
import com.tuneit.jackalope.integration.babelnet.service.BabelNetApiService;
import com.tuneit.jackalope.ltf.semnetmanager.entry.SenseEntry;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.jlt.util.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import org.springframework.context.ApplicationContext;


@Component
public class ImportCommandLineRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ImportCommandLineRunner.class);

    @Autowired
    private BabelNetApiService babelNetApiService;

    @Autowired
    private SemanticIntegrationService semanticIntegrationService;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Запуск модуля импорта BabelNet. Введите команду или 'exit'/'shutdown' для выхода.");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("shutdown")) {
                logger.info("Завершение работы модуля импорта.");
                shutdown();
                break;
            }
            try {
                processCommand(line.trim());
            } catch (Exception e) {
                logger.error("Ошибка при выполнении команды: {}", e.getMessage(), e);
            }
        }
    }

    private void shutdown() {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) applicationContext).close();
            logger.info("Приложение успешно завершено.");
        } else {
            logger.warn("Невозможно завершить приложение: неподдерживаемый тип контекста.");
        }
    }

    private void processCommand(String line) throws Exception {
        logger.debug("Обработка команды: {}", line);
        String[] tokens = line.split("\\s+");
        if (tokens.length == 0) {
            logger.warn("Пустая команда.");
            return;
        }
        String cmd = tokens[0].toLowerCase();
        switch (cmd) {
            case "help":
                printHelp();
                break;
            case "getsynset":
                if (tokens.length < 2) {
                    logger.warn("Использование: getSynset id=<bn:...>");
                    return;
                }
                handleGetSynset(tokens[1]);
                break;
            case "getedges":
                if (tokens.length < 2) {
                    logger.warn("Использование: getEdges id=<bn:...>");
                    return;
                }
                handleGetEdges(tokens[1]);
                break;
            case "getsynsets":
                if (tokens.length < 3) {
                    logger.warn("Использование: getSynsets lemma=<value> searchLang=<lang>");
                    return;
                }
                handleGetSynsets(tokens[1], tokens[2]);
                break;
            case "importedges":
                if (tokens.length < 2) {
                    logger.warn("Использование: importEdges bn=<bn:...>");
                    return;
                }
                semanticIntegrationService.handleImportEdges(tokens[1]);
                break;
            case "shutdown":
                shutdown();
                break;
            default:
                logger.warn("Неизвестная команда: {}", cmd);
                printHelp();
        }
    }

    private void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("  help                                     - вывести этот список команд");
        System.out.println("  getSynset lemma=<value> searchLang=<lang>  - получить синсет по лемме и языку");
        System.out.println("  getEdges lemma=<value> searchLang=<lang>   - получить рёбра для синсета по лемме");
        System.out.println("  getSynsets lemma=<value> searchLang=<lang> - получить синсеты по лемме и языку");
        System.out.println("  importEdges lemma=<value> searchLang=<lang> - импортировать рёбра для синсета (значение – лемма)");
        System.out.println("  shutdown                                 - завершить работу");
    }

    private void handleGetSynset(String param) throws Exception {
        String lemma = parseParamValue(param, "lemma");
        if (lemma == null) {
            logger.warn("Укажите параметр lemma=<value>");
            return;
        }
        logger.info("Запрос синсета для леммы: {}", lemma);
        List<BabelSynset> synsets = babelNetApiService.fetchSynsets(lemma, Language.EN);
        if (synsets != null && !synsets.isEmpty()) {
            BabelSynset synset = synsets.get(0);
            String mainSenseStr = babelNetApiService.getMainSenseOrFallback(synset, Language.EN);
            String label = semanticIntegrationService.getDisplayLabel(new SenseEntry());
            logger.info("Найден синсет: MainSense: {}, Synset Label: {}", mainSenseStr, label);
        } else {
            logger.info("Синсеты не найдены для леммы {}", lemma);
        }
    }

    private void handleGetEdges(String param) throws Exception {
        String lemma = parseParamValue(param, "lemma");
        if (lemma == null) {
            logger.warn("Укажите параметр lemma=<value>");
            return;
        }
        Language lang = Language.EN;
        logger.info("Запрос рёбер для синсета по лемме: {}", lemma);
        List<BabelSynset> synsets = babelNetApiService.fetchSynsets(lemma, lang);
        if (synsets == null || synsets.isEmpty()) {
            logger.info("Синсеты не найдены для леммы: {}", lemma);
            return;
        }
        BabelSynset synset = synsets.get(0);
        logger.info("Количество рёбер: {}", synset.getEdges().size());
        synset.getEdges().forEach(e -> logger.info("Edge: {} -> {}", e.getPointer(), e.getBabelSynsetIDTarget()));
    }

    private void handleGetSynsets(String lemmaParam, String langParam) throws Exception {
        String lemma = parseParamValue(lemmaParam, "lemma");
        String searchLang = parseParamValue(langParam, "searchLang");

        if (lemma == null || searchLang == null) {
            logger.warn("Использование: getSynsets lemma=... searchLang=...");
            return;
        }

        logger.info("Запрос синсетов для lemma={}, lang={}", lemma, searchLang);
        Language langEnum = Language.valueOf(searchLang.toUpperCase());
        List<BabelSynset> list = babelNetApiService.fetchSynsets(lemma, langEnum);
        logger.info("Найдено синсетов: {}", list.size());

        for (BabelSynset s : list) {
            String mainSenseStr = babelNetApiService.getMainSenseOrFallback(s, langEnum);
            logger.info("SynsetID={}, MainSense={}", s.getId(), mainSenseStr);
        }
    }

    private String parseParamValue(String param, String key) {
        int eqIndex = param.indexOf('=');
        if (eqIndex < 0) return null;
        String k = param.substring(0, eqIndex).trim();
        String v = param.substring(eqIndex + 1).trim();
        return k.equalsIgnoreCase(key) ? v : null;
    }
}