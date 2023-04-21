import sun.misc.Signal
import java.io.File
import java.util.concurrent.Executors
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val directoryPath = args[0]
    val statisticsPath = args[1]
    val concurrencyLevel = 10
    val header = "$directoryPath $statisticsPath"
    val files = File(directoryPath).walk().filter {
        it.absolutePath.endsWith(".java") && it.isFile && !it.readText().contains("@Test")
    }
    val statisticsFile = File(statisticsPath)
    if (!statisticsFile.exists()) {
        statisticsFile.createNewFile()
    }
    val stateFile = File("state.txt")
    if (!stateFile.exists()) {
        stateFile.createNewFile()
    }
    val processor = Processor(statisticsFile, stateFile, header, files.count())
    val executor = Executors.newFixedThreadPool(concurrencyLevel)

    Signal.handle(Signal("INT")) {
        processor.saveStatistics()
        println("Process has been interrupted with sigint")
        exitProcess(0)
    }

    files.forEach { file ->
        executor.submit {
            processor.processFile(file)
        }
    }
    executor.shutdown()
}