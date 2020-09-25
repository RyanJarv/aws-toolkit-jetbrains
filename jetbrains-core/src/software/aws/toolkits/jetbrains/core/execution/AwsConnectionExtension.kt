// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.util.Key
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import software.aws.toolkits.core.utils.error
import software.aws.toolkits.core.utils.getLogger
import software.aws.toolkits.core.utils.tryOrNull
import software.aws.toolkits.jetbrains.core.credentials.AwsConnectionManager
import software.aws.toolkits.jetbrains.core.credentials.ConnectionSettings
import software.aws.toolkits.jetbrains.core.credentials.CredentialManager
import software.aws.toolkits.jetbrains.core.credentials.toEnvironmentVariables
import software.aws.toolkits.jetbrains.core.region.AwsRegionProvider
import software.aws.toolkits.jetbrains.settings.AwsSettings
import software.aws.toolkits.jetbrains.settings.InjectCredentials
import software.aws.toolkits.resources.message
import software.aws.toolkits.telemetry.AwsTelemetry
import software.aws.toolkits.telemetry.Result.Failed
import software.aws.toolkits.telemetry.Result.Succeeded

class AwsConnectionRunConfigurationExtension<T : RunConfigurationBase<*>> {
    private val regionProvider = AwsRegionProvider.getInstance()
    private val credentialManager = CredentialManager.getInstance()

    fun addEnvironmentVariables(configuration: T, environment: MutableMap<String, String>, runtimeString: () -> String? = { null }) {
        val credentialConfiguration = configuration.getCopyableUserData(AWS_CONNECTION_RUN_CONFIGURATION_KEY) ?: run {
            // if the user just runs without opening and saving the settings, this never gets set. So, we have to check for it here as well
            if (AwsSettings.getInstance().injectRunConfigurations == InjectCredentials.On) {
                AwsCredInjectionOptions { useCurrentConnection = true }
            } else {
                null
            }
        } ?: return

        try {
            val connection = if (credentialConfiguration.useCurrentConnection) {
                AwsConnectionManager.getInstance(configuration.project).connectionSettings() ?: throw RuntimeException(message("configure.toolkit"))
            } else {
                val region = credentialConfiguration.region?.let {
                    regionProvider.allRegions()[it]
                } ?: throw IllegalStateException(message("configure.validate.no_region_specified"))

                val credentialProviderId = credentialConfiguration.credential ?: throw IllegalStateException(message("aws.notification.credentials_missing"))

                val credentialProvider = credentialManager.getCredentialIdentifierById(credentialProviderId)?.let {
                    credentialManager.getAwsCredentialProvider(it, region)
                } ?: throw RuntimeException(message("aws.notification.credentials_missing"))

                ConnectionSettings(credentialProvider, region)
            }

            connection.toEnvironmentVariables().forEach { (key, value) -> environment[key] = value }
            AwsTelemetry.injectCredentials(configuration.project, result = Succeeded, runtimestring = tryOrNull { runtimeString() })
        } catch (e: Exception) {
            AwsTelemetry.injectCredentials(configuration.project, result = Failed, runtimestring = tryOrNull { runtimeString() })
            LOG.error(e) { message("run_configuration_extension.inject_aws_connection_exception") }
        }
    }

    fun readExternal(runConfiguration: T, element: Element) {
        runConfiguration.putCopyableUserData(
            AWS_CONNECTION_RUN_CONFIGURATION_KEY,
            XmlSerializer.deserialize(
                element,
                AwsCredInjectionOptions::class.java
            )
        )
    }

    fun writeExternal(runConfiguration: T, element: Element) {
        runConfiguration.getCopyableUserData(AWS_CONNECTION_RUN_CONFIGURATION_KEY)?.let {
            XmlSerializer.serializeInto(it, element)
        }
    }

    fun isApplicable(): Boolean = AwsSettings.getInstance().injectRunConfigurations != InjectCredentials.Never

    private companion object {
        val LOG = getLogger<AwsConnectionRunConfigurationExtension<*>>()
    }
}

fun <T : RunConfigurationBase<*>> AwsConnectionRunConfigurationExtension<T>.addEnvironmentVariables(
    configuration: T,
    cmdLine: GeneralCommandLine,
    runtimeString: () -> String? = { null }
) = addEnvironmentVariables(configuration, cmdLine.environment, runtimeString)

fun <T : RunConfigurationBase<*>?> connectionSettingsEditor(configuration: T): AwsConnectionExtensionSettingsEditor<T>? =
    configuration?.getProject()?.let { AwsConnectionExtensionSettingsEditor(it) }

val AWS_CONNECTION_RUN_CONFIGURATION_KEY =
    Key.create<AwsCredInjectionOptions>(
        "aws.toolkit.runConfigurationConnection"
    )

class AwsCredInjectionOptions {
    var useCurrentConnection: Boolean = false
    var region: String? = null
    var credential: String? = null

    companion object {
        operator fun invoke(block: AwsCredInjectionOptions.() -> Unit) = AwsCredInjectionOptions().apply(block)
    }
}
