plugins {
    application
    id("com.google.cloud.tools.jib")
}

fun getCheckedOutGitCommitHash(): String {
    val gitFolder = "${project.rootDir}/.git/"
    val takeFromHash = 12
    /*
     * '.git/HEAD' contains either
     *      in case of detached head: the currently checked out commit hash
     *      otherwise: a reference to a file containing the current commit hash
     */
    val head = File(gitFolder + "HEAD").readText().split(":") // .git/HEAD
    val isCommit = head.size == 1 // e5a7c79edabbf7dd39888442df081b1c9d8e88fd
    // val isRef = head.length > 1     // ref: refs/heads/master

    return if (isCommit) {
        head[0].trim().take(takeFromHash)
    } else { // e5a7c79edabb
        val refHead = File(gitFolder + head[1].trim()) // .git/refs/heads/master
        refHead.readText().trim().take(takeFromHash)
    }
}

jib {
    val tag = getCheckedOutGitCommitHash()
    from {
        image = "gcr.io/distroless/java17:latest"
    }
    to {
        val imageName = project.path.substringAfter(':').replace(':', '-')
        image = "swr.eu-de.otc.t-systems.com/zbw-dev/${imageName}"
        tags = setOf(tag, "latest") as MutableSet<String>
    }
}
