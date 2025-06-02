package de.uni_leipzig.mds.processing;


import de.uni_leipzig.ampel.sdk.io.message.Message;

public interface MessageHandler<T> {
    void handleMessage(Message<String, T> message);
}
