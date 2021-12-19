import java.io.File

class ScanDirectory(private val directoryFilePath: String) {

    private val regexFunctionPattern = Regex(pattern = PUBLIC_FUNCTION_PATTERN, option = RegexOption.IGNORE_CASE)
    private val regexCommentPattern = Regex(pattern = COMMENT_PATTERN, option = RegexOption.IGNORE_CASE)
    private val regexFunctionNamePattern = Regex(pattern = ISOLATE_FUNCTION_NAME_PATTERN, option = RegexOption.IGNORE_CASE)

    init {
        if (directoryFilePath.isEmpty()) {
            throw IllegalArgumentException("Path has to be set")
        }

        runCatching { File(directoryFilePath) }
            .onFailure { err ->
                System.err.println("Failed to find directory on path $directoryFilePath\n$err")
            }
            .onSuccess { directory ->
                var kotlinFiles = findKotlinFiles(directory)
                if(kotlinFiles.isEmpty()) {
                    return@onSuccess
                }
                kotlinFiles
                    .map { kotlinFile -> findPublicFunctions(kotlinFile) }
                    .flatten()
                    .let { missingComments ->
                        val filesWithMissingComments = missingComments.distinctBy { it.filePath }
                        val percentageMissing = calculatePercentageOfOccurrences(
                            filesWithMissingComments.size.toFloat(),
                            kotlinFiles.size.toFloat()
                        )
                        println("public functions with missing comments: ${missingComments.size} ($percentageMissing%)")
                        missingComments.forEach(::println)
                    }
            }
    }

    private fun calculatePercentageOfOccurrences(occurrences: Float, fullSetSize: Float): Float {
        return occurrences.div(fullSetSize).times(100f)
    }

    private fun findKotlinFiles(rootScanDirectory: File): List<File> = rootScanDirectory
        .walkBottomUp()
        .filter { it.extension == KOTLIN_FILE_EXTENSION }
        .toList()

    private fun findPublicFunctions(file: File): List<FunctionWithMissingComment> {
        with(file.readLines()) {
            return mapIndexedNotNull { index, line ->
                val aboveLineIndex = index - 1
                if (aboveLineIndex < 0) {
                    return@mapIndexedNotNull null
                }

                if (!regexFunctionPattern.matches(line)) {
                    return@mapIndexedNotNull null
                }

                if (regexCommentPattern.matches(get(aboveLineIndex))) {
                    return@mapIndexedNotNull null
                }

                FunctionWithMissingComment(
                    filePath = file.canonicalPath,
                    line = index,
                    functionName = regexFunctionNamePattern.find(line)?.value.orEmpty()
                )
            }
        }
    }

    companion object {
        private const val KOTLIN_FILE_EXTENSION = "kt"
        private const val PUBLIC_FUNCTION_PATTERN = """^(?!.*?\b(private|override)).*?fun.\w*\("""
        private const val COMMENT_PATTERN = """.*?(\*/|//)"""
        private const val ISOLATE_FUNCTION_NAME_PATTERN = """(?<=fun ).*?(?=\()"""
    }
}