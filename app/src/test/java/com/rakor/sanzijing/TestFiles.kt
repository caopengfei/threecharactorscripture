package com.rakor.sanzijing

import java.io.File

/** JVM 单测在 app/ 模块目录运行；此工具向上查找，兼容从项目根运行的情况。 */
object TestFiles {
    fun moduleFile(relPath: String): File {
        var dir: File? = File(System.getProperty("user.dir"))
        repeat(4) {
            val f = File(dir, relPath)
            if (f.exists()) return f
            dir = dir?.parentFile
        }
        error("$relPath not found from ${System.getProperty("user.dir")}")
    }
}
