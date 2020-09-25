// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.core.execution

import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.testFramework.ProjectRule
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.jdom.Element
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.junit.Rule
import org.junit.Test
import software.aws.toolkits.jetbrains.settings.AwsSettingsRule

class JavaAwsConnectionExtensionTest {

    @Rule
    @JvmField
    val projectRule = ProjectRule()

    @Rule
    @JvmField
    val settingsRule = AwsSettingsRule()

    @Test
    fun `Round trip persistence`() {
        val runManager = RunManager.getInstance(projectRule.project)
        val configuration = runManager.createConfiguration("test", ApplicationConfigurationType::class.java).configuration as ApplicationConfiguration

        val data = AwsCredInjectionOptions {
            region = "abc123"
            credential = "mockCredential"
        }

        configuration.putCopyableUserData(AWS_CONNECTION_RUN_CONFIGURATION_KEY, data)
        configuration.mainClassName = "com.bla.Boop"

        val element = Element("bling")
        configuration.writeExternal(element)

        val deserialized = runManager.createConfiguration("re-read", ApplicationConfigurationType::class.java).configuration as ApplicationConfiguration
        deserialized.readExternal(element)

        assertThat(deserialized.mainClassName).isEqualTo("com.bla.Boop")
        assertThat(deserialized.getCopyableUserData(AWS_CONNECTION_RUN_CONFIGURATION_KEY)).isEqualToComparingFieldByField(data)
    }

    @Test
    fun `ignores gradle based run configs`() {
        val configuration = mock<GradleRunConfiguration>()
        assertThat(JavaAwsConnectionExtension().isApplicableFor(configuration)).isFalse()
    }

    @Test
    fun `doesn't apply when not set`() {

    }

    @Test
    fun `Injects when the global setting is set`() {
        val runManager = RunManager.getInstance(projectRule.project)
        val configuration = runManager.createConfiguration("test", ApplicationConfigurationType::class.java).configuration as ApplicationConfiguration
        val extension = JavaAwsConnectionExtension()

        extension.
    }

    @Test
    fun `Global setting does not overwrite run setting`() {

    }

    @Test
    fun `Global off does not inject`() {

    }
}
