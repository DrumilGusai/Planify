package com.planify.specification;

import com.planify.dto.TaskDtos.TaskFilter;
import com.planify.entity.TaskPriority;
import com.planify.entity.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskSpecsTest {

    @Test
    void specsCanBeConstructedWithFilters() {
        var filter = new TaskFilter(
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                12L,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                "search"
        );
        assertThat(TaskSpecs.ofProject(1L, filter, List.of(10L, 20L))).isNotNull();
        assertThat(TaskSpecs.forAssignee(12L, List.of(1L, 2L), filter)).isNotNull();
        assertThat(TaskSpecs.forAssignee(12L, List.of(), filter)).isNotNull();
    }
}
