package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class UiController {

    @Autowired
    private JdbcClientDetailsService clientDetailsService;

    @GetMapping("/ui")
    public ModelAndView userIndex(Map<String,Object> model, Principal principal) {
        model.put("clientDetails",clientDetailsService.listClientDetails());
        return new ModelAndView("ui/index", model);
    }

}
