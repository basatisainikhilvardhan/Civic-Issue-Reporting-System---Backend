package com.example.Demo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer> {

    List<Issue> findByReportedByOrderByCreatedAtDesc(Users reportedBy);

    Optional<Issue> findByIdAndReportedBy(Integer id, Users reportedBy);

    List<Issue> findByStatus(IssueStatus status);

    long countByStatus(IssueStatus status);

    List<Issue> findAllByOrderByCreatedAtDesc();

    List<Issue> findByCategory(IssueCategory category);
}
