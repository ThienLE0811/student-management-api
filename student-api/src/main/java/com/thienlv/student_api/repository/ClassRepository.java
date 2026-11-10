package com.thienlv.student_api.repository;

import com.thienlv.student_api.entity.SchoolClass;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ClassRepository {
    private static final Map<Long, SchoolClass> CLASSES = new LinkedHashMap<>(Map.of(
            1L, new SchoolClass(1L, "CNTT-K1"),
            2L, new SchoolClass(2L, "CNTT-K2"),
            3L, new SchoolClass(3L, "CNTT-K3")
    ));

    private static final AtomicLong ID_GEN = new AtomicLong(
            CLASSES.keySet().stream().mapToLong(Long::longValue).max().orElse(0)
    );

    public List<SchoolClass> findAll() {
        return CLASSES.values().stream().toList();
    }

    public Optional<SchoolClass> findById(Long id) {
        return Optional.ofNullable(CLASSES.get(id));
    }

    public SchoolClass save(SchoolClass schoolClass) {
        if (schoolClass.id() == null) {
            Long id = ID_GEN.incrementAndGet();
            SchoolClass newClass = new SchoolClass(id, schoolClass.name());
            CLASSES.put(id, newClass);
            return newClass;
        } else {
            CLASSES.put(schoolClass.id(), schoolClass);
            return schoolClass;
        }
    }

    public boolean deleteById(Long id) {
        SchoolClass removed = CLASSES.remove(id);
        return Objects.nonNull(removed);
    }
}
