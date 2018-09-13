package uk.gov.justice.digital.hmpps.oauth2server.utils;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MdcUtility {

    static final String USER_ID = "userId";
    static final String REQUEST_ID = "requestId";
    static final String REQUEST_DURATION = "duration";
    static final String RESPONSE_STATUS = "status";
    static final String SKIP_LOGGING = "skipLogging";

    String generateUUID() {
        return UUID.randomUUID().toString();
    }

    static boolean isLoggingAllowed() {
        return !"true".equals(MDC.get(SKIP_LOGGING));
    }

}
