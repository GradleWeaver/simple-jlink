rootProject.name = "simple-jlink-samples"

include(":samples")
include(":samples:simple-jar")
include(":samples:javafx-app")
include(":samples:groovy-dsl")
include(":samples:modular-app")

includeBuild("./plugin")
