package com.thienlv.student_api.service;

import com.thienlv.student_api.entity.SchoolClass;
import com.thienlv.student_api.entity.Student;
import com.thienlv.student_api.repository.ClassRepository;
import com.thienlv.student_api.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import com.thienlv.student_api.exception.ClassFullException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Buoi 22 - 2 request cung ghi danh vao 1 lop chi con dung 1 cho trong.
// SchoolClass gio da co @Version (optimistic locking) -> khi 2 thread doc
// cung 1 enrolledCount roi cung ghi, thread commit sau se bi Hibernate phat
// hien version da doi va nem ObjectOptimisticLockingFailureException, thay
// vi am tham ghi de (lost update) nhu truoc khi them @Version.
//
// Dung Testcontainers (khong dung DB dev that) de test nay tu chay duoc doc
// lap. Flyway se tu chay V1-V5 tren container rong nay giong het production.
@SpringBootTest
@Testcontainers
class StudentServiceConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private StudentService studentService;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private StudentRepository studentRepository;

    private SchoolClass targetClass;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        // Ten lop/email phai duy nhat (uk_classes_name, uk_students_email) -
        // 2 test case trong file nay chay tren cung 1 container, du lieu
        // khong tu rollback giua cac test -> can hau to rieng cho tung lan.
        String suffix = String.valueOf(System.nanoTime());
        SchoolClass sourceClass = classRepository.save(new SchoolClass(null, "Lop nguon " + suffix, LocalDateTime.now(), 30, 2));
        targetClass = classRepository.save(new SchoolClass(null, "Lop chi con 1 cho " + suffix, LocalDateTime.now(), 1, 0));

        student1 = studentRepository.save(new Student(null, "Sinh vien A", "race-a-" + suffix + "@test.com",
                LocalDate.of(2005, 1, 1), LocalDateTime.now(), sourceClass));
        student2 = studentRepository.save(new Student(null, "Sinh vien B", "race-b-" + suffix + "@test.com",
                LocalDate.of(2005, 1, 1), LocalDateTime.now(), sourceClass));
    }

    @Test
    void addToClass_chay_song_song_chi_1_request_thanh_cong_nho_optimistic_lock() throws InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        Runnable task1 = () -> runAndRecord(readyLatch, startLatch, student1.getId(), successCount, conflictCount);
        Runnable task2 = () -> runAndRecord(readyLatch, startLatch, student2.getId(), successCount, conflictCount);

        executor.submit(task1);
        executor.submit(task2);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        SchoolClass reloaded = classRepository.findById(targetClass.getId()).orElseThrow();
        int actualHeadcount = studentRepository.findBySchoolClassId(targetClass.getId()).size();
        System.out.printf("[race] success=%d, conflict=%d, capacity=%d, enrolledCount=%d, actualHeadcount=%d%n",
                successCount.get(), conflictCount.get(), reloaded.getCapacity(), reloaded.getEnrolledCount(), actualHeadcount);

        // Co @Version: dung 1 request thanh cong, request con lai bi
        // ObjectOptimisticLockingFailureException (map sang 409 o
        // GlobalExceptionHandler khi goi qua HTTP that). enrolledCount va
        // actualHeadcount khong bao gio vuot capacity nua.
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
        assertThat(actualHeadcount).isEqualTo(reloaded.getCapacity());
        assertThat(reloaded.getEnrolledCount()).isEqualTo(reloaded.getCapacity());
    }

    private void runAndRecord(CountDownLatch readyLatch, CountDownLatch startLatch, Long studentId,
                               AtomicInteger successCount, AtomicInteger conflictCount) {
        readyLatch.countDown();
        awaitUninterruptibly(startLatch);
        try {
            studentService.addToClass(targetClass.getId(), studentId);
            successCount.incrementAndGet();
        } catch (ObjectOptimisticLockingFailureException e) {
            conflictCount.incrementAndGet();
        }
    }

    // Buoi 22 (tuy chon) - so sanh voi @Lock(PESSIMISTIC_WRITE) thay vi
    // @Version. Khac voi optimistic (ca 2 deu doc duoc, 1 ben fail luc
    // commit voi ObjectOptimisticLockingFailureException), pessimistic chan
    // request den sau NGAY LUC DOC (block o muc DB) - khi duoc tiep tuc,
    // request do thay enrolledCount da MOI NHAT nen tu nhien rot vao nhanh
    // ClassFullException (409 nghiep vu binh thuong), khong phai loi ha
    // tang nhu optimistic.
    @Test
    void addToClassPessimistic_chay_song_song_chi_1_request_thanh_cong_nho_khoa_dong() throws InterruptedException {
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger classFullCount = new AtomicInteger();

        Runnable task1 = () -> runPessimisticAndRecord(readyLatch, startLatch, student1.getId(), successCount, classFullCount);
        Runnable task2 = () -> runPessimisticAndRecord(readyLatch, startLatch, student2.getId(), successCount, classFullCount);

        executor.submit(task1);
        executor.submit(task2);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        SchoolClass reloaded = classRepository.findById(targetClass.getId()).orElseThrow();
        int actualHeadcount = studentRepository.findBySchoolClassId(targetClass.getId()).size();
        System.out.printf("[race-pessimistic] success=%d, classFull=%d, capacity=%d, enrolledCount=%d, actualHeadcount=%d%n",
                successCount.get(), classFullCount.get(), reloaded.getCapacity(), reloaded.getEnrolledCount(), actualHeadcount);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(classFullCount.get()).isEqualTo(1);
        assertThat(actualHeadcount).isEqualTo(reloaded.getCapacity());
        assertThat(reloaded.getEnrolledCount()).isEqualTo(reloaded.getCapacity());
    }

    private void runPessimisticAndRecord(CountDownLatch readyLatch, CountDownLatch startLatch, Long studentId,
                                          AtomicInteger successCount, AtomicInteger classFullCount) {
        readyLatch.countDown();
        awaitUninterruptibly(startLatch);
        try {
            studentService.addToClassPessimistic(targetClass.getId(), studentId);
            successCount.incrementAndGet();
        } catch (ClassFullException e) {
            classFullCount.incrementAndGet();
        }
    }

    private void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
