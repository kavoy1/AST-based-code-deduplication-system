package com.ast.back.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 实体类最小冒烟测试：保证新实体可以正常被加载/实例化。
 *
 * 说明：这里只测“能 new”，不依赖数据库，便于在任何环境下快速跑通。
 */
class EntitySmokeTest {

    @Test
    void canInstantiateEntities() {
        assertNotNull(new Assignment());
        assertNotNull(new Submission());
        assertNotNull(new SubmissionFile());
        assertNotNull(new PlagiarismJob());
        assertNotNull(new SimilarityPair());
        assertNotNull(new SimilarityEvidence());
        assertNotNull(new AiExplanation());
    }
}