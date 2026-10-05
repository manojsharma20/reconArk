plugins { id("reconark.library") }

description = "reconArk SPI: the extension points (Lego sockets) and their contracts."

dependencies {
    api(project(":kernel:plugin-api"))
    api(project(":kernel:pipeline"))
    api(project(":domain:model"))
}
