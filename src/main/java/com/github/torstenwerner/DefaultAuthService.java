package com.github.torstenwerner;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultAuthService implements AuthService {
    public static final String CLIENT_ID = "48b43b73da29bfe8a80d";
    public static final String CLIENT_SECRET = "413feedaddc948cd18daa6b9a9efae4462534e6d";

    private static final RestOperations restOperations = new RestTemplate();

    private String accessToken;
    final private HttpSession session;
    final private String code;
    final private Integer state;
    final private HttpServletResponse response;

    public DefaultAuthService(String accessToken, HttpSession session, String code, Integer state, HttpServletResponse response) {
        this.accessToken = accessToken;
        this.session = session;
        this.code = code;
        this.state = state;
        this.response = response;
    }

    @Override
    public ResponseEntity<GithubUser> fetchUserData() {
        try {
            final GithubUser user = restOperations.getForObject("https://api.github.com/user?access_token=" + accessToken,
                    GithubUser.class);
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return new ResponseEntity<>(user, headers, HttpStatus.OK);
        } catch (HttpClientErrorException e) {
            if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
                return authorize();
            }
            throw e;
        }
    }

    @Override
    public ResponseEntity<GithubUser> authorize() {
        final Integer github_state = ThreadLocalRandom.current().nextInt();
        session.setAttribute("github_state", github_state);
        accessToken = null;
        setTokenCookie();
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

    private void checkState() {
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

    private void fetchAccessToken() {
        final GithubToken githubToken = restOperations.postForObject("https://github.com/login/oauth/access_token?client_id="
                + CLIENT_ID + "&client_secret=" + CLIENT_SECRET + "&code=" + code, null, GithubToken.class);
        accessToken = githubToken.getAccessToken();
    }

    private void setTokenCookie() {
        final Cookie cookie = new Cookie("github_token", accessToken);
        final int maxAge = accessToken != null ? 3600 * 24 * 365 : 0;
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    @Override
    public ResponseEntity<GithubUser> fetchAndStoreAccessToken() {
        checkState();
        fetchAccessToken();
        setTokenCookie();
        return redirectTo("http://localhost:8080/auth");
    }

    @Override
    public String toString() {
        return "AuthService{" +
                "accessToken='" + accessToken + '\'' +
                ", code='" + code + '\'' +
                ", state=" + state +
                '}';
    }
}
