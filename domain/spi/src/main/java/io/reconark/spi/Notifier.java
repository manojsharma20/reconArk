package io.reconark.spi;

import java.util.Map;

/** Sends an operational notification (bus event, email, chat). Never carries unmasked sensitive data. */
public interface Notifier {

    void notify(String type, String subject, Map<String, String> attributes);
}
