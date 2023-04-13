import kotlinx.coroutines.*
import java.io.File

suspend fun main(args: Array<String>) {
    val directoryPath = args[0]
    val statisticsPath = args[1]
    val header = "$directoryPath $statisticsPath"
    val files = File(directoryPath).walk().filter {
        it.absolutePath.endsWith(".java") && !it.absolutePath.contains("test/")
    }
    val statisticsFile = File(statisticsPath)
    if (!statisticsFile.exists()) {
        withContext(Dispatchers.IO) {
            statisticsFile.createNewFile()
        }
    }
    val stateFile = File("state.txt")
    if (!stateFile.exists()) {
        withContext(Dispatchers.IO) {
            stateFile.createNewFile()
            stateFile.writeText("$header\n")
        }
    }
    val processor = Processor(statisticsFile, stateFile, header)

    println("Begin processing")
    coroutineScope {
        val launches = mutableListOf<Job>()
        files.forEach { file ->
            launches.add(launch {
                processor.processFile(file, files.count())
            })
        }
        launches.joinAll()
    }
    println("End processing")
}