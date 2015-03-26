package com.github.torstenwerner;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GithubToken {
    private final String accessToken;
    private final String scope;
    private final String tokenType;

    public GithubToken(@JsonProperty("access_token") String accessToken,
                       @JsonProperty("scope") String scope,
                       @JsonProperty("token_type") String tokenType) {
        this.accessToken = accessToken;
        this.scope = scope;
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getScope() {
        return scope;
    }

    public String getTokenType() {
        return tokenType;
    }

    @Override
    public String toString() {
        return "TokenHolder{" +
                "accessToken='" + accessToken + '\'' +
                ", scope='" + scope + '\'' +
                ", tokenType='" + tokenType + '\'' +
                '}';
    }
}
