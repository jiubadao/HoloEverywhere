package org.holoeverywhere.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.builder.model.SigningConfig
import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact
import org.gradle.internal.reflect.Instantiator
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.holoeverywhere.plugin.extension.HoloEverywhereExtension

import javax.inject.Inject

class HoloEverywhereAppPlugin extends HoloEverywhereBasePlugin {
    private HoloEverywhereExtension extension
    private AppExtension androidExtension

    @Inject
    HoloEverywhereAppPlugin(Instantiator instantiator, ToolingModelBuilderRegistry registry) {
        super(instantiator, registry)
    }

    @Override
    void apply(Project project) {
        checkPluginOrder(project)
        loadMainPlugin(project)
        extension = extension(project)
        extension.upload.packaging = 'apk'
        project.afterEvaluate { afterEvaluate(project) }
        androidExtension = project.plugins.apply(AppPlugin).extension
    }

    def void afterEvaluate(Project project) {
        if (extension.signing.enable) {
            if (extension.signing.release.valid()) {
                createSigningConfig(extension.signing.release.obtainConfig('release'), 'release')
            }
            createSigningConfig(extension.signing.debug.obtainMaybeDebugConfig('debug'), 'debug')
        }

        final List<Object> artifacts = new ArrayList<>()
        if (extension.app.attachReleaseApk && extension.signing.release.valid()) artifacts.add(configureApk(project, 'release'))
        artifacts.each { artifact -> project.artifacts.add('archives', artifact) }
    }

    def static PublishArtifact configureApk(Project project, String type) {
        return new DefaultPublishArtifact(project.name, 'apk', 'apk',
                '', new Date(), project.file("${project.buildDir}/apk/${project.name}-${type}.apk"),
                project.tasks.getByName('assembleRelease'))
    }

    def void createSigningConfig(SigningConfig signingConfig, String name) {
        androidExtension.signingConfigs.add(signingConfig)
        androidExtension.buildTypes.getByName(name).setSigningConfig(signingConfig)
    }
}