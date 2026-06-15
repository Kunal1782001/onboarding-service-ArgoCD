package com.flowable.onboarding.repository;

import com.flowable.onboarding.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    List<Employee> findByStatus(Employee.OnboardingStatus status);

    List<Employee> findByDepartment(String department);

    Optional<Employee> findByProcessInstanceId(String processInstanceId);

    @Query("SELECT e FROM Employee e WHERE e.status = :status ORDER BY e.createdAt DESC")
    List<Employee> findByStatusOrderByCreatedAtDesc(@Param("status") Employee.OnboardingStatus status);

}
