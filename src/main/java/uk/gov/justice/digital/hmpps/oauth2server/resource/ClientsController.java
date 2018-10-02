package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import uk.gov.justice.digital.hmpps.oauth2server.config.AuthorityPropertyEditor;
import uk.gov.justice.digital.hmpps.oauth2server.config.SplitCollectionEditor;

import java.util.Collection;
import java.util.Set;

@Controller
@RequestMapping("ui/clients")
public class ClientsController {

    @Autowired
    private JdbcClientDetailsService clientsDetailsService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Collection.class, new SplitCollectionEditor(Set.class, ","));
        binder.registerCustomEditor(GrantedAuthority.class, new AuthorityPropertyEditor());
    }

    @GetMapping(value = "/form")
    @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
    public String showEditForm(@RequestParam(value = "client", required = false) String clientId, Model model) {

        ClientDetails clientDetails;
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
            @ModelAttribute BaseClientDetails clientDetails,
            @RequestParam(value = "newClient", required = false) String newClient) {

        if (newClient == null) {
            clientsDetailsService.updateClientDetails(clientDetails);
        } else {
            clientsDetailsService.addClientDetails(clientDetails);
        }

        if (!clientDetails.getClientSecret().isEmpty()) {
            clientsDetailsService.updateClientSecret(clientDetails.getClientId(), clientDetails.getClientSecret());
        }
        return "redirect:/ui";
    }

    @GetMapping(value = "/{clientId}/delete")
    @PreAuthorize("hasRole('ROLE_OAUTH_ADMIN')")
    public String deleteClient(@PathVariable String clientId) {
        clientsDetailsService.removeClientDetails(clientId);
        return "redirect:/ui";
    }
}
