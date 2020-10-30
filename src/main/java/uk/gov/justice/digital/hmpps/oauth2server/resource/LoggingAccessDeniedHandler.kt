package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class LoggingAccessDeniedHandler implements AccessDeniedHandler {

    private static Logger log = LoggerFactory.getLogger(LoggingAccessDeniedHandler.class);

    @Override
    public void handle(final HttpServletRequest request,
                       final HttpServletResponse response,
                       final AccessDeniedException ex) throws IOException {

        final var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            log.info(auth.getName()
                    + " was trying to access protected resource: "
                    + request.getRequestURI());
        }

        response.sendRedirect(request.getContextPath() + "/access-denied");

    }
}
