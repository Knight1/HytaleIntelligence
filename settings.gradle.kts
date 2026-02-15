rootProject.name = "de.tobiassachs"

//version = "1.0.0"

plugins {
    id("dev.scaffoldit") version "0.2.+"
}

hytale {
    manifest {
        Group = "Knight1"
        Name = "HytaleIntelligence"
        Main = "de.tobiassachs.HytaleIntelligence"
        IncludesAssetPack = true
    }
}