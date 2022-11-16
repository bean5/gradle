/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.configurations;

import org.gradle.api.Action;
import org.gradle.internal.Actions;

/**
 * Extends {@link ConfigurationContainerInternal} with methods that can use {@link ConfigurationRole}s to
 * define the allowed usage of a configuration at creation time.
 */
public interface RoleBasedConfigurationContainerInternal extends ConfigurationContainerInternal {
    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#INTENDED_CONSUMABLE}.
     */
    ConfigurationInternal consumable(String name, boolean lockRole);

    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#INTENDED_RESOLVABLE}.
     */
    ConfigurationInternal resolvable(String name, boolean lockRole);

    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#INTENDED_BUCKET}.
     */
    ConfigurationInternal bucket(String name, boolean lockRole);

    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#INTENDED_CONSUMABLE} that is <strong>NOT</strong> locked
     * against further usage mutations.
     */
    default ConfigurationInternal consumable(String name) {
        return consumable(name, false);
    }

    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#INTENDED_RESOLVABLE} that is <strong>NOT</strong> locked
     * against further usage mutations.
     */
    default ConfigurationInternal resolvable(String name) {
        return resolvable(name, false);
    }

    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#INTENDED_BUCKET} that is <strong>NOT</strong> locked
     * against further usage mutations.
     */
    default ConfigurationInternal bucket(String name) {
        return bucket(name, false);
    }

    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#DEPRECATED_CONSUMABLE}.
     */
    ConfigurationInternal deprecatedConsumable(String name, boolean lockRole);

    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#DEPRECATED_RESOLVABLE}.
     */
    ConfigurationInternal deprecatedResolvable(String name, boolean lockRole);


    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#DEPRECATED_CONSUMABLE} that is <strong>NOT</strong> locked
     * against further usage mutations.
     */
    default ConfigurationInternal deprecatedConsumable(String name) {
        return deprecatedConsumable(name, false);
    }

    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * using the role of {@link ConfigurationRoles#DEPRECATED_RESOLVABLE} that is <strong>NOT</strong> locked
     * against further usage mutations.
     */
    default ConfigurationInternal deprecatedResolvable(String name) {
        return deprecatedResolvable(name, false);
    }

    /**
     * Creates a new configuration in the same manner as {@link #create(String)} using the given role
     * at creation.
     *
     * @param name the name of the configuration
     * @param role the role defining the configuration's allowed usage
     * @param lockUsage {@code true} if the configuration's allowed usage should be locked to prevent any changes; {@code false} otherwise
     * @param configureAction an action to run upon the configuration's creation to configure it
     * @return the new configuration
     */
    ConfigurationInternal createWithRole(String name, ConfigurationRole role, boolean lockUsage, Action<? super ConfigurationInternal> configureAction);

    /**
     * Creates a new configuration in the same manner as {@link #create(String)} using the given role
     * at creation.
     *
     * @param name the name of the configuration
     * @param role the role defining the configuration's allowed usage
     * @param lockUsage {@code true} if the configuration's allowed usage should be locked to prevent any changes; {@code false} otherwise
     * @return the new configuration
     */
    default ConfigurationInternal createWithRole(String name, ConfigurationRole role, boolean lockUsage) {
        return createWithRole(name, role, lockUsage, Actions.doNothing());
    }

    /**
     * Creates a new configuration in the same manner as {@link #create(String)} using the given role
     * at creation and configuring it with the given action, without automatically locking the configuration's allowed usage.
     *
     * @param name the name of the configuration
     * @param role the role defining the configuration's allowed usage
     * @param configureAction an action to run upon the configuration's creation to configure it
     * @return the new configuration
     */
    default ConfigurationInternal createWithRole(String name, ConfigurationRole role, Action<? super ConfigurationInternal> configureAction) {
        return createWithRole(name, role, false, configureAction);
    }


    /**
     * Creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)}
     * without locking the configuration's allowed usage.
     */
    default ConfigurationInternal createWithRole(String name, ConfigurationRole role) {
        return createWithRole(name, role, false);
    }

    /**
     * If it does not already exist, creates a new configuration in the same manner as {@link #createWithRole(String, ConfigurationRole, boolean)};
     * if the configuration does already exist, this method will <strong>NOT</strong>> change anything about its allowed,
     * including its role, but <strong>CAN</strong> optionally confirm that the current usage of the configuration
     * matches the given role and/or prevent any further changes to the configuration's allowed usage.
     *
     * @param name the name of the configuration
     * @param role the role defining the configuration's allowed usage
     * @param lockUsage {@code true} if the configuration's allowed usage should be locked to prevent any changes; {@code false} otherwise
     * @param assertInRole {@code true} if the configuration's current usage should be confirmed to match that specified by the given role
     * @return the new configuration
     */
    default ConfigurationInternal maybeCreateWithRole(String name, ConfigurationRole role, boolean lockUsage, boolean assertInRole) {
        ConfigurationInternal configuration = (ConfigurationInternal) findByName(name);
        if (configuration == null) {
            return createWithRole(name, role, lockUsage);
        } else {
            if (assertInRole) {
                RoleChecker.assertIsInRole(configuration, role);
            }
            if (lockUsage) {
                configuration.preventUsageMutation();
            }
            return configuration;
        }
    }

    /**
     * This static util class hides methods internal to the {@code default} methods in the {@link ConfigurationContainerInternal} interface.
     */
    abstract class RoleChecker {
        private RoleChecker() { /* not instantiable */ }

        /**
         * Checks that the current allowed usage of a configuration is the same as that specified by a given role.
         *
         * @param configuration the configuration to check
         * @param role the role to check against
         * @return {@code true} if so; {@code false} otherwise
         */
        public static boolean isUsageConsistentWithRole(ConfigurationInternal configuration, ConfigurationRole role) {
            return (role.isConsumable() == configuration.isCanBeConsumed())
                    && (role.isResolvable() == configuration.isCanBeResolved())
                    && (role.isDeclarableAgainst() == configuration.isCanBeDeclaredAgainst())
                    && (role.isConsumptionDeprecated() == configuration.isDeprecatedForConsumption())
                    && (role.isResolutionDeprecated() == configuration.isDeprecatedForResolution())
                    && (role.isDeclarationAgainstDeprecated() == configuration.isDeprecatedForDeclarationAgainst());
        }

        /**
         * Checks that the current allowed usage of a configuration is the same as that specified by a given role,
         * and throws an exception with a message describing the differences if not.
         *
         * @param configuration the configuration to check
         * @param role the role to check against
         */
        public static void assertIsInRole(ConfigurationInternal configuration, ConfigurationRole role) {
            if (!isUsageConsistentWithRole(configuration, role)) {
                throw new IllegalStateException(describeDifferenceFromRole(configuration, role));
            }
        }

        private static String describeDifferenceFromRole(ConfigurationInternal configuration, ConfigurationRole role) {
            if (!isUsageConsistentWithRole(configuration, role)) {
                return "Usage for configuration: " + configuration.getName() + " is not consistent with the role: " + role.getName() + ".\n" +
                        "Expected that it is:\n" +
                        role.describe() + "\n" +
                        "But is actually is:\n" +
                        ConfigurationRole.forConfiguration(configuration).describe();
            } else {
                return "Usage for configuration: " + configuration.getName() + " is consistent with the role: " + role.getName() + ".";
            }
        }
    }
}
