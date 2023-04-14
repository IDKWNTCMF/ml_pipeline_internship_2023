import sun.misc.Signal
import java.io.File
import java.util.regex.Pattern
import kotlin.system.exitProcess

class Processor(statisticsFile: File, stateFile: File, expectedHeader: String, numberOfFiles: Int) {
    private val _statisticsFile = statisticsFile
    private val _stateFile = stateFile
    private val _numberOfFiles = numberOfFiles
    private val _word2Cnt = mutableMapOf<String, Int>()
    private val _processedFiles = mutableSetOf<String>()

    init {
        val lines = stateFile.readLines()
        if (lines.isNotEmpty() && lines.first() == expectedHeader) {
            _processedFiles.addAll(lines.drop(1))
            statisticsFile.useLines { statistics ->
                statistics.forEach { line ->
                    val (keyword, cnt) = line.split(": ")
                    _word2Cnt[keyword] = cnt.toInt()
                }
            }
            println("Continue processing ([${_processedFiles.count()}/$numberOfFiles] files have been preprocessed)")
        } else {
            stateFile.writeText("$expectedHeader\n")
            statisticsFile.writeText("")
            println("Begin processing")
        }
    }

    fun processFile(file: File) {
        if (_processedFiles.contains(file.path)) {
            return
        }
        val counter = mutableMapOf<String, Int>()
        file.useLines { lines ->
            lines.forEach { line ->
                line.split(_pattern).filter { word ->
                    word in _keywords
                }.forEach { word -> counter[word] = counter[word]?.plus(1) ?: 1 }
            }
        }
        update(counter, file)
    }

    @Synchronized
    private fun update(counter: Map<String, Int>, file: File) {
        val statisticsText = _statisticsFile.readText()
        val stateText = _stateFile.readText()
        Signal.handle(Signal("INT")) {
            println("Process has been interrupted with sigint")
            _statisticsFile.writeText(statisticsText)
            _stateFile.writeText(stateText)
            exitProcess(0)
        }

        updateStatistics(counter)
        updateState(file)
    }

    private fun updateStatistics(counter: Map<String, Int>) {
        counter.forEach { (word, count) ->
            _word2Cnt[word] = _word2Cnt[word]?.plus(count) ?: count
        }
        _statisticsFile.bufferedWriter().use {writer ->
            _word2Cnt.toSortedMap().forEach { (word, count) -> writer.write("$word: $count\n") }
        }
    }

    private fun updateState(file: File) {
        _stateFile.appendText("${file.path}\n")
        _processedFiles.add(file.path)
        println("[${_processedFiles.count()}/$_numberOfFiles] Processed file ${file.path}")
        if (_processedFiles.count() == _numberOfFiles) println("End processing")
    }

    private val _pattern = Pattern.compile("[^a-z]")
    private val _keywords = listOf(
        "byte", "short", "int", "long", "char", "float", "double", "boolean",                                                   // primitives
        "if", "else", "switch", "case", "default", "while", "do", "break", "continue", "for",                                   // conditions and loops
        "try", "catch", "finally", "throw", "throws",                                                                           // exceptions
        "private", "protected", "public",                                                                                       // visibility modifiers
        "import", "package", "class", "interface", "extends", "implements", "static", "final", "void", "abstract", "native",    // imports and declarations
        "new", "return", "this", "super",                                                                                       // return
        "synchronized", "volatile",                                                                                             // multithreading
        "const", "goto",                                                                                                        // reserved
        "instanceof", "enum", "assert", "transient", "strictfp"                                                                 // extra
    )
}