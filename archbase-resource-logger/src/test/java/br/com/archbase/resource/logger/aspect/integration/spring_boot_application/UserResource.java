package br.com.archbase.resource.logger.aspect.integration.spring_boot_application;

import br.com.archbase.resource.logger.annotation.Logging;
import br.com.archbase.resource.logger.bean.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Logging
public class UserResource {

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/getUser",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public User getUser() {
        return new User(1, "joao@example.com.br", "secretpassword");
    }
}
