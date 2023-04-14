import java.io.File
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    val directoryPath = args[0]
    val statisticsPath = args[1]
    val concurrencyLevel = 10
    val header = "$directoryPath $statisticsPath"
    val files = File(directoryPath).walk().filter {
        it.absolutePath.endsWith(".java") && !it.absolutePath.contains("test/")
    }
    val statisticsFile = File(statisticsPath)
    if (!statisticsFile.exists()) {
        statisticsFile.createNewFile()
    }
    val stateFile = File("state.txt")
    if (!stateFile.exists()) {
        stateFile.createNewFile()
        stateFile.writeText("$header\n")
    }
    val processor = Processor(statisticsFile, stateFile, header, files.count())
    val executor = Executors.newFixedThreadPool(concurrencyLevel)

    files.forEach { file ->
        executor.submit {
            processor.processFile(file)
        }
    }
    executor.shutdown()
}