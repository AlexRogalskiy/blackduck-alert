package com.synopsys.integration.alert.web.security.authentication;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.common.exception.AlertLDAPConfigurationException;
import com.synopsys.integration.alert.common.rest.model.UserModel;
import com.synopsys.integration.alert.database.user.UserRole;
import com.synopsys.integration.alert.web.security.authentication.ldap.LdapManager;

@Component
public class AlertAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(AlertAuthenticationProvider.class);
    private final DaoAuthenticationProvider alertDatabaseAuthProvider;
    private final LdapManager ldapManager;

    @Autowired
    public AlertAuthenticationProvider(final DaoAuthenticationProvider alertDatabaseAuthProvider, final LdapManager ldapManager) {
        this.alertDatabaseAuthProvider = alertDatabaseAuthProvider;
        this.ldapManager = ldapManager;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            throw new IllegalArgumentException("Only UsernamePasswordAuthenticationToken is supported, " + authentication.getClass() + " was attempted");
        }
        Authentication authenticationResult = performLdapAuthentication(authentication);
        if (!authenticationResult.isAuthenticated()) {
            authenticationResult = performDatabaseAuthentication(authentication);
        }
        final Collection<? extends GrantedAuthority> authorities = isAuthorized(authenticationResult) ? authenticationResult.getAuthorities() : List.of();
        final UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(authenticationResult.getPrincipal(), authenticationResult.getCredentials(), authorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        return authenticationToken;
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private Authentication performDatabaseAuthentication(final Authentication pendingAuthentication) {
        logger.info("Attempting database authentication...");
        return alertDatabaseAuthProvider.authenticate(pendingAuthentication);
    }

    private Authentication performLdapAuthentication(final Authentication pendingAuthentication) {
        logger.info("Checking ldap based authentication...");
        Authentication result;
        if (ldapManager.isLdapEnabled()) {
            logger.info("LDAP authentication enabled");
            try {
                final LdapAuthenticationProvider authenticationProvider = ldapManager.getAuthenticationProvider();
                result = authenticationProvider.authenticate(pendingAuthentication);
            } catch (final AlertLDAPConfigurationException ex) {
                logger.error("LDAP Configuration error", ex);
                result = pendingAuthentication;
            } catch (final Exception ex) {
                logger.error("LDAP Authentication error", ex);
                result = pendingAuthentication;
            }
        } else {
            logger.info("LDAP authentication disabled");
            result = pendingAuthentication;
        }
        return result;
    }

    private boolean isAuthorized(final Authentication authentication) {
        final EnumSet<UserRole> allowedRoles = EnumSet.allOf(UserRole.class);
        return authentication.getAuthorities().stream()
                   .map(GrantedAuthority::getAuthority)
                   .filter(role -> role.startsWith(UserModel.ROLE_PREFIX))
                   .map(role -> StringUtils.substringAfter(role, UserModel.ROLE_PREFIX))
                   .anyMatch(roleName -> allowedRoles.contains(UserRole.valueOf(roleName)));
    }
}
