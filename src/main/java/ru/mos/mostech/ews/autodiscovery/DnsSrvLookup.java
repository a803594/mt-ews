package ru.mos.mostech.ews.autodiscovery;

import lombok.extern.slf4j.Slf4j;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class DnsSrvLookup {

    public static void main(String[] args) {
        String dnsQuery = "mos.ru";
        lookupSrvRecords(dnsQuery);
    }

    public static List<LookupResult> lookupSrvRecords(String query) {
        List<LookupResult> results = new ArrayList<>();
        try {
            String fullQuery = "_autodiscover._tcp." + query;
            log.info("Отправка запроса к DNS для {}", fullQuery);
            Lookup lookup = new Lookup(fullQuery, Type.SRV);
            Record[] records = lookup.run();
            // Проверяем на наличие ответа
            if (records == null || records.length == 0) {
                log.warn("Нет ответов на запрос: {}", fullQuery);
            } else {
                int i = 1;
                for (Record r : records) {
                    if (r instanceof SRVRecord srv) {
                        log.info("Запись {}, Service: {}", i, srv.getTarget());
                        log.info("Запись {}, Port: {}", i, srv.getPort());
                        log.info("Запись {}, Priority: {}", i, srv.getPriority());
                        log.info("Запись {}, Weight: {}", i, srv.getWeight());
                        results.add(new LookupResult(removeLastDot(srv.getTarget().toString()), srv.getPort(), srv.getPriority()));
                    }
                    i++;
                }
            }
        } catch (TextParseException e) {
            log.error("Ошибка разбора запроса:", e);
        } catch (Exception e) {
            log.error("Ошибка ввода/вывода:", e);
        }
        return results.stream().sorted(Comparator.comparing(LookupResult::priority)).toList();
    }

    private static String removeLastDot(String str) {
        if (str.endsWith(".")) {
            return str.substring(0, str.lastIndexOf('.'));
        }
        return str;
    }

    public record LookupResult(String host, int port, int priority) {
    }
}