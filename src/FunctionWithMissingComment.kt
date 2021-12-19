data class FunctionWithMissingComment(
    val filePath: String,
    val line: Int,
    val functionName: String,
)  {
    override fun toString(): String {
        return "function $functionName on line $line ($filePath)"
    }
}
