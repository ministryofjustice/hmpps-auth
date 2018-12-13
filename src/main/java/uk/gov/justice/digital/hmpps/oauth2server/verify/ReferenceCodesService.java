package uk.gov.justice.digital.hmpps.oauth2server.verify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
public class ReferenceCodesService {

    private static final String EMAIL_DOMAINS_SQL =
            "select description FROM reference_codes r " +
                    "WHERE r.domain = 'EMAIL_DOMAIN' AND active_flag  ='Y' AND expired_date IS NULL";
    private final JdbcTemplate jdbcTemplate;

    public ReferenceCodesService(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> getValidEmailDomains() {
        return jdbcTemplate.queryForList(EMAIL_DOMAINS_SQL, String.class);
    }
}
