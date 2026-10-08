package com.example.Demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueStatusHistoryRepository extends JpaRepository<IssueStatusHistory, Integer> {

    List<IssueStatusHistory> findByIssueOrderByIdAsc(Issue issue);

    List<IssueStatusHistory> findByIssueIdOrderByIdAsc(Integer issueId);

    List<IssueStatusHistory> findByIssueIdOrderByChangedAtDesc(Integer issueId);
}
