plugins { alias(libs.plugins.agp.lib) }

android {
    buildFeatures { aidl = true }

    buildTypes { release { isMinifyEnabled = false } }

    sourceSets {
        named("main") {
            java.srcDirs(
                "manager-service/src/main/java",
                "daemon-service/src/main/java",
                "libxposed/service/src/main"
            )
            aidl.srcDirs(
                "manager-service/src/main/aidl",
                "daemon-service/src/main/aidl",
                "libxposed/interface/src/main/aidl"
            )
        }
    }

    aidlPackagedList += "org/lsposed/lspd/models/Module.aidl"
    namespace = "org.lsposed.lspd.services"
}

dependencies {
    api(libs.rikkax.parcelablelist)
    compileOnly(libs.androidx.annotation)
    compileOnly(projects.shared.libxposedAnnotation)
    compileOnly(projects.hiddenapi.stubs)
}
