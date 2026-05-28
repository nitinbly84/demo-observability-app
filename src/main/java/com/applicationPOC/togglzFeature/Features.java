package com.applicationPOC.togglzFeature;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

// Strategy-specific imports
import org.togglz.core.activation.GradualActivationStrategy;
import org.togglz.core.activation.ScriptEngineActivationStrategy;
import org.togglz.core.activation.SystemPropertyActivationStrategy;
import org.togglz.core.activation.UsernameActivationStrategy;
import org.togglz.core.annotation.ActivationParameter;
import org.togglz.core.annotation.DefaultActivationStrategy;

// This enum defines all the feature toggles in the application, along with their activation strategies and labels for the admin console.
public enum Features implements Feature {

    // ── 1. ALWAYS ON for everyone ────────────────────────────────────────────
    @Label("New Homepage Banner")
    @EnabledByDefault           // active for ALL users, no strategy needed
    IS_FEATURE1_ENABLED,

    // ── 2. ADMIN ONLY ────────────────────────────────────────────────────────
    // Uses ScriptEngine strategy — evaluates a Groovy/JS expression.
    // user.getAttribute("role") reads what we set in SpringSecurityUserProvider.
    @Label("Admin Dashboard v2")
    @DefaultActivationStrategy(
        id = ScriptEngineActivationStrategy.ID,
        parameters = {
            @ActivationParameter(
                name  = ScriptEngineActivationStrategy.PARAM_LANG,
                value = "groovy"
            ),
            @ActivationParameter(
                name  = ScriptEngineActivationStrategy.PARAM_SCRIPT,
                // user object is injected automatically by Togglz
                value = "user.getAttribute('role') == 'ADMIN'"
            )
        }
    )
    IS_FEATURE2_ENABLED,

    // ── 3. REGULAR USERS ONLY (not admin) ───────────────────────────────────
    @Label("New Checkout Flow for Users")
    @DefaultActivationStrategy(
        id = ScriptEngineActivationStrategy.ID,
        parameters = {
            @ActivationParameter(
                name  = ScriptEngineActivationStrategy.PARAM_LANG,
                value = "Groovy"
            ),
            @ActivationParameter(
                name  = ScriptEngineActivationStrategy.PARAM_SCRIPT,
                value = "user.getAttribute('role') == 'USER'"
            )
        }
    )
    IS_FEATURE3_ENABLED,

    // ── 4. GRADUAL ROLLOUT — 30% of requests ────────────────────────────────
    // GradualActivationStrategy hashes username (or session id) and enables
    // the feature for that percentage. Same user always gets same result.
    @Label("New Recommendation Engine — 30% rollout")
    @DefaultActivationStrategy(
        id = GradualActivationStrategy.ID,
        parameters = {
            @ActivationParameter(
                name  = GradualActivationStrategy.PARAM_PERCENTAGE,
                value = "30"           // ← change to 50, 75, 100 as confidence grows
            )
        }
    )
    IS_FEATURE4_ENABLED,

    // ── 5. GRADUAL ROLLOUT — 30%, but ADMIN always gets it ──────────────────
    // Combine gradual + role check in one Groovy script
    @Label("New Payment Gateway — 30% users, 100% admins")
    @DefaultActivationStrategy(
        id = ScriptEngineActivationStrategy.ID,
        parameters = {
            @ActivationParameter(
                name  = ScriptEngineActivationStrategy.PARAM_LANG,
                value = "Groovy"
            ),
            @ActivationParameter(
                name  = ScriptEngineActivationStrategy.PARAM_SCRIPT,
                // Admins always see it; regular users only if hashcode falls in 30%
                value = "user.getAttribute('role') == 'ADMIN' || " +
                        "Math.abs(user.getName().hashCode() % 100) < 30"
            )
        }
    )
    IS_FEATURE5_ENABLED,

    // ── 6. SPECIFIC USERNAMES (beta testers, internal QA) ───────────────────
    @Label("Experimental Search — internal beta users only")
    @DefaultActivationStrategy(
        id = UsernameActivationStrategy.ID,
        parameters = {
            @ActivationParameter(
                name  = UsernameActivationStrategy.PARAM_USERS,
                value = "alice, bob, qa-team-user, nitin"   // comma-separated
            )
        }
    )
    IS_FEATURE6_ENABLED,

    // ── 7. TIME-BASED — active after a specific date/time ───────────────────
    // No built-in date strategy in Togglz core, but ScriptEngine handles it cleanly
    @Label("Summer Sale Banner — active from 2025-06-01")
    @DefaultActivationStrategy(
        id = ScriptEngineActivationStrategy.ID,
        parameters = {
            @ActivationParameter(
                name  = ScriptEngineActivationStrategy.PARAM_LANG,
                value = "Groovy"
            ),
            @ActivationParameter(
                    name  = ScriptEngineActivationStrategy.PARAM_SCRIPT,
                    value = "java.time.LocalDate.now().isAfter(java.time.LocalDate.parse('2026-05-27'))"
                    // Evaluates to true starting exactly on 2026-05-27
                )
        }
    )
    IS_FEATURE7_ENABLED,

    // ── 8. TIME WINDOW — active between two dates ───────────────────────────
    @Label("Black Friday Feature — 28 Nov to 2 Dec only")
    @DefaultActivationStrategy(
        id = ScriptEngineActivationStrategy.ID,
        parameters = {
            @ActivationParameter(
                name  = ScriptEngineActivationStrategy.PARAM_LANG,
                value = "Groovy"
            ),
            @ActivationParameter(
                name  = ScriptEngineActivationStrategy.PARAM_SCRIPT,
                value = "def now = new Date(); " +
                		"java.time.LocalDate.now().isAfter(java.time.LocalDate.parse('2026-05-27')) && " +
                		"java.time.LocalDate.now().isBefore(java.time.LocalDate.parse('2026-05-29'))"
                // month 10 = November, month 11 = December
            )
        }
    )
    
    IS_FEATURE8_ENABLED,

    // ── 9. SYSTEM PROPERTY — ops kill-switch via env var ────────────────────
    // java -DENABLE_DARK_MODE=true  or  export ENABLE_DARK_MODE=true
    // Useful for ops team to flip without any code change or Redis update
    @Label("Dark Mode — ops environment toggle")
    @DefaultActivationStrategy(
        id = SystemPropertyActivationStrategy.ID,
        parameters = {
            @ActivationParameter(
                name  = SystemPropertyActivationStrategy.PARAM_PROPERTY_NAME,
                value = "ENABLE_DARK_MODE"
            ),
            @ActivationParameter(
                name  = SystemPropertyActivationStrategy.PARAM_PROPERTY_VALUE,
                value = "true"
            )
        }
    )
    IS_FEATURE9_ENABLED;


    // ── Convenience method — use this in your service/controller ─────────────
    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}