plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

version = "0.3.19+mc1.21.1"
group = "com.itrpminimal"

base {
    archivesName.set("itrp-minimal-capture-neoforge")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

neoForge {
    version = "21.1.193"
    mods {
        register("itrp_minimal_capture") {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}
