package art.ourhyt.legacy2modern.migration.application;

public interface PayloadPolicyPort {
    int maxPayloadBytes();

    int maxLines();
}
