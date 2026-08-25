plugins {
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}

version = project.mod_version
group = 'com.villageevolution.mod'

base {
    archivesName = project.mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

println "Java: ${System.getProperty 'java.version'}, JVM: ${System.getProperty 'java.vm.version'} (${System.getProperty 'java.vendor'}), Arch: ${System.getProperty 'os.arch'}"

minecraft {
    // Mojang's official mappings. Runtime + dev environment both resolve
    // plain field/method names like "goalSelector" thanks to this.
    mappings channel: 'official', version: project.minecraft_version

    runs {
        client {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            mods {
                villageevolution {
                    source sourceSets.main
                }
            }
        }
        server {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            mods {
                villageevolution {
                    source sourceSets.main
                }
            }
        }
    }
}

sourceSets.main.resources { srcDir 'src/main/resources' }

repositories {
    mavenCentral()
}

dependencies {
    minecraft "net.minecraftforge:forge:${project.minecraft_version}-${project.forge_version}"
}

tasks.named('processResources', ProcessResources).configure {
    var replaceProperties = [
        minecraft_version: project.minecraft_version, minecraft_version_range: project.minecraft_version_range,
        forge_version: project.forge_version, forge_version_range: project.forge_version_range,
        loader_version_range: project.loader_version_range,
        mod_id: project.mod_id, mod_name: project.mod_name,
        mod_license: project.mod_license, mod_version: project.mod_version,
        mod_authors: project.mod_authors, mod_description: project.mod_description
    ]
    inputs.properties replaceProperties
    filesMatching(['META-INF/mods.toml']) {
        expand replaceProperties + [project: project]
    }
}
