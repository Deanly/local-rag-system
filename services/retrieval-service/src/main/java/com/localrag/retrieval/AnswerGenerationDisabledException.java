package com.localrag.retrieval;

public final class AnswerGenerationDisabledException extends RuntimeException {
    public AnswerGenerationDisabledException() {
        super("Local answer generation is disabled for this runtime profile");
    }
}
