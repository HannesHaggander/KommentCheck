fun main(args: Array<String>) {
    args.forEachIndexed { index, arg ->
        if (arg == "--path" && args.size > index.inc()) ScanDirectory(args[index.inc()])
    }
}
