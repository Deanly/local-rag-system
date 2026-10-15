package com.localrag.common.embedding;

public class EmbeddingContractException extends IllegalStateException {
    public EmbeddingContractException(String message) {
        super(message);
    }

    public EmbeddingContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
