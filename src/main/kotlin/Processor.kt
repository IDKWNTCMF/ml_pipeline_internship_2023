import java.io.File
import java.util.regex.Pattern

class Processor(statisticsFile: File, stateFile: File, expectedHeader: String, numberOfFiles: Int) {
    private val _statisticsFile = statisticsFile
    private val _stateFile = stateFile
    private val _header = expectedHeader
    private val _numberOfFiles = numberOfFiles
    private val _word2Cnt = mutableMapOf<String, Int>()
    private val _processedFiles = mutableSetOf<String>()
    private var _stableState = Pair(mapOf<String, Int>(), setOf<String>())

    init {
        val lines = stateFile.readLines()
        if (lines.isNotEmpty() && lines.first() == expectedHeader) {
            lines.drop(1).forEach { filename ->
                if (File(filename).exists()) {
                    _processedFiles.add(filename)
                }
            }
            statisticsFile.useLines { statistics ->
                statistics.forEach { line ->
                    val (keyword, cnt) = line.split(": ")
                    _word2Cnt[keyword] = cnt.toInt()
                }
            }
            _stableState = Pair(_word2Cnt, _processedFiles)
            println("Continue processing ([${_processedFiles.count()}/$numberOfFiles] files have been preprocessed)")
        } else {
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
        updateStatistics(counter)
        updateState(file)
        _stableState = Pair(_word2Cnt, _processedFiles)
    }

    @Synchronized
    private fun updateStatistics(counter: Map<String, Int>) {
        counter.forEach { (word, count) ->
            _word2Cnt[word] = _word2Cnt[word]?.plus(count) ?: count
        }
    }

    @Synchronized
    private fun updateState(file: File) {
        _processedFiles.add(file.path)
        println("[${_processedFiles.count()}/$_numberOfFiles] Processed file ${file.path}")
        if (_processedFiles.count() == _numberOfFiles) {
            println("End processing")
            saveStatistics(_word2Cnt, _processedFiles)
        }
    }

    @Synchronized
    fun saveStatistics(word2Cnt: Map<String, Int> = _stableState.first, processedFiles: Set<String> = _stableState.second) {
        _statisticsFile.bufferedWriter().use { writer ->
            word2Cnt.toSortedMap().forEach { (word, count) -> writer.write("$word: $count\n") }
        }
        _stateFile.bufferedWriter().use { writer ->
            writer.write("$_header\n")
            processedFiles.forEach { filepath -> writer.write("$filepath\n") }
        }
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
