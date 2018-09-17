package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UiController {

    @GetMapping("/ui")
    public String userIndex() {
        return "ui/index";
    }

}
