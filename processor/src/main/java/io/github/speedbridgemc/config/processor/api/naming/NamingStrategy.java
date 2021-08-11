package io.github.speedbridgemc.config.processor.api.naming;

import io.github.speedbridgemc.config.processor.api.Identifiable;
import io.github.speedbridgemc.config.processor.api.ProcessingWorker;

public interface NamingStrategy extends Identifiable, ProcessingWorker {
    String name(String variant, String originalName);
}
