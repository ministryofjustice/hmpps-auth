package uk.gov.justice.digital.hmpps.oauth2server.verify;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.model.ReferenceDomain;
import uk.gov.justice.digital.hmpps.oauth2server.nomis.repository.ReferenceCodeRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ReferenceCodesService {
    private final LoadingCache<ReferenceDomain, List<String>> referenceCodesCache;

    public ReferenceCodesService(final ReferenceCodeRepository referenceCodeRepository) {
        referenceCodesCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build(CacheLoader.from(k -> {
            final var emailDomains = referenceCodeRepository.findByDomainCodeIdentifierDomainAndActiveIsTrueAndExpiredDateIsNull(k);
            return emailDomains
                    .stream()
                    .map(r -> r.getDescription().replaceAll("%", ".*").toLowerCase())
                    .collect(Collectors.toList());
        }));
    }

    public boolean isValidEmailDomain(final String domain) {
        if (StringUtils.isBlank(domain)) {
            return false;
        }
        final var domainLower = domain.toLowerCase();
        try {
            return referenceCodesCache.get(ReferenceDomain.EMAIL_DOMAIN).stream().anyMatch(domainLower::matches);
        } catch (final ExecutionException e) {
            // nothing we can do here, so throw toys out of pram
            log.error("Caught exception retrieving reference codes", e);
            throw new RuntimeException(e);
        }
    }
}
