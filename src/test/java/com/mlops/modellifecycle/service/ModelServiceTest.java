package com.mlops.modellifecycle.service;

import com.mlops.modellifecycle.domain.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ModelServiceTest {

    private final ModelRegistryPort registry = mock(ModelRegistryPort.class);
    private final ModelPolicy policy = new ModelPolicy(List.of(
            new TransitionRule(),
            new AccuracyStopRule(0.80)
    ));
    private final ModelService service = new ModelService(policy, registry);

    @Test
    void publishesToRegistryWhenModelGoesToProduction() {
        ModelId id = ModelId.newId();

        service.changeModelStatus(id, ModelStatus.EVALUATED, ModelStatus.PRODUCTION, 0.90);

        verify(registry).publish(id);
    }

    @Test
    void doesNotPublishWhenMoveIsForbidden() {
        ModelId id = ModelId.newId();

        assertThrows(IllegalStateException.class,
                () -> service.changeModelStatus(id, ModelStatus.DRAFT, ModelStatus.PRODUCTION, 0.90));

        verify(registry, never()).publish(id);
    }
}