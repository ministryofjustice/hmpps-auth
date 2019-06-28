package uk.gov.justice.digital.hmpps.oauth2server.utils;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MdcUtility {

    static final String USER_ID_HEADER = "userId";
    static final String REQUEST_DURATION = "duration";
    static final String RESPONSE_STATUS = "status";
    static final String SKIP_LOGGING = "skipLogging";

    static boolean isLoggingAllowed() {
        return !"true".equals(MDC.get(SKIP_LOGGING));
    }
}
