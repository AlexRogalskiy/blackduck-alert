package com.synopsys.integration.alert;

import org.springframework.beans.factory.annotation.Autowired;

import com.synopsys.integration.alert.util.AlertIntegrationTest;
import com.synopsys.integration.alert.workflow.upgrade.DescriptorRegistrar;

public class FieldRegistrationIntegrationTest extends AlertIntegrationTest {
    @Autowired
    protected DescriptorRegistrar descriptorRegistrar;

    public void registerDescriptors() {
        descriptorRegistrar.registerDescriptors();
    }
}
