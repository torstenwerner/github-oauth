package com.github.torstenwerner;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;

public interface AuthService {
    ResponseEntity<GithubUser> fetchUserData();

    ResponseEntity<GithubUser> authorize();

    ResponseEntity<GithubUser> fetchAndStoreAccessToken();

    class GithubUser extends HashMap<String, Object> {
    }
}
