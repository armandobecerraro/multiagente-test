package com.smagesci.agents;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BaseAgentTest {
    
    @Test
    void testBaseAgentClassExists() {
        // Verify the class can be loaded
        assertNotNull(BaseAgent.class);
    }
    
    @Test
    void testBaseAgentExtendsJadeAgent() {
        assertTrue(jade.core.Agent.class.isAssignableFrom(BaseAgent.class));
    }
    
    @Test
    void testBaseAgentIsAbstract() {
        assertTrue(java.lang.reflect.Modifier.isAbstract(BaseAgent.class.getModifiers()));
    }
}
