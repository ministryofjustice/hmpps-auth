package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

}
