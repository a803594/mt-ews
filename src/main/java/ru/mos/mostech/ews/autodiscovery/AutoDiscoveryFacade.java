package ru.mos.mostech.ews.autodiscovery;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import ru.mos.mostech.ews.autodiscovery.HttpAutoDiscoverClient.AutoDiscoveryRequestParams;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class AutoDiscoveryFacade {

    public static void main(String[] args) {
        Optional<ResolveEwsResults> resolveEws = resolveEws(ResolveEwsParams.builder()
                .email("***REMOVED***")
                .password("***REMOVED***")
                .build());

        log.info("resolveEws: {}", resolveEws);
    }

    public static Optional<ResolveEwsResults> resolveEws(ResolveEwsParams params) {
        Objects.requireNonNull(params.getEmail(), "email is required");
        int index = params.getEmail().indexOf("@");
        if (index == -1) {
            throw new IllegalArgumentException("email doesn't contain @");
        }
        String domain = params.getEmail().substring(index + 1);
        List<DnsSrvLookup.LookupResult> lookupResults = DnsSrvLookup.lookupSrvRecords(domain);
        if (lookupResults.isEmpty()) {
            return Optional.empty();
        }
        DnsSrvLookup.LookupResult result = lookupResults.iterator().next();

        List<AutoDiscoveryRequestParams> requests = List.of(
                getParams(params, result, true, true, false),
                getParams(params, result, true, false, false),
                getParams(params, result, true, false, true),

                getParams(params, result, false, true, false),
                getParams(params, result, false, false, false),
                getParams(params, result, false, false, true)
        );

        return requests.stream()
                .map(req -> {
                    String url = HttpAutoDiscoverClient.sendRequest(req);
                    if (url == null || url.isEmpty()) {
                        return null;
                    }
                    return ResolveEwsResults.builder()
                            .user(req.getUser())
                            .email(req.getEmail())
                            .domain(req.getEmail().substring(req.getEmail().indexOf("@") + 1))
                            .url(url)
                            .build();
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static AutoDiscoveryRequestParams getParams(
            ResolveEwsParams params,
            DnsSrvLookup.LookupResult result,
            boolean isSecure,
            boolean forceUseEmail,
            boolean forceHQ) {
        return AutoDiscoveryRequestParams.builder()
                .host(result.host())
                .port(result.port())
                .isSecure(isSecure)
                .email(params.getEmail())
                .user(getUser(params.getEmail(), forceUseEmail, forceHQ))
                .password(params.getPassword())
                .build();
    }

    private static String getUser(String email, boolean forceUseEmail, boolean forceHQ) {
        int index = email.indexOf("@");
        String user = email.substring(0, index);
        String domain = email.substring(index + 1);
        if (domain.endsWith("mos.ru") || forceHQ) {
            return "HQ\\" + user;
        }
        if (forceUseEmail) {
            return email;
        }
        return user;
    }

    @Data
    @Builder
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public static class ResolveEwsParams {
        String email;
        String password;
    }

    @Data
    @Builder
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public static class ResolveEwsResults {
        String email;
        String domain;
        String user;
        String url;
        long time = System.currentTimeMillis();
    }

}
