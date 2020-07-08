package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class UiController {

    @Autowired
    private JdbcClientDetailsService clientDetailsService;

    @GetMapping("/ui")
    public ModelAndView userIndex(final Map<String, Object> model) {
        model.put("clientDetails", clientDetailsService.listClientDetails());
        return new ModelAndView("ui/index", model);
    }

}
