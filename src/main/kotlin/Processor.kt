import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.regex.Pattern

class Processor(statisticsFile: File, stateFile: File, expectedHeader: String) {
    private val _statisticsFile = statisticsFile
    private val _stateFile = stateFile
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
        } else {
            stateFile.writeText("$expectedHeader\n")
            statisticsFile.writeText("")
        }
    }

    suspend fun processFile(file: File, numberOfFiles: Int) {
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
        _mutex.withLock {
            updateStatistics(counter)
            updateState(file, numberOfFiles)
        }
    }

    private fun updateStatistics(counter: MutableMap<String, Int>) {
        counter.forEach { (word, count) ->
            _word2Cnt[word] = _word2Cnt[word]?.plus(count) ?: count
        }
        _statisticsFile.bufferedWriter().use {writer ->
            _word2Cnt.toSortedMap().forEach { (word, count) -> writer.write("$word: $count\n") }
        }
    }

    private fun updateState(file: File, numberOfFiles: Int) {
        _stateFile.appendText("${file.path}\n")
        _processedFiles.add(file.path)
        println("[${_processedFiles.count()}/$numberOfFiles] Processed file ${file.path}")
    }

    private val _mutex = Mutex()
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