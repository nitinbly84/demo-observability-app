package com.applicationPOC.togglzFeature;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.togglz.core.user.FeatureUser;
import org.togglz.core.user.SimpleFeatureUser;
import org.togglz.core.user.UserProvider;

@Component
public class SpringSecurityUserProvider implements UserProvider {

    @Override
    public FeatureUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            // anonymous request — not an admin
            return new SimpleFeatureUser("anonymous", false);
        }

        String username = auth.getName();

        // second argument = isFeatureAdmin
        // true  → this user can toggle features in the Togglz admin console
        // false → regular user, subject to activation strategy rules
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Pass the role as an attribute so strategies can read it
        SimpleFeatureUser user = new SimpleFeatureUser(username, isAdmin);
        user.setAttribute("role", isAdmin ? "ADMIN" : "USER");

        return user;
    }
}
