// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.gradle

import org.gradle.api.Project

object IdeVersions {
    fun ideProfile(project: Project): Profile {
        val profileName = resolveIdeProfileName(project)
        return ideProfiles[profileName] ?: throw IllegalStateException("Can't find profile for $profileName")
    }

    private val ideProfiles = listOf(
        Profile(
            name = "2019.3",
            communityPlugins = listOf(
                "java",
                "com.intellij.gradle",
                "org.jetbrains.idea.maven",
                "PythonCore:193.5233.139",
                "Docker:193.5233.140"
            ),
            ultimatePlugins = listOf(
                "JavaScript",
                "JavaScriptDebugger",
                "Pythonid:193.5233.109"
            ),
            riderSdkOverride = "2019.3.4",
            rdGenVersion = "0.193.146",
            nugetVersion = "2019.3.4"
        ),
        Profile(
            name = "2020.1",
            communityPlugins = listOf(
                "java",
                "com.intellij.gradle",
                "org.jetbrains.idea.maven",
                "PythonCore:201.6668.31",
                "Docker:201.6668.30"
            ),
            ultimatePlugins = listOf(
                "JavaScript",
                "JavaScriptDebugger",
                "com.intellij.database",
                "Pythonid:201.6668.31"
            ),
            rdGenVersion = "0.203.144",
            nugetVersion = "2020.1.0"
        ),
        Profile(
            name = "2020.2",
            communityPlugins = listOf(
                "java",
                "com.intellij.gradle",
                "org.jetbrains.idea.maven",
                "PythonCore:202.6397.124",
                "Docker:202.6397.93"
            ),
            ultimatePlugins = listOf(
                "JavaScript",
                "JavaScriptDebugger",
                "com.intellij.database",
                "Pythonid:202.6397.98"
            ),
            rdGenVersion = "0.203.144",
            nugetVersion = "2020.2.0"
        ),
        Profile(
            name = "2020.3",
            communityPlugins = listOf(
                "java",
                "com.intellij.gradle",
                "org.jetbrains.idea.maven",
                "PythonCore:203.4818.41",
                "Docker:203.4818.26"
            ),
            ultimatePlugins = listOf(
                "JavaScript",
                "JavaScriptDebugger",
                "com.intellij.database",
                "Pythonid:203.4818.26"
            ),
            ijSdkOverride = "203.4818.26-EAP-SNAPSHOT",
            riderSdkOverride = "2020.3-SNAPSHOT",
            rdGenVersion = "0.203.144",
            nugetVersion = "2020.3.0-eap03"
        )
    ).associateBy { it.name }

    private fun resolveIdeProfileName(project: Project): String = if (System.getenv()["ALTERNATIVE_IDE_PROFILE_NAME"] != null) {
        System.getenv("ALTERNATIVE_IDE_PROFILE_NAME")
    } else {
        project.properties["ideProfileName"]?.toString() ?: throw IllegalStateException("No ideProfileName property set")
    }
}

open class ProductProfile(
    val sdkVersion: String,
    val plugins: Array<String>
)

class RiderProfile(
    sdkVersion: String,
    plugins: Array<String>,
    val rdGenVersion: String,
    val nugetVersion: String
) : ProductProfile(sdkVersion, plugins)

class Profile(
    val name: String,
    val shortName: String = shortenedIdeProfileName(name),
    val sinceVersion: String = shortName,
    val untilVersion: String = "$sinceVersion.*",
    communityPlugins: List<String>,
    ultimatePlugins: List<String>,
    ijSdkOverride: String? = null,
    riderSdkOverride: String? = null,
    rdGenVersion: String, // https://www.myget.org/feed/rd-snapshots/package/maven/com.jetbrains.rd/rd-gen
    nugetVersion: String // https://www.nuget.org/packages/JetBrains.Rider.SDK/
) {
    private val commonPlugins = arrayOf(
        "org.jetbrains.plugins.terminal",
        "org.jetbrains.plugins.yaml"
    )

    val community: ProductProfile = ProductProfile(sdkVersion = "IC-${ijSdkOverride ?: name}", plugins = commonPlugins + communityPlugins)
    val ultimate: ProductProfile = ProductProfile(sdkVersion = "IU-${ijSdkOverride ?: name}", plugins = commonPlugins + ultimatePlugins)
    val rider: RiderProfile = RiderProfile(
        sdkVersion = "RD-${riderSdkOverride ?: name}",
        plugins = arrayOf("org.jetbrains.plugins.yaml"),
        rdGenVersion = rdGenVersion,
        nugetVersion = nugetVersion
    )
}

private fun shortenedIdeProfileName(sdkName: String): String {
    val parts = sdkName.trim().split(".")
    return parts[0].substring(2) + parts[1]
}
