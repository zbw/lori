rootProject.name = "zbwcloud"
include("app:lori:api")
include("app:lori:client")
include("app:lori:server")
include("app:lori:server:ui")
include("job:lori_import")
include("job:template_apply")
include("job:refresh_materialized_views")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.5.0")
}
