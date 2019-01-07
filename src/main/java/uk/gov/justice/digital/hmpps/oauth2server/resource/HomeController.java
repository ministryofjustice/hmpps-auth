package uk.gov.justice.digital.hmpps.oauth2server.resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {
    private final String nnUrl;
    private final String hdcUrl;

    public HomeController(@Value("${application.nn-endpoint-url}") final String nnUrl,
                          @Value("${application.hdc-endpoint-url}") final String hdcUrl) {
        this.nnUrl = nnUrl;
        this.hdcUrl = hdcUrl;
    }

    @GetMapping("/")
    public ModelAndView home() {
        final var modelAndView = new ModelAndView("index");
        if (StringUtils.isNotBlank(nnUrl)) {
            modelAndView.addObject("nnUrl", nnUrl);
        }
        if (StringUtils.isNotBlank(hdcUrl)) {
            modelAndView.addObject("hdcUrl", hdcUrl);
        }
        return modelAndView;
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

}
