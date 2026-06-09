package com.finance.payment.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final MerchantPrincipal principal;

    public ApiKeyAuthentication(MerchantPrincipal principal) {
        super(AuthorityUtils.createAuthorityList("ROLE_MERCHANT"));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public MerchantPrincipal getPrincipal() {
        return principal;
    }
}
