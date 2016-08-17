package java.io

@library
public interface Closeable {
    public open fun close(): Unit
}

internal interface Serializable
