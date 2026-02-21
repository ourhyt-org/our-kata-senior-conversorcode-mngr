package art.ourhyt.legacy2modern.migration.domain.ports.outputs;

public interface PayloadPolicyPort {
    int maxPayloadBytes();

    int maxLines();
}
