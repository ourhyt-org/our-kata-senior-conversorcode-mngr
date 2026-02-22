package art.ourhyt.legacy2modern.conversions.domain.ports.outputs;

import art.ourhyt.legacy2modern.conversions.domain.model.ConversionMessage;

public interface QueuePublisherPort {
    void publish(ConversionMessage message);
}
