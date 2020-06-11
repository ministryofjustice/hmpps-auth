package uk.gov.justice.digital.hmpps.oauth2server.resource;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthorityPropertyEditor;
import uk.gov.justice.digital.hmpps.oauth2server.config.SplitCollectionEditor;
import uk.gov.justice.digital.hmpps.oauth2server.security.UserPersonDetails;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@Controller
@RequestMapping("ui/clients")
public class ClientsController {

    private final JdbcClientDetailsService clientsDetailsService;
    private final TelemetryClient telemetryClient;

    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        binder.registerCustomEditor(Collection.class, new SplitCollectionEditor(Set.class, ","));
        binder.registerCustomEditor(GrantedAuthority.class, new AuthorityPropertyEditor());
    }

    @GetMapping(value = "/form")
    @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
    public String showEditForm(@RequestParam(value = "client", required = false) final String clientId, final Model model) {

        final ClientDetails clientDetails;
        if (clientId != null) {
            clientDetails = clientsDetailsService.loadClientByClientId(clientId);
        } else {
            clientDetails = new BaseClientDetails();
        }

        model.addAttribute("clientDetails", clientDetails);
        return "ui/form";
    }


    @PostMapping(value = "/edit")
    @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
    public String editClient(
            final Authentication authentication,
            @ModelAttribute final AuthClientDetails clientDetails,
            @RequestParam(value = "newClient", required = false) final String newClient) {

        final var userDetails = (UserPersonDetails) authentication.getPrincipal();
        final var telemetryMap = Map.of("username", userDetails.getUsername(), "clientId", clientDetails.getClientId());

        if (newClient == null) {
            clientsDetailsService.updateClientDetails(clientDetails);
            telemetryClient.trackEvent("AuthClientDetailsUpdate", telemetryMap, null);
        } else {
            clientsDetailsService.addClientDetails(clientDetails);
            telemetryClient.trackEvent("AuthClientDetailsAdd", telemetryMap, null);
        }

        if (!clientDetails.getClientSecret().isEmpty()) {
            clientsDetailsService.updateClientSecret(clientDetails.getClientId(), clientDetails.getClientSecret());
            telemetryClient.trackEvent("AuthClientSecretUpdated", telemetryMap, null);
        }
        return "redirect:/ui";
    }

    @GetMapping(value = "/{clientId}/delete")
    @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
    public String deleteClient(final Authentication authentication, @PathVariable final String clientId) {
        final var userDetails = (UserPersonDetails) authentication.getPrincipal();
        final var telemetryMap = Map.of("username", userDetails.getUsername(), "clientId", clientId);

        clientsDetailsService.removeClientDetails(clientId);
        telemetryClient.trackEvent("AuthClientDetailsDeleted", telemetryMap, null);

        return "redirect:/ui";
    }

    // Unfortunately the getAdditionalInformation getter creates an unmodifiable map, so can't be used with web binder.
    // Have to therefore extend and create our own accessor instead.
    public static class AuthClientDetails extends BaseClientDetails {
        public String getJwtFields() {
            return (String) getAdditionalInformation().get("jwtFields");
        }

        public void setJwtFields(final String jwtFields) {
            addAdditionalInformation("jwtFields", jwtFields);
        }
    }
}
