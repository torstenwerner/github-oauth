package com.github.torstenwerner;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class AuthController {
    public static final String CLIENT_ID = "48b43b73da29bfe8a80d";
    public static final String CLIENT_SECRET = "413feedaddc948cd18daa6b9a9efae4462534e6d";

    private final RestOperations restOperations = new RestTemplate();

    public static class GithubUser extends HashMap<String, Object> {
    }

    @RequestMapping("/auth")
    public ResponseEntity<GithubUser> auth(@CookieValue(value = "github_token", required = false) String accessToken,
                                           HttpSession session, @RequestParam(required = false) String code,
                                           @RequestParam(required = false) Integer state,
                                           HttpServletResponse response) throws URISyntaxException {
        if (accessToken != null) {
            return fetchUserData(session, accessToken, response);
        }

        if (state == null) {
            return authorize(session, response);
        }

        checkState(session, code, state);
        accessToken = fetchAccessToken(code);
        setTokenCookie(accessToken, response);
        return redirectTo("http://localhost:8080/auth");
    }

    private ResponseEntity<GithubUser> fetchUserData(HttpSession session, String accessToken, HttpServletResponse response) {
        try {
            final GithubUser user = restOperations.getForObject("https://api.github.com/user?access_token=" + accessToken,
                    GithubUser.class);
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return new ResponseEntity<>(user, headers, HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
                return authorize(session, response);
            }
            throw e;
        }
    }

    private ResponseEntity<GithubUser> authorize(HttpSession session, HttpServletResponse response) {
        final Integer github_state = ThreadLocalRandom.current().nextInt();
        session.setAttribute("github_state", github_state);
        setTokenCookie(null, response);
        return redirectTo("https://github.com/login/oauth/authorize?client_id=" + CLIENT_ID + "&state=" + github_state);
    }

    private ResponseEntity<GithubUser> redirectTo(String url) {
        final HttpHeaders headers = new HttpHeaders();
        try {
            final URI authorizeUri = new URI(url);
            headers.setLocation(authorizeUri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("syntax error", e);
        }
        return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
    }

    private void checkState(HttpSession session, String code, Integer state) {
        final Object sessionState = session.getAttribute("github_state");
        if (sessionState == null) {
            throw new RuntimeException("we have no state in session");
        }
        if (!state.equals(sessionState)) {
            throw new RuntimeException("state parameter is wrong, was: " + sessionState);
        }
        if (code == null) {
            throw new RuntimeException("we got no code");
        }
    }

    private String fetchAccessToken(String code) {
        final GithubToken githubToken = restOperations.postForObject("https://github.com/login/oauth/access_token?client_id="
                + CLIENT_ID + "&client_secret=" + CLIENT_SECRET + "&code=" + code, null, GithubToken.class);
        return githubToken.getAccessToken();
    }

    private void setTokenCookie(String accessToken, HttpServletResponse response) {
        final Cookie cookie = new Cookie("github_token", accessToken);
        final int maxAge = accessToken != null ? 3600 * 24 * 365 : 0;
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
