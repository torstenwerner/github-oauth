package com.github.torstenwerner;

import com.github.torstenwerner.AuthService.GithubUser;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
public class AuthController {
    @RequestMapping("/auth")
    public ResponseEntity<GithubUser> auth(@CookieValue(value = "github_token", required = false) String accessToken,
                                           HttpSession session, @RequestParam(required = false) String code,
                                           @RequestParam(required = false) Integer state,
                                           HttpServletResponse response) {
        final AuthService service = new DefaultAuthService(accessToken, session, code, state, response);

        if (accessToken != null) {
            return service.fetchUserData();
        }

        if (state == null) {
            return service.authorize();
        }

        return service.fetchAndStoreAccessToken();
    }
}
